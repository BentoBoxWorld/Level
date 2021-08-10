package world.bentobox.level;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.commands.AdminLevelCommand;
import world.bentobox.level.commands.AdminLevelStatusCommand;
import world.bentobox.level.commands.AdminSetInitialLevelCommand;
import world.bentobox.level.commands.AdminTopCommand;
import world.bentobox.level.commands.IslandLevelCommand;
import world.bentobox.level.commands.IslandTopCommand;
import world.bentobox.level.commands.IslandValueCommand;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.listeners.IslandActivitiesListeners;
import world.bentobox.level.listeners.JoinLeaveListener;
import world.bentobox.level.objects.LevelsData;
import world.bentobox.level.requests.LevelRequestHandler;
import world.bentobox.level.requests.TopTenRequestHandler;

/**
 * @author tastybento
 *
 */
public class Level extends Addon implements Listener {

    // The 10 in top ten
    public static final int TEN = 10;

    // Settings
    private ConfigSettings settings;
    private Config<ConfigSettings> configObject = new Config<>(this, ConfigSettings.class);
    private BlockConfig blockConfig;
    private Pipeliner pipeliner;
    private LevelsManager manager;
    private boolean stackersEnabled;
    private boolean advChestEnabled;
    private boolean roseStackersEnabled;
    private final List<GameModeAddon> registeredGameModes = new ArrayList<>();

    @Override
    public void onLoad() {
        // Save the default config from config.yml
        saveDefaultConfig();
        if (loadSettings()) {
            // Disable
            logError("Level settings could not load! Addon disabled.");
            setState(State.DISABLED);
        } else {
            configObject.saveConfigObject(settings);
        }
    }

    private boolean loadSettings() {
        // Load settings again to get worlds
        settings = configObject.loadConfigObject();
        return settings == null;
    }

