package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.listeners.IslandActivitiesListeners;
import world.bentobox.level.listeners.JoinLeaveListener;

/**
 * @author tastybento
 *
 */
public class LevelTest extends CommonTestSetup {

	private static File jFile;
	@Mock
	private User user;
	@Mock
	private FlagsManager fm;
	@Mock
	private GameModeAddon gameMode;
	@Mock
	private AddonsManager am;

	@Mock
	private Settings pluginSettings;

	private Level addon;

	@Mock
	private Logger logger;
	@Mock
	private PlaceholdersManager phm;
	@Mock
	private CompositeCommand cmd;
	@Mock
	private CompositeCommand adminCmd;

	@Mock
	private BlockConfig blockConfig;
    @Mock
    private HooksManager hm;
    private MockedStatic<ItemsAdderHook> itemsAdderMock;

	@BeforeAll
	public static void beforeClass() throws IOException {
		// Make the addon jar
		jFile = new File("addon.jar");
		// Copy over config file from src folder
		Path fromPath = Paths.get("src/main/resources/config.yml");
		Path path = Paths.get("config.yml");
		Files.copy(fromPath, path, StandardCopyOption.REPLACE_EXISTING);
		// Copy over block config file from src folder
		fromPath = Paths.get("src/main/resources/blockconfig.yml");
		path = Paths.get("blockconfig.yml");
		Files.copy(fromPath, path, StandardCopyOption.REPLACE_EXISTING);
		try (JarOutputStream tempJarOutputStream = new JarOutputStream(new FileOutputStream(jFile))) {
			// Added the new files to the jar.
			try (FileInputStream fis = new FileInputStream(path.toFile())) {
				byte[] buffer = new byte[1024];
				int bytesRead = 0;
				JarEntry entry = new JarEntry(path.toString());
				tempJarOutputStream.putNextEntry(entry);
				while ((bytesRead = fis.read(buffer)) != -1) {
					tempJarOutputStream.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Override
    @BeforeEach
	public void setUp() throws Exception {
        super.setUp();
        when(plugin.getHooks()).thenReturn(hm);

		// The database type has to be created one line before the thenReturn() to work!
		DatabaseType value = DatabaseType.JSON;
		when(plugin.getSettings()).thenReturn(pluginSettings);
		when(pluginSettings.getDatabaseType()).thenReturn(value);

        // ItemsAdderHook
        itemsAdderMock = Mockito.mockStatic(ItemsAdderHook.class, Mockito.RETURNS_MOCKS);
        itemsAdderMock.when(() -> ItemsAdderHook.isInRegistry(anyString())).thenReturn(true);
		// Command manager
		CommandsManager cm = mock(CommandsManager.class);
		when(plugin.getCommandsManager()).thenReturn(cm);

		// Player
		// Sometimes use Mockito.withSettings().verboseLogging()
		when(user.isOp()).thenReturn(false);
		uuid = UUID.randomUUID();
		when(user.getUniqueId()).thenReturn(uuid);
		when(user.getPlayer()).thenReturn(p);
		when(user.getName()).thenReturn("tastybento");
		User.setPlugin(plugin);

		// Player has island to begin with
		when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

		// Locales
		// Return the reference (USE THIS IN THE FUTURE)
		when(user.getTranslation(Mockito.anyString()))
				.thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));


        // Util
        mockedUtil.when(() -> Util.inTest()).thenReturn(true);

		// Addon
		addon = new Level();
		File dataFolder = new File("addons/Level");
		addon.setDataFolder(dataFolder);
		addon.setFile(jFile);
		AddonDescription desc = new AddonDescription.Builder("bentobox", "Level", "1.3").description("test")
				.authors("tastybento").build();
		addon.setDescription(desc);
		addon.setSettings(new ConfigSettings());
		// Addons manager
		when(plugin.getAddonsManager()).thenReturn(am);
		// One game mode
		when(am.getGameModeAddons()).thenReturn(Collections.singletonList(gameMode));
		AddonDescription desc2 = new AddonDescription.Builder("bentobox", "BSkyBlock", "1.3").description("test")
				.authors("tasty").build();
		when(gameMode.getDescription()).thenReturn(desc2);
		when(gameMode.getOverWorld()).thenReturn(world);

		// Player command
		@NonNull
		Optional<CompositeCommand> opCmd = Optional.of(cmd);
		when(gameMode.getPlayerCommand()).thenReturn(opCmd);
		// Admin command
		Optional<CompositeCommand> opAdminCmd = Optional.of(adminCmd);
		when(gameMode.getAdminCommand()).thenReturn(opAdminCmd);

		// Flags manager
		when(plugin.getFlagsManager()).thenReturn(fm);
		when(fm.getFlags()).thenReturn(Collections.emptyList());

		// placeholders
		when(plugin.getPlaceholdersManager()).thenReturn(phm);

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Override
	@AfterEach
	public void tearDown() throws Exception {
        super.tearDown();
		deleteAll(new File("database"));
	}

	@AfterAll
	public static void cleanUp() throws Exception {
		new File("addon.jar").delete();
		new File("config.yml").delete();
		new File("blockconfig.yml").delete();
		deleteAll(new File("addons"));
	}

	/**
     * Test method for {@link world.bentobox.level.Level#allLoaded()
     */
	@Test
    public void testAllLoaded() {
	    mockedBukkit.when(() -> Bukkit.getWorld("acidisland_world")).thenReturn(null);
        addon.allLoaded();
		verify(plugin).log("[Level] Level hooking into BSkyBlock");
        verify(cmd, times(4)).getAddon(); // 4 commands
		verify(adminCmd, times(5)).getAddon(); // Five commands
		// Placeholders
		verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_island_level"), any());
		verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_visited_island_level"), any());
		verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_points_to_next_level"), any());
		for (int i = 1; i < 11; i++) {
			verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_top_name_" + i), any());
			verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_top_value_" + i), any());
		}
		// Commands
		verify(am).registerListener(eq(addon), any(IslandActivitiesListeners.class));
		verify(am).registerListener(eq(addon), any(JoinLeaveListener.class));

        verify(plugin).log("[Level] Level Addon: No such world in blockconfig.yml : acidisland_world");

	}

	/**
	 * Test method for {@link world.bentobox.level.Level#getSettings()}.
	 */
	@Test
	public void testGetSettings() {
		addon.onEnable();
		ConfigSettings s = addon.getSettings();
		assertEquals(100, s.getLevelCost());
	}

}
