package world.bentobox.level.listeners;

import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

/**
 * Applies the BentoBox admin deaths commands ({@code /<admin> deaths set|add|remove|reset})
 * to Level's per-island death counts, so admins do not need a separate Level command.
 * The change is applied to every island the player is a member of in that game mode.
 * <p>
 * Only registered when the running BentoBox has {@link PlayerDeathsChangedEvent}.
 *
 * @author tastybento
 */
public class AdminDeathsListener implements Listener {

    private final Level addon;

    /**
     * @param addon - addon
     */
    public AdminDeathsListener(Level addon) {
        this.addon = addon;
    }

    /**
     * Apply an admin deaths change to the player's islands in that world.
     * @param e event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathsChanged(PlayerDeathsChangedEvent e) {
        World world = e.getWorld();
        if (world == null || !addon.isRegisteredGameModeWorld(world)) {
            return;
        }
        UUID uuid = e.getPlayerUUID();
        for (Island island : addon.getIslands().getIslands(world, uuid)) {
            if (!island.getMemberSet().contains(uuid)) {
                continue;
            }
            switch (e.getAction()) {
            case SET -> addon.getManager().setDeaths(island, uuid, e.getAmount());
            case RESET -> addon.getManager().setDeaths(island, uuid, 0);
            case ADD -> addon.getManager().addDeaths(island, uuid, e.getAmount());
            case REMOVE -> addon.getManager().removeDeaths(island, uuid, e.getAmount());
            }
        }
    }
}
