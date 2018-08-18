package bentobox.addon.level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import bentobox.addon.level.commands.AdminLevel;
import bentobox.addon.level.commands.AdminTop;
import bentobox.addon.level.commands.IslandLevel;
import bentobox.addon.level.commands.IslandTop;
import bentobox.addon.level.config.Settings;
import bentobox.addon.level.database.object.LevelsData;
import bentobox.addon.level.listeners.JoinLeaveListener;
import bentobox.addon.level.listeners.NewIslandListener;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;


/**
 * Addon to BSkyBlock that enables island level scoring and top ten functionality
 * @author tastybento
 *
 */
public class Level extends Addon {

    // Settings
    private Settings settings;

    // Database handler for level data
    private Database<LevelsData> handler;

    // A cache of island levels.
    private Map<UUID, LevelsData> levelsCache;

    // The Top Ten object
    private TopTen topTen;

    // Level calculator
    private LevelPresenter levelCalc;

    /**
     * Calculates a user's island
     * @param world - the world where this island is
     * @param user - the user who is asking, or null if none
     * @param playerUUID - the target island member's UUID
     */
    public void calculateIslandLevel(World world, User user, UUID playerUUID) {
        levelCalc.calculateIslandLevel(world, user, playerUUID);
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

    /**
     * Load a player from the cache or database
     * @param targetPlayer - UUID of target player
     * @return LevelsData object or null if not found
     */
    public LevelsData getLevelsData(UUID targetPlayer) {
        // Get from database if not in cache
        if (!levelsCache.containsKey(targetPlayer) && handler.objectExists(targetPlayer.toString())) {
            levelsCache.put(targetPlayer, handler.loadObject(targetPlayer.toString()));
        }
        // Return cached value or null
        return levelsCache.get(targetPlayer);
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
        // Load the plugin's config
        settings = new Settings(this);
        // Get the BSkyBlock database
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = new Database<>(this, LevelsData.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Load the calculator
        levelCalc = new LevelPresenter(this, this.getPlugin());
        // Start the top ten and register it for clicks
        topTen = new TopTen(this);
        registerListener(topTen);
        // Register commands
        // AcidIsland hook in
        this.getPlugin().getAddonsManager().getAddonByName("AcidIsland").ifPresent(a -> {
            CompositeCommand acidIslandCmd = getPlugin().getCommandsManager().getCommand("ai");
            if (acidIslandCmd != null) {
                new IslandLevel(this, acidIslandCmd);
                new IslandTop(this, acidIslandCmd);
                CompositeCommand acidCmd = getPlugin().getCommandsManager().getCommand("acid");
                new AdminLevel(this, acidCmd);
                new AdminTop(this, acidCmd);
            }
        });
        // BSkyBlock hook in
        this.getPlugin().getAddonsManager().getAddonByName("BSkyBlock").ifPresent(a -> {
            CompositeCommand bsbIslandCmd = getPlugin().getCommandsManager().getCommand("island");
            if (bsbIslandCmd != null) {
                new IslandLevel(this, bsbIslandCmd);
                new IslandTop(this, bsbIslandCmd);
                CompositeCommand bsbAdminCmd = getPlugin().getCommandsManager().getCommand("bsbadmin");
                new AdminLevel(this, bsbAdminCmd);
                new AdminTop(this, bsbAdminCmd);
            }
        });

        // Register new island listener
        registerListener(new NewIslandListener(this));
        registerListener(new JoinLeaveListener(this));
        // Done

    }

    /**
     * Save the levels to the database
     * @param async - if true, saving will be done async
     */
    public void save(boolean async){
        // No async for now
        levelsCache.values().forEach(handler::saveObject);
    }

    /**
     * Sets the player's level to a value
     * @param world
     * @param targetPlayer
     * @param level
     */
    public void setIslandLevel(World world, UUID targetPlayer, long level) {
        LevelsData ld = getLevelsData(targetPlayer);
        if (ld == null) {
            ld = new LevelsData(targetPlayer, level, world);
        } else {
            ld.setLevel(world, level);
        }
        // Add to cache
        levelsCache.put(targetPlayer, ld);
        topTen.addEntry(world, targetPlayer, getIslandLevel(world, targetPlayer));
    }

    /**
     * Sets the initial island level
     * @param island - island
     * @param level - initial calculated island level
     */
    public void setInitialIslandLevel(Island island, long level) {
        setIslandLevel(island.getWorld(), island.getOwner(), level);
        levelsCache.get(island.getOwner()).setInitialIslandLevel(level);
    }


    public Database<LevelsData> getHandler() {
        return handler;
    }

    public void uncachePlayer(UUID uniqueId) {
        if (levelsCache.containsKey(uniqueId)) {
            handler.saveObject(levelsCache.get(uniqueId));
        }
        levelsCache.remove(uniqueId);
    }

}
