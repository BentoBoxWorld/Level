package world.bentobox.level.listeners;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandEvent.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandPreclearEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandRegisteredEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandResettedEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandUnregisteredEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamJoinedEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamKickEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamLeaveEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamSetownerEvent;
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
        zeroIsland(e.getIsland());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandResettedEvent e) {
        zeroIsland(e.getIsland());
    }

    private void zeroIsland(final Island island) {
        // Clear the island setting
        if (island.getOwner() != null && island.getWorld() != null) {
            addon.getPipeliner().addIsland(island).thenAccept(results -> {
                addon.getManager().setInitialIslandLevel(island, results.getLevel());
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandPreclearEvent e) {
        // Remove player from the top ten and level
        UUID uuid = e.getIsland().getOwner();
        World world = e.getIsland().getWorld();
        remove(world, uuid);
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
        // Remove player from the top ten and level
        remove(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandRegisteredEvent e) {
        // Remove player from the top ten and level
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
