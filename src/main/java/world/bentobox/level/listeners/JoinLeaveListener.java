package world.bentobox.level.listeners;

import java.util.Objects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import world.bentobox.level.Level;

/**
 * Listens for when players join
 * @author tastybento
 *
 */
public class JoinLeaveListener implements Listener {

    private final Level addon;

    /**
     * @param addon - addon
     */
    public JoinLeaveListener(Level addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Load player into cache
        addon.getManager().getLevelsData(e.getPlayer().getUniqueId());
        // If level calc on login is enabled, run through all the worlds and calculate the level
        if (addon.getSettings().isCalcOnLogin()) {
            addon.getPlugin().getAddonsManager().getGameModeAddons().stream()
            .filter(gm -> !addon.getSettings().getGameModes().contains(gm.getDescription().getName()))
            .map(gm -> gm.getIslands().getIsland(gm.getOverWorld(), e.getPlayer().getUniqueId()))
            .filter(Objects::nonNull)
            .forEach(island -> addon.getManager().calculateLevel(e.getPlayer().getUniqueId(), island));
        }

    }

}
