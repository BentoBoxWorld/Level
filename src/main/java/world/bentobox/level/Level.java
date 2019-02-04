package world.bentobox.level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.commands.admin.AdminLevelCommand;
import world.bentobox.level.commands.admin.AdminTopCommand;
import world.bentobox.level.commands.island.IslandLevelCommand;
import world.bentobox.level.commands.island.IslandTopCommand;
import world.bentobox.level.config.Settings;
import world.bentobox.level.listeners.IslandTeamListeners;
import world.bentobox.level.listeners.JoinLeaveListener;
import world.bentobox.level.objects.LevelsData;
import world.bentobox.level.placeholders.LevelPlaceholder;
import world.bentobox.level.placeholders.TopTenNamePlaceholder;
import world.bentobox.level.placeholders.TopTenPlaceholder;
import world.bentobox.level.requests.LevelRequestHandler;

/**
 * Addon to BSkyBlock/AcidIsland that enables island level scoring and top ten functionality
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
     * @param targetPlayer - target player
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
            save();
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
        // Register commands for AcidIsland and BSkyBlock
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> settings.getGameModes().contains(gm.getDescription().getName()))
        .forEach(gm -> {
            log("Level hooking into " + gm.getDescription().getName());
            gm.getAdminCommand().ifPresent(adminCommand ->  {
                new AdminLevelCommand(this, adminCommand);
                new AdminTopCommand(this, adminCommand);
            });
            gm.getPlayerCommand().ifPresent(playerCmd -> {
                new IslandLevelCommand(this, playerCmd);
                new IslandTopCommand(this, playerCmd);
            });
            // Register placeholders
            if (getPlugin().getPlaceholdersManager() != null) {
                getPlugin().getPlaceholdersManager().registerPlaceholder(this, gm.getDescription().getName().toLowerCase() + "-island-level", new LevelPlaceholder(this, gm));
                // Top Ten
                for (int i = 1; i < 11; i++) {
                    getPlugin().getPlaceholdersManager().registerPlaceholder(this, gm.getDescription().getName().toLowerCase() + "-island-level-top-value-" + i, new TopTenPlaceholder(this, gm, i));
                    getPlugin().getPlaceholdersManager().registerPlaceholder(this, gm.getDescription().getName().toLowerCase() + "-island-level-top-name-" + i, new TopTenNamePlaceholder(this, gm, i));
                }
            }
        });

        // Register new island listener
        registerListener(new IslandTeamListeners(this));
        registerListener(new JoinLeaveListener(this));

        // Register request handlers
        registerRequestHandler(new LevelRequestHandler(this));



        // Done

    }

    /**
     * Save the levels to the database
     */
    private void save(){
        // No async for now
        levelsCache.values().forEach(handler::saveObject);
    }

    /**
     * Sets the player's level to a value
     * @param world - world
     * @param targetPlayer - target player
     * @param level - level
     */
    public void setIslandLevel(World world, UUID targetPlayer, long level) {
        if (world == null || targetPlayer == null) {
            this.logError("Level: request to store a null " + world + " " + targetPlayer);
            return;
        }
        LevelsData ld = getLevelsData(targetPlayer);
        if (ld == null) {
            ld = new LevelsData(targetPlayer, level, world);
        } else {
            ld.setLevel(world, level);
        }
        // Add to cache
        levelsCache.put(targetPlayer, ld);
        topTen.addEntry(world, targetPlayer, getIslandLevel(world, targetPlayer));
        handler.saveObject(ld);
    }

    /**
     * Sets the initial island level
     * @param island - island
     * @param level - initial calculated island level
     */
    public void setInitialIslandLevel(Island island, long level) {
        if (island.getWorld() == null || island.getOwner() == null) {
            this.logError("Level: request to store a null (initial) " + island.getWorld() + " " + island.getOwner());
            return;
        }
        setIslandLevel(island.getWorld(), island.getOwner(), level);
        levelsCache.get(island.getOwner()).setInitialIslandLevel(level);
    }

    public Database<LevelsData> getHandler() {
        return handler;
    }

    public void uncachePlayer(UUID uniqueId) {
        if (levelsCache.containsKey(uniqueId) && levelsCache.get(uniqueId) != null) {
            handler.saveObject(levelsCache.get(uniqueId));
        }
        levelsCache.remove(uniqueId);
    }

}
