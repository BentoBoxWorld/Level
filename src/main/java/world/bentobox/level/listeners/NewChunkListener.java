package world.bentobox.level.listeners;

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

    private final Level addon;

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
        Location centre = new Location(world, (chunk.getX() << 4) + 8, world.getMinHeight(),
                (chunk.getZ() << 4) + 8);
        Island island = addon.getIslands().getIslandAt(centre).orElse(null);
        if (island == null || island.getOwner() == null) {
            return;
        }
        // Capture all main-thread state before going async.
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        int seaHeight = addon.getPlugin().getIWM().getSeaHeight(world);
        double underwaterMultiplier = addon.getSettings().getUnderWaterMultiplier();
        int minProtectedX = island.getMinProtectedX();
        int maxProtectedX = island.getMaxProtectedX();
        int minProtectedZ = island.getMinProtectedZ();
        int maxProtectedZ = island.getMaxProtectedZ();
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        int chunkBlockX = chunk.getX() << 4;
        int chunkBlockZ = chunk.getZ() << 4;

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            long total = scanSnapshot(snapshot, world, chunkBlockX, chunkBlockZ, minHeight, maxHeight,
                    minProtectedX, maxProtectedX, minProtectedZ, maxProtectedZ, seaHeight,
                    underwaterMultiplier);
            if (total != 0L) {
                Bukkit.getScheduler().runTask(addon.getPlugin(),
                        () -> addon.getManager().addToInitialCount(island, total));
            }
        });
    }

    private long scanSnapshot(ChunkSnapshot snapshot, World world, int chunkBlockX, int chunkBlockZ,
            int minHeight, int maxHeight, int minProtectedX, int maxProtectedX, int minProtectedZ,
            int maxProtectedZ, int seaHeight, double underwaterMultiplier) {
        long total = 0L;
        for (int x = 0; x < 16; x++) {
            int globalX = chunkBlockX + x;
            if (globalX < minProtectedX || globalX >= maxProtectedX) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                int globalZ = chunkBlockZ + z;
                if (globalZ < minProtectedZ || globalZ >= maxProtectedZ) {
                    continue;
                }
                for (int y = minHeight; y < maxHeight; y++) {
                    Material mat = snapshot.getBlockType(x, y, z);
                    if (mat.isAir()) {
                        continue;
                    }
                    Integer value = addon.getBlockConfig().getValue(world, mat);
                    if (value == null || value == 0) {
                        continue;
                    }
                    if (seaHeight > 0 && y <= seaHeight) {
                        total += (long) (value * underwaterMultiplier);
                    } else {
                        total += value;
                    }
                }
            }
        }
        return total;
    }
}
