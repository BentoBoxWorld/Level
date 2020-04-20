package world.bentobox.level;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.calculators.CalcIslandLevel;
import world.bentobox.level.commands.admin.AdminLevelCommand;
import world.bentobox.level.commands.admin.AdminTopCommand;
import world.bentobox.level.commands.island.IslandLevelCommand;
import world.bentobox.level.commands.island.IslandTopCommand;
import world.bentobox.level.commands.island.IslandValueCommand;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.listeners.IslandTeamListeners;
import world.bentobox.level.listeners.JoinLeaveListener;
import world.bentobox.level.objects.LevelsData;
import world.bentobox.level.requests.LevelRequestHandler;
import world.bentobox.level.requests.TopTenRequestHandler;

/**
 * Addon to BSkyBlock/AcidIsland that enables island level scoring and top ten functionality
 * @author tastybento
 *
 */
public class Level extends Addon {

    private static Addon addon;

    // Settings
    private ConfigSettings settings;
    private Config<ConfigSettings> configObject = new Config<>(this, ConfigSettings.class);

    /**
     * @param settings the settings to set
     */
    public void setSettings(ConfigSettings settings) {
        this.settings = settings;
    }

    // Database handler for level data
    private Database<LevelsData> handler;

    // A cache of island levels.
    private Map<UUID, LevelsData> levelsCache;

    // The Top Ten object
    private TopTen topTen;

    // Level calculator
    private LevelPresenter levelPresenter;



    private BlockConfig blockConfig;

    /**
     * Calculates a user's island
     * @param world - the world where this island is
     * @param user - the user who is asking, or null if none
     * @param playerUUID - the target island member's UUID
     */
    public void calculateIslandLevel(World world, @Nullable User user, @NonNull UUID playerUUID) {
        levelPresenter.calculateIslandLevel(world, user, playerUUID);
    }

    /**
     * Get level from cache for a player.
     * @param targetPlayer - target player UUID
     * @return Level of player or zero if player is unknown or UUID is null
     */
    public long getIslandLevel(World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return 0L;
        LevelsData ld = getLevelsData(targetPlayer);
        return ld == null ? 0L : ld.getLevel(world);
    }

    /**
     * Load a player from the cache or database
     * @param targetPlayer - UUID of target player
     * @return LevelsData object or null if not found
     */
    @Nullable
    public LevelsData getLevelsData(@NonNull UUID targetPlayer) {
        // Get from database if not in cache
        if (!levelsCache.containsKey(targetPlayer) && handler.objectExists(targetPlayer.toString())) {
            LevelsData ld = handler.loadObject(targetPlayer.toString());
            if (ld != null) {
                levelsCache.put(targetPlayer, ld);
            } else {
                handler.deleteID(targetPlayer.toString());
            }
        }
        // Return cached value or null
        return levelsCache.get(targetPlayer);
    }

    /**
     * @return the settings
     */
    public ConfigSettings getSettings() {
        return settings;
    }

    public TopTen getTopTen() {
        return topTen;
    }

