package world.bentobox.level.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandPreclearEvent;
import world.bentobox.bentobox.api.events.island.IslandRegisteredEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.api.events.island.IslandUnregisteredEvent;
import world.bentobox.bentobox.api.events.team.TeamJoinedEvent;
import world.bentobox.bentobox.api.events.team.TeamKickEvent;
import world.bentobox.bentobox.api.events.team.TeamLeaveEvent;
import world.bentobox.bentobox.api.events.team.TeamSetownerEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

/**
 * Listens for new islands or ownership changes and sets the level to zero
 * automatically
 * 
 * @author tastybento
 *
 */
public class IslandActivitiesListeners implements Listener {

    private final Level addon;

    /**
     * @param addon - addon
     */
    public IslandActivitiesListeners(Level addon) {
        this.addon = addon;

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandCreatedEvent e) {
        if (addon.getSettings().isZeroNewIslandLevels()) {
            // Delay the zero scan so decoration (fluid simulation,
            // late-arriving obsidian/portal frames, trial-spawner setup,
            // chunk-border ore patches) has time to settle before we
            // snapshot the baseline. Reuses the listener's decoration-settle
            // setting (zero-scan-delay-ticks, default 600 = 30s) so both
            // paths honour the same "give the world a chance to finish
            // generating" window.
            long delay = Math.max(150L, addon.getSettings().getZeroScanDelayTicks());
            Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> zeroIsland(e.getIsland()), delay);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandResettedEvent e) {
        if (addon.getSettings().isZeroNewIslandLevels()) {
            zeroIsland(e.getIsland());
        }
    }

    private void zeroIsland(final Island island) {
        // Clear the island setting
        if (island.getOwner() != null && island.getWorld() != null) {
            addon.getPipeliner().zeroIsland(island)
                    .thenAccept(results -> {
                        if (results == null) {
                            // Scan was aborted (island deleted/unowned mid-flight).
                            // Drop the tracking maps so the next zero scan
                            // for this island starts clean.
                            addon.getManager().drainZeroScanDeferred(island);
                            return;
                        }
                        // Hard reset the baseline. The calculator already
                        // overwrote the per-chunk handicap map with the
                        // scanned values via reconcileHandicapChunks(...,
                        // zeroScan=true) inside tidyUp; this aligns the
                        // initialCount with that same baseline.
                        addon.getManager().setInitialIslandCount(island, results.getTotalPoints());
                        // Fold in any listener credits captured during the
                        // scan for chunks the scan didn't visit (e.g.
                        // chunks generated mid-scan after their position
                        // was already polled). Reconciling these as a
                        // non-zero scan adds them to both the per-chunk
                        // map and the initialCount in one atomic write,
                        // so subsequent scans see those chunks as fully
                        // accounted for.
                        java.util.Map<String, Long> missed = addon.getManager().drainZeroScanDeferred(island);
                        if (!missed.isEmpty()) {
                            addon.getManager().reconcileHandicapChunks(island, missed, false);
                        }
                    });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandPreclearEvent e) {
        remove(e.getIsland().getWorld(), e.getIsland().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDeleted(IslandDeleteEvent e) {
        // Remove island
        addon.getManager().deleteIsland(e.getIsland().getUniqueId());
    }

    private void remove(World world, String uuid) {
        if (uuid != null && world != null) {
            addon.getManager().removeEntry(world, uuid);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIslandOwner(TeamSetownerEvent e) {

        // Remove island from the top ten and level
        remove(e.getIsland().getWorld(), e.getIsland().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamJoinedEvent e) {
        // TODO: anything to do here?
        // Remove player from the top ten and level
        // remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandUnregisteredEvent e) {

        // Remove island from the top ten
        remove(e.getIsland().getWorld(), e.getIsland().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandRegisteredEvent e) {
        // TODO: anything to do here?
        // Remove player from the top ten
        // remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamLeaveEvent e) {
        // TODO: anything to do here?
        // Remove player from the top ten and level
        // remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamKickEvent e) {
        //// TODO: anything to do here?
        // Remove player from the top ten and level
        // remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

}
