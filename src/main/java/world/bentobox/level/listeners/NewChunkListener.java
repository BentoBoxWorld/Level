package world.bentobox.level.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

/**
 * Listens for freshly-generated chunks inside an island's protected area and
 * adds the chunk's generator block points to the island's initial-count
 * handicap.
 * <p>
 * Together with the {@code gen=false} initial zero scan in
 * {@link world.bentobox.level.calculators.IslandLevelCalculator}, this lets
 * zero-new-island-level mode work on islands with very large protection
 * ranges. The initial scan only records what is already generated at island
 * creation time (typically just the schematic chunks). As the player
 * explores and new chunks are generated, this listener accumulates their
 * generator block points into the initial count so they cancel out of the
 * regular level calc — players only get credit for blocks they actually
 * place.
 */
public class NewChunkListener implements Listener {

    /**
     * Snapshot of the main-thread state needed to score one chunk on a worker
     * thread. Bundled into a record so the async scan helpers don't need to
     * carry a dozen parameters each.
     */
    private record ScanContext(World world, int chunkBlockX, int chunkBlockZ, int minHeight, int maxHeight,
            int minProtectedX, int maxProtectedX, int minProtectedZ, int maxProtectedZ,
            int seaHeight, double underwaterMultiplier) {
    }

    private final Level addon;
    /**
     * Per-island set of chunk keys (x:z packed into a long) that have already
     * contributed to the initial-count handicap during this server run.
     * Defends against ChunkLoadEvent firing isNewChunk=true more than once for
     * the same chunk under heavy chunk activity (Paper ticket churn, parallel
     * level-scan loads, plugin re-triggers). Without this, every duplicate
     * firing inflated the handicap by another chunk's worth of value.
     * <p>
     * Indexed by island uniqueId. Entries persist for the lifetime of the JVM.
     * After a restart Paper reports isNewChunk=false for already-generated
     * chunks, so chunks counted in earlier runs are not at risk of being
     * recounted on the next run.
     */
    private final Map<String, Set<Long>> countedChunks = new HashMap<>();

    public NewChunkListener(Level addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.isNewChunk()) {
            return;
        }
        if (!addon.getSettings().isZeroNewIslandLevels()) {
            return;
        }
        Chunk chunk = e.getChunk();
        World world = chunk.getWorld();
        if (!addon.isRegisteredGameModeWorld(world)) {
            return;
        }
        // Use the chunk centre to look up the island that owns it.
        // The + 8.0 keeps the addition in double arithmetic so SonarQube does not
        // flag a theoretical int-overflow before the implicit widening.
        Location centre = new Location(world, (chunk.getX() << 4) + 8.0, world.getMinHeight(),
                (chunk.getZ() << 4) + 8.0);
        Island island = addon.getIslands().getIslandAt(centre).orElse(null);
        if (island == null || island.getOwner() == null) {
            return;
        }
        // Dedup: only credit each chunk to an island once per server run.
        long key = chunkKey(chunk.getX(), chunk.getZ());
        Set<Long> seen = countedChunks.computeIfAbsent(island.getUniqueId(), k -> new HashSet<>());
        if (!seen.add(key)) {
            return;
        }
        // Capture all main-thread state before going async.
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        ScanContext ctx = new ScanContext(world, chunk.getX() << 4, chunk.getZ() << 4,
                world.getMinHeight(), world.getMaxHeight(),
                island.getMinProtectedX(), island.getMaxProtectedX(),
                island.getMinProtectedZ(), island.getMaxProtectedZ(),
                addon.getPlugin().getIWM().getSeaHeight(world),
                addon.getSettings().getUnderWaterMultiplier());

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            long total = scanSnapshot(snapshot, ctx);
            if (total != 0L) {
                Bukkit.getScheduler().runTask(addon.getPlugin(),
                        () -> addon.getManager().addToInitialCount(island, total));
            }
        });
    }

    /**
     * Pack chunk (x, z) into a single 64-bit key. Negative coordinates are
     * preserved by masking to 32 bits before shifting.
     */
    private static long chunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    private long scanSnapshot(ChunkSnapshot snapshot, ScanContext ctx) {
        long total = 0L;
        // Per-chunk material counts so we can apply the same limits the regular
        // scan applies. Without this, value-bearing limited blocks (cobblestone,
        // stone with non-zero value, etc.) inflate the handicap past anything
        // the regular scan would ever credit.
        Map<Material, Integer> perMaterial = new HashMap<>();
        for (int x = 0; x < 16; x++) {
            int globalX = ctx.chunkBlockX + x;
            if (globalX >= ctx.minProtectedX && globalX < ctx.maxProtectedX) {
                total += scanRow(snapshot, x, ctx, perMaterial);
            }
        }
        return total;
    }

    private long scanRow(ChunkSnapshot snapshot, int x, ScanContext ctx, Map<Material, Integer> perMaterial) {
        long total = 0L;
        for (int z = 0; z < 16; z++) {
            int globalZ = ctx.chunkBlockZ + z;
            if (globalZ >= ctx.minProtectedZ && globalZ < ctx.maxProtectedZ) {
                total += scanColumn(snapshot, x, z, ctx, perMaterial);
            }
        }
        return total;
    }

    private long scanColumn(ChunkSnapshot snapshot, int x, int z, ScanContext ctx, Map<Material, Integer> perMaterial) {
        long total = 0L;
        for (int y = ctx.minHeight; y < ctx.maxHeight; y++) {
            total += valueAt(snapshot, x, y, z, ctx, perMaterial);
        }
        return total;
    }

    private long valueAt(ChunkSnapshot snapshot, int x, int y, int z, ScanContext ctx,
            Map<Material, Integer> perMaterial) {
        Material mat = snapshot.getBlockType(x, y, z);
        if (mat.isAir()) {
            return 0L;
        }
        Integer value = addon.getBlockConfig().getValue(ctx.world, mat);
        if (value == null || value == 0) {
            return 0L;
        }
        // Respect per-material limits so the listener can never credit more
        // than the regular scan would. Counts are local to this chunk; the
        // listener does not (yet) share state with prior chunks for an island,
        // so the cap is applied per chunk. This still drops infinite handicap
        // accumulation from terrain-rich blocks far below what an uncapped
        // listener would record.
        Integer limit = addon.getBlockConfig().getLimit(mat);
        if (limit != null) {
            int count = perMaterial.getOrDefault(mat, 0);
            if (count >= limit) {
                return 0L;
            }
            perMaterial.put(mat, count + 1);
        }
        if (ctx.seaHeight > 0 && y <= ctx.seaHeight) {
            return (long) (value * ctx.underwaterMultiplier);
        }
        return value;
    }
}
