package bskyblock.addon.level;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class LevelPresenter extends LevelPlugin {

    private int levelWait;
    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    public LevelPresenter(Level plugin) {
        super(plugin);    
    }

    /**
     * Calculates the island level
     * 
     * @param sender
     *            - Player object of player who is asking
     * @param targetPlayer
     *            - UUID of the player's island that is being requested
     * @return - true if successful.
     */
    public boolean calculateIslandLevel(final User sender, final UUID targetPlayer) {
        return calculateIslandLevel(sender, targetPlayer, false);
    }

    /**
     * Calculates the island level
     * @param sender - asker of the level info
     * @param targetPlayer
     * @param report - if true, a detailed report will be provided
     * @return - false if this is cannot be done
     */
    public boolean calculateIslandLevel(final User sender, final UUID targetPlayer, boolean report) {
        // Check if sender has island
        if (!bSkyBlock.getIslands().hasIsland(targetPlayer)) {
            sender.sendLegacyMessage("Target does not have an island");
            return false;
        }
        // Player asking for their own island calc
        if (!sender.isPlayer() || sender.getUniqueId().equals(targetPlayer) || sender.isOp() || sender.hasPermission(Settings.PERMPREFIX + "mod.info")) {
            // Newer better system - uses chunks
            if (!onLevelWaitTime(sender) || levelWait <= 0 || sender.isOp() || sender.hasPermission(Settings.PERMPREFIX + "mod.info")) {
                sender.sendLegacyMessage(ChatColor.GREEN + "Calculating level, please wait...");
                setLevelWaitTime(sender);
                new ChunkScanner(plugin, bSkyBlock.getIslands().getIsland(targetPlayer), sender);
            } else {
                sender.sendLegacyMessage( ChatColor.YELLOW + String.valueOf(getLevelWaitTime(sender)));
            }

        } else {
            // Asking for the level of another player
            sender.sendMessage("island.islandLevelIs","[level]", String.valueOf(plugin.getIslandLevel(targetPlayer)));
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