    @Override
    public void onEnable() {
        loadBlockSettings();
        // Start pipeline
        pipeliner = new Pipeliner(this);
        // Start Manager
        manager = new LevelsManager(this);
        // Register listeners
        this.registerListener(new IslandActivitiesListeners(this));
        this.registerListener(new JoinLeaveListener(this));
        this.registerListener(this);
        // Register commands for GameModes
        registeredGameModes.clear();
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName()))
        .forEach(gm -> {
            log("Level hooking into " + gm.getDescription().getName());
            registerCommands(gm);
            registerPlaceholders(gm);
            registeredGameModes.add(gm);
        });
        // Register request handlers
        registerRequestHandler(new LevelRequestHandler(this));
        registerRequestHandler(new TopTenRequestHandler(this));

        // Check if WildStackers is enabled on the server
        // I only added support for counting blocks into the island level
        // Someone else can PR if they want spawners added to the Leveling system :)
        stackersEnabled = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
        if (stackersEnabled) {
            log("Hooked into WildStackers.");
        }
        // Check if AdvancedChests is enabled on the server
        Plugin advChest = Bukkit.getPluginManager().getPlugin("AdvancedChests");
        advChestEnabled = advChest != null;
        if (advChestEnabled) {
            // Check version
            if (compareVersions(advChest.getDescription().getVersion(), "14.2") > 0) {
                log("Hooked into AdvancedChests.");
            } else {
                logError("Could not hook into AdvancedChests " + advChest.getDescription().getVersion() + " - requires version 14.3 or later");
                advChestEnabled = false;
            }
        }
        // Check if RoseStackers is enabled
        roseStackersEnabled = Bukkit.getPluginManager().isPluginEnabled("RoseStacker");
        if (roseStackersEnabled) {
            log("Hooked into RoseStackers.");
        }
    }

    /**
     * Compares versions
     * @param version1
     * @param version2
     * @return <0 if version 1 is older than version 2, =0 if the same, >0 if version 1 is newer than version 2
     */
    public static int compareVersions(String version1, String version2) {
        int comparisonResult = 0;

        String[] version1Splits = version1.split("\\.");
        String[] version2Splits = version2.split("\\.");
        int maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length);

        for (int i = 0; i < maxLengthOfVersionSplits; i++){
            Integer v1 = i < version1Splits.length ? Integer.parseInt(version1Splits[i]) : 0;
            Integer v2 = i < version2Splits.length ? Integer.parseInt(version2Splits[i]) : 0;
            int compare = v1.compareTo(v2);
            if (compare != 0) {
                comparisonResult = compare;
                break;
            }
        }
        return comparisonResult;
    }

    @EventHandler
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Perform upgrade check
        manager.migrate();
        // Load TopTens
        manager.loadTopTens();
        /*
         * DEBUG code to generate fake islands and then try to level them all.
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            getPlugin().getAddonsManager().getGameModeAddons().stream()
            .filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName()))
            .forEach(gm -> {
                for (int i = 0; i < 1000; i++) {
                    try {
                        NewIsland.builder().addon(gm).player(User.getInstance(UUID.randomUUID())).name("default").reason(Reason.CREATE).noPaste().build();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            });
            // Queue all islands DEBUG

            getIslands().getIslands().stream().filter(Island::isOwned).forEach(is -> {

                this.getManager().calculateLevel(is.getOwner(), is).thenAccept(r ->
                log("Result for island calc " + r.getLevel() + " at " + is.getCenter()));

            });
       }, 60L);*/
    }


    private void registerPlaceholders(GameModeAddon gm) {
        if (getPlugin().getPlaceholdersManager() == null) return;
        // Island Level
        getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                gm.getDescription().getName().toLowerCase() + "_island_level",
                user -> getManager().getIslandLevelString(gm.getOverWorld(), user.getUniqueId()));
        getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                gm.getDescription().getName().toLowerCase() + "_points_to_next_level",
                user -> getManager().getPointsToNextString(gm.getOverWorld(), user.getUniqueId()));

        // Visited Island Level
        getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                gm.getDescription().getName().toLowerCase() + "_visited_island_level", user -> getVisitedIslandLevel(gm, user));

        // Register Top Ten Placeholders
        for (int i = 1; i < 11; i++) {
            final int rank = i;
            // Name
            getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                    gm.getDescription().getName().toLowerCase() + "_top_name_" + i, u -> getRankName(gm.getOverWorld(), rank));
            // Level
            getPlugin().getPlaceholdersManager().registerPlaceholder(this,
                    gm.getDescription().getName().toLowerCase() + "_top_value_" + i, u -> getRankLevel(gm.getOverWorld(), rank));
        }

    }

    String getRankName(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > TEN) rank = TEN;
        return getPlayers().getName(getManager().getTopTen(world, TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst().orElse(null));
    }

    String getRankLevel(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > TEN) rank = TEN;
        return getManager()
                .formatLevel(getManager()
                        .getTopTen(world, TEN)
                        .values()
                        .stream()
                        .skip(rank - 1L)
                        .limit(1L)
                        .findFirst()
                        .orElse(null));
    }

    String getVisitedIslandLevel(GameModeAddon gm, User user) {
        if (!gm.inWorld(user.getLocation())) return "";
        return getIslands().getIslandAt(user.getLocation())
                .map(island -> getManager().getIslandLevelString(gm.getOverWorld(), island.getOwner()))
                .orElse("0");
    }


    private void registerCommands(GameModeAddon gm) {
        gm.getAdminCommand().ifPresent(adminCommand ->  {
            new AdminLevelCommand(this, adminCommand);
            new AdminTopCommand(this, adminCommand);
            new AdminLevelStatusCommand(this, adminCommand);
            if (getSettings().isZeroNewIslandLevels()) {
                new AdminSetInitialLevelCommand(this, adminCommand);
            }
        });
        gm.getPlayerCommand().ifPresent(playerCmd -> {
            new IslandLevelCommand(this, playerCmd);
            new IslandTopCommand(this, playerCmd);
            new IslandValueCommand(this, playerCmd);
        });
    }

    @Override
    public void onDisable() {
        // Stop the pipeline
        this.getPipeliner().stop();
        // Save player data and the top tens
        if (manager != null) {
            manager.save();
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
        }

    }


    /**
     * @return the blockConfig
     */
    public BlockConfig getBlockConfig() {
        return blockConfig;
    }

    /**
     * @return the settings
     */
    public ConfigSettings getSettings() {
        return settings;
    }

    /**
     * @return the pipeliner
     */
    public Pipeliner getPipeliner() {
        return pipeliner;
    }

    /**
     * @return the manager
     */
    public LevelsManager getManager() {
        return manager;
    }

    /**
     * Set the config settings - used for tests only
     * @param configSettings - config settings
     */
    void setSettings(ConfigSettings configSettings) {
        this.settings = configSettings;

    }

    /**
     * @return the stackersEnabled
     */
    public boolean isStackersEnabled() {
        return stackersEnabled;
    }

    /**
     * @return the advChestEnabled
     */
    public boolean isAdvChestEnabled() {
        return advChestEnabled;
    }

    /**
     * Get level from cache for a player.
     * @param targetPlayer - target player UUID
     * @return Level of player or zero if player is unknown or UUID is null
     */
    public long getIslandLevel(World world, @Nullable UUID targetPlayer) {
        return getManager().getIslandLevel(world, targetPlayer);
    }

    /**
     * Sets the player's level to a value
     * @param world - world
     * @param targetPlayer - target player
     * @param level - level
     */
    public void setIslandLevel(World world, UUID targetPlayer, long level) {
        getManager().setIslandLevel(world, targetPlayer, level);
    }

    /**
     * Zeros the initial island level
     * @param island - island
     * @param level - initial calculated island level
     */
    public void setInitialIslandLevel(@NonNull Island island, long level) {
        getManager().setInitialIslandLevel(island, level);
    }

    /**
     * Get the initial island level
     * @param island - island
     * @return level or 0 by default
     */
    public long getInitialIslandLevel(@NonNull Island island) {
        return getManager().getInitialLevel(island);
    }

    /**
     * Calculates a user's island
     * @param world - the world where this island is
     * @param user - not used! See depecration message
     * @param playerUUID - the target island member's UUID
     * @deprecated Do not use this anymore. Use getManager().calculateLevel(playerUUID, island)
     */
    @Deprecated
    public void calculateIslandLevel(World world, @Nullable User user, @NonNull UUID playerUUID) {
        Island island = getIslands().getIsland(world, playerUUID);
        if (island != null) getManager().calculateLevel(playerUUID, island);
    }

    /**
     * Provide the levels data for the target player
     * @param targetPlayer - UUID of target player
     * @return LevelsData object or null if not found. Only island levels are set!
     * @deprecated Do not use this anymore. Use {@link #getIslandLevel(World, UUID)}
     */
    @Deprecated
    public LevelsData getLevelsData(UUID targetPlayer) {
        LevelsData ld = new LevelsData(targetPlayer);
        getPlugin().getAddonsManager().getGameModeAddons().stream()
        .filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName()))
        .forEach(gm -> {
            if (getSettings().isZeroNewIslandLevels()) {
                Island island = getIslands().getIsland(gm.getOverWorld(), targetPlayer);
                if (island != null) {
                    ld.setInitialLevel(gm.getOverWorld(), this.getInitialIslandLevel(island));
                }
            }
            ld.setLevel(gm.getOverWorld(), this.getIslandLevel(gm.getOverWorld(), targetPlayer));
        });
        return ld;
    }

    /**
     * @return the registeredGameModes
     */
    public List<GameModeAddon> getRegisteredGameModes() {
        return registeredGameModes;
    }

    /**
     * Check if Level addon is active in game mode
     * @param gm Game Mode Addon
     * @return true if active, false if not
     */
    public boolean isRegisteredGameMode(GameModeAddon gm) {
        return registeredGameModes.contains(gm);
    }

    /**
     * Checks if Level addon is active in world
     * @param world world
     * @return true if active, false if not
     */
    public boolean isRegisteredGameModeWorld(World world) {
        return registeredGameModes.stream().map(GameModeAddon::getOverWorld).anyMatch(w -> Util.sameWorld(world, w));
    }

    /**
     * @return the roseStackersEnabled
     */
    public boolean isRoseStackersEnabled() {
        return roseStackersEnabled;
    }

}
