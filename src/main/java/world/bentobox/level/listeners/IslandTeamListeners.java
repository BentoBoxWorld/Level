package world.bentobox.level.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.island.IslandEvent.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandRegisteredEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandResettedEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandUnregisteredEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamJoinedEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamKickEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamLeaveEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamSetownerEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.CalcIslandLevel;

/**
 * Listens for new islands or ownership changes and sets the level to zero automatically
 * @author tastybento
 *
 */
public class IslandTeamListeners implements Listener {

    private final Level addon;
    private final Map<Island, CalcIslandLevel> cil;

    /**
     * @param addon - addon
     */
    public IslandTeamListeners(Level addon) {
        this.addon = addon;
        cil = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandCreatedEvent e) {
        // Clear the island setting
        addon.setInitialIslandLevel(e.getIsland(), 0L);
        if (e.getIsland().getOwner() != null && e.getIsland().getWorld() != null) {
            cil.putIfAbsent(e.getIsland(), new CalcIslandLevel(addon, e.getIsland(), () -> zeroLevel(e.getIsland())));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandResettedEvent e) {
        // Clear the island setting
        addon.setInitialIslandLevel(e.getIsland(), 0L);
        if (e.getIsland().getOwner() != null && e.getIsland().getWorld() != null) {
            cil.putIfAbsent(e.getIsland(), new CalcIslandLevel(addon, e.getIsland(), () -> zeroLevel(e.getIsland())));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIslandOwner(TeamSetownerEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getIsland().getOwner(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getIsland().getOwner());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamJoinedEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getPlayerUUID(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandUnregisteredEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getPlayerUUID(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(IslandRegisteredEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getPlayerUUID(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamLeaveEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getPlayerUUID(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIsland(TeamKickEvent e) {
        // Remove player from the top ten and level
        addon.setIslandLevel(e.getIsland().getWorld(), e.getPlayerUUID(), 0);
        addon.getTopTen().removeEntry(e.getIsland().getWorld(), e.getPlayerUUID());
    }

    private void zeroLevel(Island island) {
        if (cil.containsKey(island)) {
            long level = cil.get(island).getResult().getLevel();
            // Get deaths
            int deaths = addon.getPlayers().getDeaths(island.getWorld(), island.getOwner());
            // Death penalty calculation.
            if (addon.getSettings().getLevelCost() != 0) {
                // Add the deaths because this makes the original island that much "bigger"
                level += deaths * addon.getSettings().getDeathPenalty() / addon.getSettings().getLevelCost();
            }
            addon.setInitialIslandLevel(island, level);
            cil.remove(island);
        }
    }
}
