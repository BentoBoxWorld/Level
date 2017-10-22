package bskyblock.addin.level;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import bskyblock.addin.level.config.LocaleManager;
import bskyblock.addin.level.config.PluginConfig;
import bskyblock.addin.level.database.object.Levels;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.ArgumentHandler;
import us.tastybento.bskyblock.api.commands.CanUseResp;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class Level extends JavaPlugin {

    private BSkyBlock bSkyBlock;

    // Level calc cool down
    private HashMap<UUID, Long> levelWaitTime = new HashMap<UUID, Long>();

    // Level calc checker
    BukkitTask checker = null;

    private int levelWait;

    private LocaleManager localeManager;

    private HashMap<UUID, Long> islandLevel;

    private AbstractDatabaseHandler<Levels> handler;

    private BSBDatabase database;

    private Levels levelsDatabase;


    @Override
    public void onEnable() {
        new PluginConfig(this);
        bSkyBlock = BSkyBlock.getPlugin();
        islandLevel = new HashMap<>();
        // Set up database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        handler = (AbstractDatabaseHandler<Levels>) database.getHandler(bSkyBlock, Levels.class);
        try {
            levelsDatabase = handler.loadObject("addon-levels");
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | SecurityException | ClassNotFoundException | IntrospectionException
                | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new TopTen(this);
        // Local locales
        localeManager = new LocaleManager(this);
        bSkyBlock.getIslandCommand().addSubCommand(new ArgumentHandler("island") {

            @Override
            public CanUseResp canUse(CommandSender sender) {
                return new CanUseResp(true);
            }

            @Override
            public void execute(CommandSender sender, String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    UUID playerUUID = player.getUniqueId();

                    if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.info")) {
                        if (!bSkyBlock.getPlayers().inTeam(playerUUID) && !bSkyBlock.getPlayers().hasIsland(playerUUID)) {
                            Util.sendMessage(player, ChatColor.RED + bSkyBlock.getLocale(sender).get("error.noisland"));
                            return;
                        } else {
                            calculateIslandLevel(player, playerUUID);
                            return;
                        }
                    } else {
                        //Util.sendMessage(player, ChatColor.RED + bSkyBlock.myLocale(playerUUID).errorNoPermission);
                        return;
                    }
                }
            }

            @Override
            public Set<String> tabComplete(CommandSender sender, String[] args) {
                return null;
            }

            @Override
            public String[] usage(CommandSender sender) {
                return new String[]{null, "Calculate your island's level"};
            }
        }.alias("level"));


    }

    @Override
    public void onDisable(){
        if (levelsDatabase != null) {
            save(false);
        }
    }
    
    /**
     * Save the levels to the database
     * @param async - if true, saving will be done async
     */
    public void save(boolean async){
        Runnable save = () -> {
            try {
                handler.saveObject(levelsDatabase);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                    | InstantiationException | NoSuchMethodException | IntrospectionException | SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        };
        if(async){
            getServer().getScheduler().runTaskAsynchronously(this, save);
        } else {
            save.run();
        }
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
                    new LevelCalcByChunk(this, bSkyBlock, targetPlayer, asker, report);
                } else {
                    Util.sendMessage(asker, ChatColor.YELLOW + String.valueOf(getLevelWaitTime(asker)));
                }

            } else {
                // Asking for the level of another player
                //Util.sendMessage(asker, ChatColor.GREEN + bSkyBlock.myLocale(asker.getUniqueId()).islandislandLevelis.replace("[level]", String.valueOf(bSkyBlock.getIslands().getIslandLevel(targetPlayer))));
            }
        } else {
            // Console request            
            //Util.sendMessage(sender, ChatColor.GREEN + bSkyBlock.myLocale().levelCalculating);
            new LevelCalcByChunk(this, bSkyBlock, targetPlayer, sender, report);
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

    public boolean onLevelWaitTime(final Player player) {
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

    public Long getIslandLevel(UUID targetPlayer) {
        //getLogger().info("DEBUG: getting island level for " + bSkyBlock.getPlayers().getName(targetPlayer));
        return levelsDatabase.getLevel(targetPlayer);
    }

    public void setIslandLevel(UUID targetPlayer, long level) {
        //getLogger().info("DEBUG: set island level to " + level + " for " + bSkyBlock.getPlayers().getName(targetPlayer));
        levelsDatabase.addLevel(targetPlayer, level);
        save(true);
    }

    /**
     * @param sender
     * @return Locale object for sender
     */
    public BSBLocale getLocale(CommandSender sender) {
        return localeManager.getLocale(sender);
    }

    /**
     * @param uuid
     * @return Locale object for UUID
     */
    public BSBLocale getLocale(UUID uuid) {
        return localeManager.getLocale(uuid);
    }

}
