package bskyblock.addon.level;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.World;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;

public class LevelPresenter {

    private int levelWait;
    private final Level plugin;
    private final BSkyBlock bSkyBlock;

    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    public LevelPresenter(Level plugin, BSkyBlock bSkyBlock) {
        this.plugin = plugin;
        this.bSkyBlock = bSkyBlock;
    }

    /**
     * Calculates the island level
     * @param world - world to check
     * @param sender - asker of the level info
     * @param targetPlayer
     * @param report - if true, a detailed report will be provided
     * @param permPrefix - per prefix for this player
     * @return - false if this is cannot be done
     */
    public boolean calculateIslandLevel(World world, final User sender, UUID targetPlayer, boolean report, String permPrefix) {
        // Check if sender has island
        boolean inTeam = false;
        if (!bSkyBlock.getIslands().hasIsland(world, targetPlayer)) {
            // Player may be in a team
            if (bSkyBlock.getIslands().inTeam(world, targetPlayer)) {
                targetPlayer = bSkyBlock.getIslands().getTeamLeader(world, targetPlayer);
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
                new LevelCalcByChunk(plugin, bSkyBlock.getIslands().getIsland(world, targetPlayer), targetPlayer, sender, report, permPrefix);
            } else {
                // Cooldown
                sender.sendMessage("island.level.cooldown", "[time]", String.valueOf(getLevelWaitTime(sender)));
            }

        } else {
            // Asking for the level of another player
            sender.sendMessage("island.level.island-level-is","[level]", String.valueOf(plugin.getIslandLevel(world, targetPlayer)));
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
            if (levelWaitTime.get(sender.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
                return true;
            }

            return false;
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
