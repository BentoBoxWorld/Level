package bskyblock.addon.level;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import bskyblock.addon.level.commands.AdminLevel;
import bskyblock.addon.level.commands.AdminTop;
import bskyblock.addon.level.commands.IslandLevel;
import bskyblock.addon.level.commands.IslandTop;
import bskyblock.addon.level.config.Settings;
import bskyblock.addon.level.database.object.LevelsData;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDatabase;

/**
 * Addon to BSkyBlock that enables island level scoring and top ten functionality
 * @author tastybento
 *
 */
public class Level extends Addon {
    
    // Settings
    private Settings settings;

    // Database handler for level data
    private AbstractDatabaseHandler<LevelsData> handler;

    // The BSkyBlock database object
    private BSBDatabase database;

    // A cache of island levels. Island levels are not kept in memory unless required.
    // The cache is saved when the server shuts down and the plugin is disabled.
    // TODO: Save regularly to avoid crash issues.
    private Map<UUID, Long> levelsCache;

    // The Top Ten object
    private TopTen topTen;
    
    // Level calculator
    private LevelPresenter levelCalc;

    /**
     * Calculates a user's island
     * @param user
     * @param playerUUID - the player's UUID
     * @param b
     */
    public void calculateIslandLevel(User user, UUID playerUUID, boolean b) {
        levelCalc.calculateIslandLevel(user, playerUUID, b);        
    }

    public AbstractDatabaseHandler<LevelsData> getHandler() {
        return handler;
    }
    
    /**
     * Get level from cache for a player
     * @param targetPlayer
     * @return Level of player
     */
    public long getIslandLevel(UUID targetPlayer) {
        if (levelsCache.containsKey(targetPlayer)) {
            return levelsCache.get(targetPlayer);
        }
        // Get from database
        LevelsData level;
        try {
            level = handler.loadObject(targetPlayer.toString());
            if (level == null) {
                // We do not know this player, set to zero
                return 0;
            }
            levelsCache.put(targetPlayer, level.getLevel());
            return level.getLevel();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | ClassNotFoundException | IntrospectionException e) {
            getLogger().severe("Could not load player's level! " + e.getMessage());
        }
        return 0;
    }

    /**
     * @return the settings
     */
    public final Settings getSettings() {
        return settings;
    }
    
    public TopTen getTopTen() {
        return topTen;
    }

    private void load() {
        try {
            for (LevelsData level : handler.loadObjects()) {
                levelsCache.put(UUID.fromString(level.getUniqueId()), level.getLevel());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | ClassNotFoundException | IntrospectionException e) {
            getLogger().severe("Could not load levels cache data! " + e.getMessage());
        }
    }

    @Override
    public void onDisable(){
        // Save the cache
        if (levelsCache != null) {
            save(false);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        // Check if it is enabled - it might be loaded, but not enabled.
        if (getBSkyBlock() == null || !getBSkyBlock().isEnabled()) {
            getLogger().severe("BSkyBlock does not exist or is not enabled. Stopping.");
            this.setEnabled(false);
            return;
        }
        // Load the plugin's config
        settings = new Settings(this);
        // Get the BSkyBlock database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = (AbstractDatabaseHandler<LevelsData>) database.getHandler(LevelsData.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Load all the levels
        load();
        // Load the calculator
        levelCalc = new LevelPresenter(this);
        // Start the top ten and register it for clicks
        topTen = new TopTen(this);
        registerListener(topTen);
        // Register commands
        CompositeCommand bsbIslandCmd = getBSkyBlock().getCommandsManager().getCommand(Constants.ISLANDCOMMAND);
        new IslandLevel(this, bsbIslandCmd);
        new IslandTop(this, bsbIslandCmd);
        CompositeCommand bsbAdminCmd = getBSkyBlock().getCommandsManager().getCommand(Constants.ADMINCOMMAND);
        new AdminLevel(this, bsbAdminCmd);
        new AdminTop(this, bsbAdminCmd);
        // Done
    }

    /**
     * Save the levels to the database
     * @param async - if true, saving will be done async
     */
    public void save(boolean async){
        Runnable save = () -> {
            try {
                for (Entry<UUID, Long> en : levelsCache.entrySet()) {
                    LevelsData lv = new LevelsData();
                    lv.setLevel(en.getValue());
                    lv.setUniqueId(en.getKey().toString());
                    handler.saveObject(lv);
                }
            } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
                getLogger().severe("Could not save levels async! " + e.getMessage());
            }
        };
        if(async){
            getServer().getScheduler().runTaskAsynchronously(getBSkyBlock(), save);
        } else {
            save.run();
        }
    }

    /**
     * Sets the player's level to a value
     * @param targetPlayer
     * @param level
     */
    protected void setIslandLevel(UUID targetPlayer, long level) {
        // Add to cache
        levelsCache.put(targetPlayer, level);
        topTen.addEntry(targetPlayer, level);
    }

}
