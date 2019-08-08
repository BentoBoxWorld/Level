package world.bentobox.level;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.calculators.PlayerLevel;

class LevelPresenter {

    private int levelWait;
    private final Level addon;
    private final BentoBox plugin;

    // Level calc cool down
    private final HashMap<UUID, Long> levelWaitTime = new HashMap<>();

    public LevelPresenter(Level addon, BentoBox plugin) {
        this.addon = addon;
        this.plugin = plugin;
    }

    /**
     * Calculates the island level
     * @param world - world to check
     * @param sender - asker of the level info
     * @param targetPlayer - target player
     */
    public void calculateIslandLevel(World world, final User sender, UUID targetPlayer) {
        // Get permission prefix for this world
        String permPrefix = plugin.getIWM().getPermissionPrefix(world);
        // Check if target has island
        boolean inTeam = false;
        if (!plugin.getIslands().hasIsland(world, targetPlayer)) {
            // Player may be in a team
            if (plugin.getIslands().inTeam(world, targetPlayer)) {
                targetPlayer = plugin.getIslands().getOwner(world, targetPlayer);
                inTeam = true;
            } else {
                if (sender != null) sender.sendMessage("general.errors.player-has-no-island");
                return;
            }
        }
        // Player asking for their own island calc
        if (sender == null || inTeam || !sender.isPlayer() || sender.getUniqueId().equals(targetPlayer) || sender.isOp() || sender.hasPermission(permPrefix + "mod.info")) {
            // Newer better system - uses chunks
            if (sender == null || !onLevelWaitTime(sender) || levelWait <= 0 || sender.isOp() || sender.hasPermission(permPrefix + "mod.info")) {
                if (sender != null) {
                    sender.sendMessage("island.level.calculating");
                    setLevelWaitTime(sender);
                }
                new PlayerLevel(addon, plugin.getIslands().getIsland(world, targetPlayer), targetPlayer, sender);
            } else {
                // Cooldown
                sender.sendMessage("island.level.cooldown", "[time]", String.valueOf(getLevelWaitTime(sender)));
            }

        } else {
            // Asking for the level of another player
            sender.sendMessage("island.level.island-level-is","[level]", String.valueOf(addon.getIslandLevel(world, targetPlayer)));
        }
    }

    /**
     * Sets cool down for the level command
     *
     * @param user - user
     */
    private void setLevelWaitTime(final User user) {
        levelWaitTime.put(user.getUniqueId(), Calendar.getInstance().getTimeInMillis() + levelWait * 1000);
    }

    private boolean onLevelWaitTime(final User sender) {
        if (levelWaitTime.containsKey(sender.getUniqueId())) {
            return levelWaitTime.get(sender.getUniqueId()) > Calendar.getInstance().getTimeInMillis();
        }

        return false;
    }

    private long getLevelWaitTime(final User sender) {
        if (levelWaitTime.containsKey(sender.getUniqueId())) {
            if (levelWaitTime.get(sender.getUniqueId()) > Calendar.getInstance().getTimeInMillis()) {
                return (levelWaitTime.get(sender.getUniqueId()) - Calendar.getInstance().getTimeInMillis()) / 1000;
            }

            return 0L;
        }

        return 0L;
    }
}
