package world.bentobox.level.listeners;

import java.util.UUID;

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
 * Listens for new islands or ownership changes and sets the level to zero automatically
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
            zeroIsland(e.getIsland());
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
            addon.getPipeliner().zeroIsland(island).thenAccept(results ->
            addon.getManager().setInitialIslandLevel(island, results.getLevel()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandPreclearEvent e) {

        // Remove player from the top ten and level
        UUID uuid = e.getIsland().getOwner();
        World world = e.getIsland().getWorld();
        remove(world, uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDeleted(IslandDeleteEvent e) {
        // Remove island
        addon.getManager().deleteIsland(e.getIsland().getUniqueId());
    }

    private void remove(World world, UUID uuid) {
        if (uuid != null && world != null) {
            addon.getManager().removeEntry(world, uuid);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIslandOwner(TeamSetownerEvent e) {

        // Remove player from the top ten and level
        remove(e.getIsland().getWorld(), e.getIsland().getOwner());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamJoinedEvent e) {

        // Remove player from the top ten and level
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandUnregisteredEvent e) {

        // Remove player from the top ten
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandRegisteredEvent e) {

        // Remove player from the top ten
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamLeaveEvent e) {

        // Remove player from the top ten and level
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamKickEvent e) {

        // Remove player from the top ten and level
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

}
