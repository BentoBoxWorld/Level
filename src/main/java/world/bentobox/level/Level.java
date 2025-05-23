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
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.commands.AdminLevelCommand;
import world.bentobox.level.commands.AdminLevelStatusCommand;
import world.bentobox.level.commands.AdminSetInitialLevelCommand;
import world.bentobox.level.commands.AdminStatsCommand;
import world.bentobox.level.commands.AdminTopCommand;
import world.bentobox.level.commands.IslandDetailCommand;
import world.bentobox.level.commands.IslandLevelCommand;
import world.bentobox.level.commands.IslandTopCommand;
import world.bentobox.level.commands.IslandValueCommand;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.listeners.IslandActivitiesListeners;
import world.bentobox.level.listeners.JoinLeaveListener;
import world.bentobox.level.listeners.MigrationListener;
import world.bentobox.level.requests.LevelRequestHandler;
import world.bentobox.level.requests.TopTenRequestHandler;
import world.bentobox.visit.VisitAddon;
import world.bentobox.warps.Warp;

/**
 * @author tastybento
 *
 */
public class Level extends Addon {

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
	private boolean ultimateStackerEnabled;
	private final List<GameModeAddon> registeredGameModes = new ArrayList<>();

	/**
	 * Local variable that stores if warpHook is present.
	 */
	private Warp warpHook;

	/**
	 * Local variable that stores if visitHook is present.
	 */
	private VisitAddon visitHook;

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

