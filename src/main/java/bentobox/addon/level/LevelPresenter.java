package bentobox.addon.level;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.World;

import bentobox.addon.level.calculators.PlayerLevel;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

public class LevelPresenter {

    private int levelWait;
    private final Level addon;
    private final BentoBox plugin;

    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    public LevelPresenter(Level addon, BentoBox plugin) {
        this.addon = addon;
        this.plugin = plugin;
    }

    /**
     * Calculates the island level
     * @param world - world to check
     * @param sender - asker of the level info
     * @param targetPlayer
     * @return - false if this is cannot be done
     */
    public boolean calculateIslandLevel(World world, final User sender, UUID targetPlayer) {
        // Get permission prefix for this world
        String permPrefix = plugin.getIWM().getPermissionPrefix(world);
        // Check if target has island
        boolean inTeam = false;
        if (!plugin.getIslands().hasIsland(world, targetPlayer)) {
            // Player may be in a team
            if (plugin.getIslands().inTeam(world, targetPlayer)) {
                targetPlayer = plugin.getIslands().getTeamLeader(world, targetPlayer);
                inTeam = true;
            } else {
                sender.sendMessage("general.errors.player-has-no-island");
                return false;
            }
        }
        // Player asking for their own island calc
        if (inTeam || !sender.isPlayer() || sender.getUniqueId().equals(targetPlayer) || sender.isOp() || sender.hasPermission(permPrefix + "mod.info")) {
            // Newer better system - uses chunks
            if (!onLevelWaitTime(sender) || levelWait <= 0 || sender.isOp() || sender.hasPermission(permPrefix + "mod.info")) {
                sender.sendMessage("island.level.calculating");
                setLevelWaitTime(sender);
                new PlayerLevel(addon, plugin.getIslands().getIsland(world, targetPlayer), targetPlayer, sender);
            } else {
                // Cooldown
                sender.sendMessage("island.level.cooldown", "[time]", String.valueOf(getLevelWaitTime(sender)));
            }

        } else {
            // Asking for the level of another player
            sender.sendMessage("island.level.island-level-is","[level]", String.valueOf(addon.getIslandLevel(world, targetPlayer)));
        }
        return true;
    }

    /**
     * Sets cool down for the level command
     *
     * @param player
     */
    private void setLevelWaitTime(final User player) {
        levelWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + levelWait * 1000));
    }

    private boolean onLevelWaitTime(final User sender) {
        if (levelWaitTime.containsKey(sender.getUniqueId())) {
            return levelWaitTime.get(sender.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis();

        }

        return false;
    }

    private long getLevelWaitTime(final User sender) {
        if (levelWaitTime.containsKey(sender.getUniqueId())) {
            if (levelWaitTime.get(sender.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
                return (levelWaitTime.get(sender.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis()) / 1000;
            }

            return 0L;
        }

        return 0L;
    }
}