    /**
     * @return the levelPresenter
     */
    public LevelPresenter getLevelPresenter() {
        return levelPresenter;
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
    public void onLoad() {
        // Save the default config from config.yml
        saveDefaultConfig();
        // Load settings from config.yml. This will check if there are any issues with it too.
        if (loadSettings()) {
            configObject.saveConfigObject(settings);
        }
    }

    private boolean loadSettings() {
        // Load settings again to get worlds
        settings = configObject.loadConfigObject();
        if (settings == null) {
            // Disable
            logError("Level settings could not load! Addon disabled.");
            setState(State.DISABLED);
            return false;
        }
        // Check for legacy blocks and limits etc.
        if (getConfig().isConfigurationSection("blocks")
                || getConfig().isConfigurationSection("limits")
                || getConfig().isConfigurationSection("worlds")) {
            logWarning("Converting old config.yml format - shifting blocks, limits and worlds to blockconfig.yml");
            File blockConfigFile = new File(this.getDataFolder(), "blockconfig.yml");
            if (blockConfigFile.exists()) {
                logError("blockconfig.yml already exists! Saving config as blockconfig.yml.2");
                blockConfigFile = new File(this.getDataFolder(), "blockconfig.yml.2");
            }
            YamlConfiguration blockConfig = new YamlConfiguration();
            copyConfigSection(blockConfig, "limits");
            copyConfigSection(blockConfig, "blocks");
            copyConfigSection(blockConfig, "worlds");
            try {
                blockConfig.save(blockConfigFile);
            } catch (IOException e) {
                logError("Could not save! " + e.getMessage());
            }
        }
        return true;
    }

    private void copyConfigSection(YamlConfiguration blockConfig, String sectionName) {
        if (!getConfig().isConfigurationSection(sectionName)) return;
        ConfigurationSection section = getConfig().getConfigurationSection(sectionName);
        for (String k:section.getKeys(true)) {
            blockConfig.set(sectionName + "." + k, section.get(k));
        }
    }

    private void loadBlockSettings() {
        // Save the default blockconfig.yml
        this.saveResource("blockconfig.yml", false);

        YamlConfiguration blockValues = new YamlConfiguration();
        try {
            File file = new File(this.getDataFolder(), "blockconfig.yml");
            blockValues.load(file);
            // Load the block config class
            blockConfig = new BlockConfig(this, blockValues, file);
        } catch (IOException | InvalidConfigurationException e) {
            // Disable
            logError("Level blockconfig.yml settings could not load! Addon disabled.");
            setState(State.DISABLED);
            return;
        }

    }

    @Override
    public void onEnable() {
        addon = this;
        loadBlockSettings();
        // Get the BSkyBlock database
        // Set up the database handler to store and retrieve Island classes
        // Note that these are saved by the BSkyBlock database
        handler = new Database<>(this, LevelsData.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Load the calculator
        levelPresenter = new LevelPresenter(this, this.getPlugin());
        // Start the top ten and register it for clicks
        topTen = new TopTen(this);
        registerListener(topTen);
        // Register commands for GameModes
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> settings
                .getGameModes()
                .contains(gm
                        .getDescription()
                        .getName()))
        .forEach(gm -> {
            log("Level hooking into " + gm.getDescription().getName());
            registerCommands(gm);
            // Register placeholders
            if (getPlugin().getPlaceholdersManager() != null) {
                registerPlaceholders(gm);
            }
        });

        // Register new island listener
        registerListener(new IslandTeamListeners(this));
        registerListener(new JoinLeaveListener(this));

        // Register request handlers
        registerRequestHandler(new LevelRequestHandler(this));
        registerRequestHandler(new TopTenRequestHandler(this));

        // Check if WildStackers is enabled on the server
        // I only added support for counting blocks into the island level
        // Someone else can PR if they want spawners added to the Leveling system :)
        CalcIslandLevel.stackersEnabled = Bukkit.getPluginManager().getPlugin("WildStacker") != null;

        // Done
    }

    private void registerPlaceholders(GameModeAddon gm) {
        // Island Level
        getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                gm.getDescription().getName().toLowerCase() + "_island_level",
                user -> getLevelPresenter().getLevelString(getIslandLevel(gm.getOverWorld(), user.getUniqueId())));

        // Visited Island Level
        getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                gm.getDescription().getName().toLowerCase() + "_visited_island_level", user -> getVisitedIslandLevel(gm, user));

        // Top Ten
        for (int i = 1; i <= 10; i++) {
            final int rank = i;
            // Value
            getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                    gm.getDescription().getName().toLowerCase() + "_top_value_" + rank, u -> String.valueOf(getTopTen().getTopTenLevel(gm.getOverWorld(), rank)));

            // Name
            getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                    gm.getDescription().getName().toLowerCase() + "_top_name_" + rank,
                    u -> getPlayers().getName(getTopTen().getTopTenUUID(gm.getOverWorld(), rank)));
        }

    }

    private String getVisitedIslandLevel(GameModeAddon gm, User user) {
        if (!gm.inWorld(user.getLocation())) return "";
        return getIslands().getIslandAt(user.getLocation())
                .map(island -> getLevelPresenter().getLevelString(getIslandLevel(gm.getOverWorld(), island.getOwner())))
                .orElse("0");
    }

    private void registerCommands(GameModeAddon gm) {
        gm.getAdminCommand().ifPresent(adminCommand ->  {
            new AdminLevelCommand(this, adminCommand);
            new AdminTopCommand(this, adminCommand);
        });
        gm.getPlayerCommand().ifPresent(playerCmd -> {
            new IslandLevelCommand(this, playerCmd);
            new IslandTopCommand(this, playerCmd);
            new IslandValueCommand(this, playerCmd);
        });
    }

    /**
     * Save the levels to the database
     */
    private void save(){
        // Remove any potential null values from the cache
        levelsCache.values().removeIf(Objects::isNull);
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
     * Zeros the initial island level
     * @param island - island
     * @param level - initial calculated island level
     */
    public void setInitialIslandLevel(@NonNull Island island, long level) {
        if (island.getWorld() == null || island.getOwner() == null) {
            this.logError("Level: request to store a null (initial) " + island.getWorld() + " " + island.getOwner());
            return;
        }
        setIslandLevel(island.getWorld(), island.getOwner(), 0L);
        levelsCache.get(island.getOwner()).setInitialLevel(island.getWorld(), level);
    }

    /**
     * Get the initial island level
     * @param island - island
     * @return level or 0 by default
     */
    public long getInitialIslandLevel(@NonNull Island island) {
        // Remove any potential null values from the cache
        levelsCache.values().removeIf(Objects::isNull);
        return levelsCache.containsKey(island.getOwner()) ? levelsCache.get(island.getOwner()).getInitialLevel(island.getWorld()) : 0L;
    }

    /**
     * @return database handler
     */
    @Nullable
    public Database<LevelsData> getHandler() {
        return handler;
    }

    public static Addon getInstance() {
        return addon;
    }

    /**
     * @return the blockConfig
     */
    public BlockConfig getBlockConfig() {
        return blockConfig;
    }

}
