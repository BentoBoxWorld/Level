package bskyblock.addin.level;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class CalculateLevel extends LevelPlugin {
    
    private int levelWait;
    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    public CalculateLevel(Level plugin) {
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
    public boolean calculateIslandLevel(final CommandSender sender, final UUID targetPlayer) {
        return calculateIslandLevel(sender, targetPlayer, false);
    }

    /**
     * Calculates the island level
     * @param sender - asker of the level info
     * @param targetPlayer
     * @param report - if true, a detailed report will be provided
     * @return - false if this is cannot be done
     */
    public boolean calculateIslandLevel(final CommandSender sender, final UUID targetPlayer, boolean report) {
        if (sender instanceof Player) {
            Player asker = (Player)sender;
            // Player asking for their own island calc
            if (asker.getUniqueId().equals(targetPlayer) || asker.isOp() || VaultHelper.hasPerm(asker, Settings.PERMPREFIX + "mod.info")) {
                // Newer better system - uses chunks
                if (!onLevelWaitTime(asker) || levelWait <= 0 || asker.isOp() || VaultHelper.hasPerm(asker, Settings.PERMPREFIX + "mod.info")) {
                    Util.sendMessage(asker, ChatColor.GREEN + "Calculating level, please wait...");
                    setLevelWaitTime(asker);
                    new LevelCalcByChunk(plugin, bSkyBlock, targetPlayer, asker, report);
                } else {
                    Util.sendMessage(asker, ChatColor.YELLOW + String.valueOf(getLevelWaitTime(asker)));
                }

            } else {
                // Asking for the level of another player
                Util.sendMessage(asker, ChatColor.GREEN + plugin.getLocale(asker.getUniqueId()).get("island.islandLevelIs").replace("[level]", String.valueOf(plugin.getIslandLevel(targetPlayer))));
            }
        } else {
            // Console request            
            //Util.sendMessage(sender, ChatColor.GREEN + bSkyBlock.myLocale().levelCalculating);
            new LevelCalcByChunk(plugin, bSkyBlock, targetPlayer, sender, report);
        }
        return true;
    }

    /**
     * Sets cool down for the level command
     * 
     * @param player
     */
    private void setLevelWaitTime(final Player player) {
        levelWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + levelWait * 1000));
    }

    private boolean onLevelWaitTime(final Player player) {
        if (levelWaitTime.containsKey(player.getUniqueId())) {
            if (levelWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
                return true;
            }

            return false;
        }

        return false;
    }

    private long getLevelWaitTime(final Player player) {
        if (levelWaitTime.containsKey(player.getUniqueId())) {
            if (levelWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
                return (levelWaitTime.get(player.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis()) / 1000;
            }

            return 0L;
        }

        return 0L;
    }
}
