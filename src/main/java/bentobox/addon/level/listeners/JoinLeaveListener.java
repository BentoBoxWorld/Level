package bentobox.addon.level.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import bentobox.addon.level.Level;

/**
 * Listens for when players join and leave
 * @author tastybento
 *
 */
public class JoinLeaveListener implements Listener {

    private Level addon;

    /**
     * @param addon
     */
    public JoinLeaveListener(Level addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Load player into cache
        addon.getLevelsData(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        addon.uncachePlayer(e.getPlayer().getUniqueId());
    }


}