		// Save existing panels.
		this.saveResource("panels/top_panel.yml", false);
		this.saveResource("panels/detail_panel.yml", false);
		this.saveResource("panels/value_panel.yml", false);
	}

	private boolean loadSettings() {
		// Load settings again to get worlds
		settings = configObject.loadConfigObject();

		return settings == null;
	}

	@Override
	public void onEnable() {
        // Everything waits until allLoaded
    }

    @Override
    public void allLoaded() {
        super.allLoaded();
        loadBlockSettings();
        initializePipelineAndManager();
        registerAllListeners();
        registerGameModeCommands();
        registerRequestHandlers();
        hookPlugin("WildStacker", this::hookWildStackers);
        hookAdvancedChests();
        hookPlugin("RoseStacker", this::hookRoseStackers);
        hookPlugin("UltimateStacker", this::hookUltimateStacker);

        if (this.isEnabled()) {
            hookExtensions();
        }
    }

    private void initializePipelineAndManager() {
        pipeliner = new Pipeliner(this);
        manager = new LevelsManager(this);
    }

    private void registerAllListeners() {
        registerListener(new IslandActivitiesListeners(this));
        registerListener(new JoinLeaveListener(this));
        registerListener(new MigrationListener(this));
    }

    private void registerGameModeCommands() {
        registeredGameModes.clear();
        getPlugin().getAddonsManager().getGameModeAddons().stream()
                .filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName())).forEach(gm -> {
                    log("Level hooking into " + gm.getDescription().getName());
                    registerCommands(gm);
                    new PlaceholderManager(this).registerPlaceholders(gm);
                    registeredGameModes.add(gm);
                });
    }

    private void registerRequestHandlers() {
        registerRequestHandler(new LevelRequestHandler(this));
        registerRequestHandler(new TopTenRequestHandler(this));
    }

    /**
     * A helper that only executes the provided hookAction if the plugin is not disabled.
     */
    private void hookPlugin(String pluginName, Runnable hookAction) {
        if (!settings.getDisabledPluginHooks().contains(pluginName)) {
            hookAction.run();
        }
    }

    private void hookWildStackers() {
        stackersEnabled = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
        if (stackersEnabled) {
            log("Hooked into WildStackers.");
        }
    }

    private void hookAdvancedChests() {
        if (!settings.getDisabledPluginHooks().contains("AdvancedChests")) {
            Plugin advChest = Bukkit.getPluginManager().getPlugin("AdvancedChests");
            advChestEnabled = advChest != null;
            if (advChestEnabled) {
                if (compareVersions(advChest.getDescription().getVersion(), "23.0") > 0) {
                    log("Hooked into AdvancedChests.");
                } else {
                    logError("Could not hook into AdvancedChests " + advChest.getDescription().getVersion()
                            + " - requires version 23.0 or later");
                    advChestEnabled = false;
                }
            }
        }
    }

    private void hookRoseStackers() {
        roseStackersEnabled = Bukkit.getPluginManager().isPluginEnabled("RoseStacker");
        if (roseStackersEnabled) {
            log("Hooked into RoseStackers.");
        }
    }

    private void hookUltimateStacker() {
        ultimateStackerEnabled = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");
        if (ultimateStackerEnabled) {
            log("Hooked into UltimateStacker.");
        }
	}

	/**
	 * This method tries to hook into addons and plugins
	 */
	private void hookExtensions() {
		// Try to find Visit addon and if it does not exist, display a warning
		this.getAddonByName("Visit").ifPresentOrElse(addon -> {
			this.visitHook = (VisitAddon) addon;
			this.log("Level Addon hooked into Visit addon.");
		}, () -> this.visitHook = null);

		// Try to find Warps addon and if it does not exist, display a warning
		this.getAddonByName("Warps").ifPresentOrElse(addon -> {
			this.warpHook = (Warp) addon;
			this.log("Level Addon hooked into Warps addon.");
		}, () -> this.warpHook = null);
	}

	/**
	 * Compares versions
	 * 
	 * @param version1 version 1
	 * @param version2 version 2
	 * @return {@code <0 if version 1 is older than version 2, =0 if the same, >0 if version 1 is newer than version 2}
	 */
	public static int compareVersions(String version1, String version2) {
		int comparisonResult = 0;

		String[] version1Splits = version1.split("\\.");
		String[] version2Splits = version2.split("\\.");
		int maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length);

		for (int i = 0; i < maxLengthOfVersionSplits; i++) {
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

	private void registerCommands(GameModeAddon gm) {
		gm.getAdminCommand().ifPresent(adminCommand -> {
			new AdminLevelCommand(this, adminCommand);
			new AdminTopCommand(this, adminCommand);
			new AdminLevelStatusCommand(this, adminCommand);
			if (getSettings().isZeroNewIslandLevels()) {
				new AdminSetInitialLevelCommand(this, adminCommand);
			}
			new AdminStatsCommand(this, adminCommand);
		});
		gm.getPlayerCommand().ifPresent(playerCmd -> {
			new IslandLevelCommand(this, playerCmd);
			new IslandTopCommand(this, playerCmd);
			new IslandValueCommand(this, playerCmd);
            new IslandDetailCommand(this, playerCmd);
		});
	}

	@Override
	public void onDisable() {
		// Stop the pipeline
		this.getPipeliner().stop();
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
	 * 
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
	 * 
	 * @param targetPlayer - target player UUID
	 * @return Level of player or zero if player is unknown or UUID is null
	 */
	public long getIslandLevel(World world, @Nullable UUID targetPlayer) {
		return getManager().getIslandLevel(world, targetPlayer);
	}

	/**
     * Sets the player's level to a value. This will only last until the player reruns the level command
     * 
     * @param world        - world
     * @param targetPlayer - target player
     * @param level        - level
     * @deprecated This is a useless method. 
     */
	public void setIslandLevel(World world, UUID targetPlayer, long level) {
		getManager().setIslandLevel(world, targetPlayer, level);
	}

	/**
	 * Zeros the initial island level
	 * 
	 * @param island - island
	 * @param level  - initial calculated island level
	 */
    /* TODO
    public void setInitialIslandLevel(@NonNull Island island, long level) {
    	getManager().setInitialIslandLevel(island, level);
    }
    
    /**
     * Get the initial island level
     * 
     * @param island - island
     * @return level or 0 by default
     */
    /* TODO
    public long getInitialIslandLevel(@NonNull Island island) {
    	return getManager().getInitialLevel(island);
    }*/

    /**
     * Get the initial island count
     * 
     * @param island - island
     * @return count or 0 by default
     */
    public long getInitialIslandCount(@NonNull Island island) {
        return getManager().getInitialCount(island);
    }

	/**
	 * Calculates a user's island
	 * 
	 * @param world      - the world where this island is
	 * @param user       - not used! See depecration message
	 * @param playerUUID - the target island member's UUID
	 * @deprecated Do not use this anymore. Use
	 *             getManager().calculateLevel(playerUUID, island)
	 */
	@Deprecated(since = "2.3.0", forRemoval = true)
	public void calculateIslandLevel(World world, @Nullable User user, @NonNull UUID playerUUID) {
		Island island = getIslands().getIsland(world, playerUUID);
		if (island != null)
			getManager().calculateLevel(playerUUID, island);
	}

	/**
	 * Provide the levels data for the target player
	 * 
	 * @param targetPlayer - UUID of target player
	 * @return LevelsData object or null if not found. Only island levels are set!
	 * @deprecated Do not use this anymore. Use {@link #getIslandLevel(World, UUID)}
	 */
    /*
    @Deprecated(since = "2.3.0", forRemoval = true)
    public LevelsData getLevelsData(UUID targetPlayer) {
    	LevelsData ld = new LevelsData(targetPlayer);
    	getPlugin().getAddonsManager().getGameModeAddons().stream()
    			.filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName())).forEach(gm -> {
    				if (getSettings().isZeroNewIslandLevels()) {
    					Island island = getIslands().getIsland(gm.getOverWorld(), targetPlayer);
    					if (island != null) {
    						ld.setInitialLevel(gm.getOverWorld(), this.getInitialIslandLevel(island));
    					}
    				}
    				ld.setLevel(gm.getOverWorld(), this.getIslandLevel(gm.getOverWorld(), targetPlayer));
    			});
    	return ld;
    }*/

	/**
	 * @return the registeredGameModes
	 */
	public List<GameModeAddon> getRegisteredGameModes() {
		return registeredGameModes;
	}

	/**
	 * Check if Level addon is active in game mode
	 * 
	 * @param gm Game Mode Addon
	 * @return true if active, false if not
	 */
	public boolean isRegisteredGameMode(GameModeAddon gm) {
		return registeredGameModes.contains(gm);
	}

	/**
	 * Checks if Level addon is active in world
	 * 
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

	/**
	 * @return the ultimateStackerEnabled
	 */
	public boolean isUltimateStackerEnabled() {
		return ultimateStackerEnabled;
	}

	/**
	 * Method Level#getVisitHook returns the visitHook of this object.
	 *
	 * @return {@code Visit} of this object, {@code null} otherwise.
	 */
	public VisitAddon getVisitHook() {
		return this.visitHook;
	}

	/**
	 * Method Level#getWarpHook returns the warpHook of this object.
	 *
	 * @return {@code Warp} of this object, {@code null} otherwise.
	 */
	public Warp getWarpHook() {
		return this.warpHook;
	}

    public boolean isItemsAdder() {
        return !getSettings().isDisableItemsAdder() && getPlugin().getHooks().getHook("ItemsAdder").isPresent();
    }

}
