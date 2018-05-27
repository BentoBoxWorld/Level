package bskyblock.addon.level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import bskyblock.addon.level.commands.AdminLevel;
import bskyblock.addon.level.commands.AdminTop;
import bskyblock.addon.level.commands.IslandLevel;
import bskyblock.addon.level.commands.IslandTop;
import bskyblock.addon.level.config.Settings;
import bskyblock.addon.level.database.object.LevelsData;
import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
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
    private BSBDatabase<LevelsData> handler;

    // A cache of island levels. 
    private Map<UUID, LevelsData> levelsCache;

    // The Top Ten object
    private TopTen topTen;

    // Level calculator
    private LevelPresenter levelCalc;

    /**
     * Calculates a user's island
     * @param world 
     * @param user
     * @param playerUUID - the player's UUID
     * @param b
     * @param permPrefix 
     */
    public void calculateIslandLevel(World world, User user, UUID playerUUID, boolean b, String permPrefix) {
        levelCalc.calculateIslandLevel(world, user, playerUUID, b, permPrefix);        
    }

    /**
     * Get level from cache for a player
     * @param targetPlayer
     * @return Level of player
     */
    public long getIslandLevel(World world, UUID targetPlayer) { 
        LevelsData ld = getLevelsData(targetPlayer);
        return ld == null ? 0L : ld.getLevel(world);
    }

    private LevelsData getLevelsData(UUID targetPlayer) {
        // Load player
        return levelsCache.getOrDefault(targetPlayer, handler.loadObject(targetPlayer.toString()));
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

    @Override
    public void onDisable(){
        // Save the cache
        if (levelsCache != null) {
            save(false);
        }
        if (topTen != null) {
            topTen.saveTopTen();
        }
    }

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
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = new BSBDatabase<>(this, LevelsData.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Load the calculator
        levelCalc = new LevelPresenter(this, getBSkyBlock());
        // Start the top ten and register it for clicks
        topTen = new TopTen(this);
        registerListener(topTen);
        // Register commands - run one tick later to allow all addons to load
        // AcidIsland hook in
        getServer().getScheduler().runTask(getBSkyBlock(), () -> {
            this.getBSkyBlock().getAddonsManager().getAddonByName("AcidIsland").ifPresent(a -> {
                getLogger().info("DEBUG: " + getBSkyBlock().getCommandsManager().listCommands());
                CompositeCommand acidIslandCmd = getBSkyBlock().getCommandsManager().getCommand("ai");
                new IslandLevel(this, acidIslandCmd);
                new IslandTop(this, acidIslandCmd);
                CompositeCommand acidCmd = getBSkyBlock().getCommandsManager().getCommand("acid");
                new AdminLevel(this, acidCmd);
                new AdminTop(this, acidCmd);
            });
        });
        // BSkyBlock hook in
        CompositeCommand bsbIslandCmd = getBSkyBlock().getCommandsManager().getCommand("island");
        new IslandLevel(this, bsbIslandCmd);
        new IslandTop(this, bsbIslandCmd);
        CompositeCommand bsbAdminCmd = getBSkyBlock().getCommandsManager().getCommand("bsbadmin");
        new AdminLevel(this, bsbAdminCmd);
        new AdminTop(this, bsbAdminCmd);
        // Done
    }

    /**
     * Save the levels to the database
     * @param async - if true, saving will be done async
     */
    public void save(boolean async){
        Runnable save = () -> levelsCache.values().forEach(handler::saveObject);
        if(async){
            getServer().getScheduler().runTaskAsynchronously(getBSkyBlock(), save);
        } else {
            save.run();
        }
    }

    /**
     * Sets the player's level to a value
     * @param world 
     * @param targetPlayer
     * @param level
     * @param permPrefix
     */
    protected void setIslandLevel(World world, UUID targetPlayer, long level, String permPrefix) {
        LevelsData ld = getLevelsData(targetPlayer);
        if (ld == null) {
            ld = new LevelsData(targetPlayer, level, world);
        } else {
            ld.setLevel(world, level);
        }
        // Add to cache
        levelsCache.put(targetPlayer, ld);
        topTen.addEntry(world, targetPlayer, level, permPrefix);
    }

    public BSBDatabase<LevelsData> getHandler() {
        return handler;
    }

}
