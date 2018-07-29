package bskyblock.addon.level.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import bskyblock.addon.level.Level;
import bskyblock.addon.level.calculators.CalcIslandLevel;
import world.bentobox.bbox.api.events.island.IslandEvent.IslandCreatedEvent;
import world.bentobox.bbox.api.events.island.IslandEvent.IslandResettedEvent;
import world.bentobox.bbox.database.objects.Island;

/**
 * Listens for new islands and sets the level to zero automatically
 * @author tastybento
 *
 */
public class NewIslandListener implements Listener {

    private Level addon;
    private Map<Island, CalcIslandLevel> cil;

    /**
     * @param addon
     */
    public NewIslandListener(Level addon) {
        this.addon = addon;
        cil = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandCreatedEvent e) {
        cil.putIfAbsent(e.getIsland(), new CalcIslandLevel(addon, e.getIsland(), () -> zeroLevel(e.getIsland())));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandResettedEvent e) {
        cil.putIfAbsent(e.getIsland(), new CalcIslandLevel(addon, e.getIsland(), () -> zeroLevel(e.getIsland())));
    }

    private void zeroLevel(Island island) {
        if (cil.containsKey(island)) {
            addon.setInitialIslandLevel(island, cil.get(island).getResult().getLevel());
            cil.remove(island);
        }
    }
}
