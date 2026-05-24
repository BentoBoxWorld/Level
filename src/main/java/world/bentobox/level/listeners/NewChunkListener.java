package world.bentobox.level.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.IslandLevelCalculator;

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
 * <p>
 * The actual scoring is delegated to
 * {@link IslandLevelCalculator#scoreNewChunkSnapshot} so the per-block logic
 * stays in lockstep with the regular scan — custom blocks, double slabs and
 * configured values are all counted identically. No per-listener state is
 * kept across chunks: {@link ChunkLoadEvent#isNewChunk()} already guarantees
 * each generated chunk is reported once per server run, so a separate dedup
 * set would only ever grow unbounded.
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
        // The + 8.0 keeps the addition in double arithmetic so SonarQube does not
        // flag a theoretical int-overflow before the implicit widening.
        Location centre = new Location(world, (chunk.getX() << 4) + 8.0, world.getMinHeight(),
                (chunk.getZ() << 4) + 8.0);
        Island island = addon.getIslands().getIslandAt(centre).orElse(null);
        if (island == null || island.getOwner() == null) {
            return;
        }

        int delay = Math.max(0, addon.getSettings().getZeroScanDelayTicks());
        addon.getManager().addPendingZero(island);
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(),
                () -> processChunk(world, chunk, island), delay);
    }

    /**
     * Snapshot the chunk after the configured delay and score it on a worker
     * thread using the real {@link IslandLevelCalculator} logic. The chunk may
     * have been unloaded by the time this runs; Bukkit's {@link ChunkSnapshot}
     * is immutable, so as long as the chunk is loaded here the scoring can
     * safely run off-thread. Once done,
     * {@link world.bentobox.level.LevelsManager#completePendingZero} releases
     * the in-flight counter so any waiting level scan can finalise.
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

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            long total = IslandLevelCalculator.scoreNewChunkSnapshot(addon, island, snapshot, chunk, world);
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                // During an active zero-island scan, route the credit
                // through the deferral map so the post-scan drain can
                // decide whether this chunk's value belongs in the
                // baseline (scan missed it) or should be dropped (scan
                // counted it).
                String worldName = world.getName();
                if (!addon.getManager().tryDeferZeroScanCredit(island, worldName, chunkX, chunkZ, total)
                        && total != 0L) {
                    // No active zero scan — fold the chunk into the
                    // per-chunk handicap map and initialCount in one
                    // atomic save. The map keeps the credit frozen so a
                    // future regular scan sees this chunk as already
                    // accounted for and doesn't double-credit it.
                    addon.getManager().addHandicapChunk(island,
                            IslandLevelCalculator.handicapKey(world, chunkX, chunkZ), total);
                }
                addon.getManager().completePendingZero(island);
            });
        });
    }
}
