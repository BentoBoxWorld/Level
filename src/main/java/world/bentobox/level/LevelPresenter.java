package world.bentobox.level;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.calculators.PlayerLevel;

public class LevelPresenter {

    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    private static final TreeMap<BigInteger, String> LEVELS;
    static {
        LEVELS = new TreeMap<>();

        LEVELS.put(THOUSAND, "k");
        LEVELS.put(THOUSAND.pow(2), "M");
        LEVELS.put(THOUSAND.pow(3), "G");
        LEVELS.put(THOUSAND.pow(4), "T");
    }

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
            long lvl = addon.getIslandLevel(world, targetPlayer);

            sender.sendMessage("island.level.island-level-is","[level]", getLevelString(lvl));
        }
    }

    /**
     * Get the string representation of the level. May be converted to shorthand notation, e.g., 104556 = 10.5k
     * @param lvl - long value to represent
     * @return string of the level.
     */
    public String getLevelString(long lvl) {
        String level = String.valueOf(lvl);
        // Asking for the level of another player
        if(addon.getSettings().isShorthand()) {
            BigInteger levelValue = BigInteger.valueOf(lvl);

            Map.Entry<BigInteger, String> stage = LEVELS.floorEntry(levelValue);

            if (stage != null) { // level > 1000
                // 1 052 -> 1.0k
                // 1 527 314 -> 1.5M
                // 3 874 130 021 -> 3.8G
                // 4 002 317 889 -> 4.0T
                level = new DecimalFormat("#.#").format(levelValue.divide(stage.getKey().divide(THOUSAND)).doubleValue()/1000.0) + stage.getValue();
            }
        }
        return level;
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
