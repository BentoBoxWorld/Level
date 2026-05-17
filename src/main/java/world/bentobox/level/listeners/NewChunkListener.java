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
import world.bentobox.level.LevelsManager;

/**
 * Listens for freshly-generated chunks inside an island's protected area and
 * adds the chunk's generator block points to the island's initial-count
 * handicap.
 * <p>
 * The snapshot is captured a configurable number of ticks <em>after</em>
 * {@link ChunkLoadEvent#isNewChunk()} fires
 * ({@code zero-scan-delay-ticks}). This delay lets neighbouring chunks finish
 * their decoration phase — late-arriving blocks like obsidian (lava+water),
 * ore patches spilling across chunk borders, broken nether-portal frames,
 * etc. — so the captured snapshot matches what a regular level scan would
 * later see. Together with
 * {@link world.bentobox.level.LevelsManager#awaitPendingZeros LevelsManager#awaitPendingZeros},
 * the regular scan never returns a stale level while a delayed capture is
 * still in flight.
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
     * Per-island set of chunk keys (x:z packed into a long) already
     * <em>queued or processed</em> in this server run. Defends against
     * ChunkLoadEvent firing more than once for the same chunk under heavy
     * activity (Paper ticket churn, parallel level-scan loads). After a
     * restart Paper reports isNewChunk=false for already-generated chunks so
     * earlier-run chunks are not at risk of re-counting on the next run.
     */
    private final Map<String, Set<Long>> queuedChunks = new HashMap<>();

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
        // Dedup: only enqueue each chunk for an island once per server run.
        long key = LevelsManager.chunkKey(chunk.getX(), chunk.getZ());
        Set<Long> seen = queuedChunks.computeIfAbsent(island.getUniqueId(), k -> new HashSet<>());
        if (!seen.add(key)) {
            return;
        }

        int delay = Math.max(0, addon.getSettings().getZeroScanDelayTicks());
        addon.getManager().addPendingZero(island);
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(),
                () -> processChunk(world, chunk, island), delay);
    }

    /**
     * Snapshot the chunk after the configured delay and process it on a
     * worker thread. The chunk may have been unloaded by the time this runs;
     * Bukkit's ChunkSnapshot is immutable, so as long as the chunk is loaded
     * here we can scan it off-thread. Once done,
     * {@code completePendingZero} releases the in-flight counter so any
     * waiting level scan can finalise.
     */
    private void processChunk(World world, Chunk chunk, Island island) {
        // Skip if the island was deleted while waiting for the delay.
        if (island.isDeleted() || island.getOwner() == null) {
            addon.getManager().completePendingZero(island);
            return;
        }
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        ScanContext ctx = new ScanContext(world, chunkX << 4, chunkZ << 4,
                world.getMinHeight(), world.getMaxHeight(),
                island.getMinProtectedX(), island.getMaxProtectedX(),
                island.getMinProtectedZ(), island.getMaxProtectedZ(),
                addon.getPlugin().getIWM().getSeaHeight(world),
                addon.getSettings().getUnderWaterMultiplier());

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            long total = scanSnapshot(snapshot, ctx);
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                // During an active zero-island scan, route the credit
                // through the deferral map so the post-scan drain can
                // decide whether this chunk's value belongs in the
                // baseline (scan missed it) or should be dropped (scan
                // counted it).
                if (!addon.getManager().tryDeferZeroScanCredit(island, chunkX, chunkZ, total)
                        && total != 0L) {
                    addon.getManager().addToInitialCount(island, total);
                }
                addon.getManager().completePendingZero(island);
            });
        });
    }

    private long scanSnapshot(ChunkSnapshot snapshot, ScanContext ctx) {
        long total = 0L;
        // Per-chunk material counts so we can apply the same limits the regular
        // scan applies. Without this, value-bearing limited blocks (cobblestone,
        // stone with non-zero value, etc.) could push the handicap past
        // anything the regular scan would ever credit.
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
        // listener does not share state with prior chunks for an island, so
        // the cap applies per chunk. That still drops uncapped accumulation
        // from terrain-rich blocks far below what an uncapped listener
        // would record.
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
