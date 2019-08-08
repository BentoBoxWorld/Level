package world.bentobox.level.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.level.Level;

/**
 * Listens for when players join and leave
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
        addon.getLevelsData(e.getPlayer().getUniqueId());
        // If level calc on login is enabled, run through all the worlds and calculate the level
        if (addon.getSettings().isLogin()) {
            addon.getPlugin().getAddonsManager().getGameModeAddons().stream()
            .filter(gm -> addon.getSettings().getGameModes().contains(gm.getDescription().getName()))
            .forEach(gm -> {
                addon.calculateIslandLevel(gm.getOverWorld(), null, e.getPlayer().getUniqueId());
            });
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        addon.uncachePlayer(e.getPlayer().getUniqueId());
    }


}
