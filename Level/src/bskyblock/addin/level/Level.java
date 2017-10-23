package bskyblock.addin.level;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import bskyblock.addin.level.config.LocaleManager;
import bskyblock.addin.level.config.PluginConfig;
import bskyblock.addin.level.database.object.Levels;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

public class Level extends JavaPlugin {

    private BSkyBlock bSkyBlock;

    // Level calc checker
    BukkitTask checker = null;

    private LocaleManager localeManager;

    private AbstractDatabaseHandler<Levels> handler;

    private BSBDatabase database;

    private Levels levelsCache;

    @Override
    public void onEnable() {
        // Load the plugin's config
        new PluginConfig(this);
        // Get the BSkyBlock plugin
        bSkyBlock = BSkyBlock.getPlugin();
        if (!bSkyBlock.isEnabled()) {
            this.setEnabled(false);
            return;
        }
        // Set up database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = (AbstractDatabaseHandler<Levels>) database.getHandler(bSkyBlock, Levels.class);
        // Load the levels to a cache
        levelsCache = new Levels();
        try {
            levelsCache = handler.loadObject("addon-levels");
            if (levelsCache == null) {
                levelsCache = new Levels(); 
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | SecurityException | ClassNotFoundException | IntrospectionException
                | SQLException e) {
            e.printStackTrace();
        }
        // Start the top ten
        new TopTen(this);
        // Local locales
        localeManager = new LocaleManager(this);
        // Register commands
        new Commands(this);
        // Done
    }

    @Override
    public void onDisable(){
        // Save the cache
        if (levelsCache != null) {
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
                handler.saveObject(levelsCache);
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
     * Get level from cache for a player
     * @param targetPlayer
     * @return Level of player
     */
    public Long getIslandLevel(UUID targetPlayer) {
        //getLogger().info("DEBUG: getting island level for " + bSkyBlock.getPlayers().getName(targetPlayer));
        return levelsCache.getLevel(targetPlayer);
    }

    /**
     * Save the player's level in the local cache 
     * @param targetPlayer
     * @param level
     */
    public void setIslandLevel(UUID targetPlayer, long level) {
        //getLogger().info("DEBUG: set island level to " + level + " for " + bSkyBlock.getPlayers().getName(targetPlayer));
        levelsCache.addLevel(targetPlayer, level);
        save(true);
    }

    /**
     * Get the locale for this player
     * @param sender
     * @return Locale object for sender
     */
    public BSBLocale getLocale(CommandSender sender) {
        return localeManager.getLocale(sender);
    }

    /**
     * Get the locale for this UUID
     * @param uuid
     * @return Locale object for UUID
     */
    public BSBLocale getLocale(UUID uuid) {
        return localeManager.getLocale(uuid);
    }

}
