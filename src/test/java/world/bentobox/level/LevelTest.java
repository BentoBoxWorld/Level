package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.level.listeners.IslandTeamListeners;
import world.bentobox.level.listeners.JoinLeaveListener;

/**
 * @author tastybento
 *
 */
@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class})
public class LevelTest {

    private static File jFile;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private BentoBox plugin;
    @Mock
    private FlagsManager fm;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private AddonsManager am;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private Settings settings;

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
    private World world;
    private UUID uuid;

    @BeforeClass
    public static void beforeClass() throws IOException {
        jFile = new File("addon.jar");
        // Copy over config file from src folder
        Path fromPath = Paths.get("src/main/resources/config.yml");
        Path path = Paths.get("config.yml");
        Files.copy(fromPath, path);
        try (JarOutputStream tempJarOutputStream = new JarOutputStream(new FileOutputStream(jFile))) {
            //Added the new files to the jar.
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                JarEntry entry = new JarEntry(path.toString());
                tempJarOutputStream.putNextEntry(entry);
                while((bytesRead = fis.read(buffer)) != -1) {
                    tempJarOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        //when(plugin.isEnabled()).thenReturn(true);
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Locales
        // Return the reference (USE THIS IN THE FUTURE)
        when(user.getTranslation(Mockito.anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Server
        PowerMockito.mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(Bukkit.getPluginManager()).thenReturn(mock(PluginManager.class));

        // Addon
        addon = new Level();
        File dataFolder = new File("addons/Level");
        addon.setDataFolder(dataFolder);
        addon.setFile(jFile);
        AddonDescription desc = new AddonDescription.Builder("bentobox", "Level", "1.3").description("test").authors("tastybento").build();
        addon.setDescription(desc);
        // Addons manager
        when(plugin.getAddonsManager()).thenReturn(am);
        // One game mode
        when(am.getGameModeAddons()).thenReturn(Collections.singletonList(gameMode));
        AddonDescription desc2 = new AddonDescription.Builder("bentobox", "BSkyBlock", "1.3").description("test").authors("tasty").build();
        when(gameMode.getDescription()).thenReturn(desc2);

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

        // The database type has to be created one line before the thenReturn() to work!
        when(plugin.getSettings()).thenReturn(settings);
        DatabaseType value = DatabaseType.JSON;
        when(settings.getDatabaseType()).thenReturn(value);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        ItemMeta meta = mock(ItemMeta.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.getDataVersion()).thenReturn(777);
        when(Bukkit.getUnsafe()).thenReturn(unsafe);

        // placeholders
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // World
        when(world.getName()).thenReturn("bskyblock-world");
        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.getOwner()).thenReturn(uuid);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        deleteAll(new File("database"));
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        new File("addon.jar").delete();
        new File("config.yml").delete();
        deleteAll(new File("addons"));
    }

    private static void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }

    /**
     * Test method for {@link world.bentobox.level.Level#onEnable()}.
     */
    @Test
    public void testOnEnable() {
        addon.onEnable();
        verify(plugin).logWarning("[Level] Level Addon: No such world in config.yml : acidisland_world");
        verify(plugin).log("[Level] Level hooking into BSkyBlock");
        verify(cmd, times(3)).getAddon(); // Three commands
        verify(adminCmd, times(2)).getAddon(); // Two commands
        // Placeholders
        verify(phm).registerPlaceholder(eq(addon), eq("bskyblock-island-level"), any());
        for (int i = 1; i < 11; i++) {
            verify(phm).registerPlaceholder(eq(addon), eq("bskyblock-island-level-top-name-" + i), any());
            verify(phm).registerPlaceholder(eq(addon), eq("bskyblock-island-level-top-value-" + i), any());
        }
        verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_island_level"), any());
        verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_visited_island_level"), any());
        for (int i = 1; i < 11; i++) {
            verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_top_name_" + i), any());
            verify(phm).registerPlaceholder(eq(addon), eq("bskyblock_top_value_" + i), any());
        }
        // Commands
        verify(am).registerListener(eq(addon), any(IslandTeamListeners.class));
        verify(am).registerListener(eq(addon), any(JoinLeaveListener.class));
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getIslandLevel(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandLevelUnknown() {
        addon.onEnable();
        assertEquals(0L, addon.getIslandLevel(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getIslandLevel(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandLevelNullTarget() {
        addon.onEnable();
        assertEquals(0L, addon.getIslandLevel(world, UUID.randomUUID()));

    }

    /**
     * Test method for {@link world.bentobox.level.Level#getLevelsData(java.util.UUID)}.
     */
    @Test
    public void testGetLevelsDataUnknown() {
        addon.onEnable();
        assertNull(addon.getLevelsData(UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getSettings()}.
     */
    @Test
    public void testGetSettings() {
        addon.onEnable();
        world.bentobox.level.config.Settings s = addon.getSettings();
        assertEquals(100, s.getDeathPenalty());
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getTopTen()}.
     */
    @Test
    public void testGetTopTen() {
        addon.onEnable();
        assertNotNull(addon.getTopTen());
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getLevelPresenter()}.
     */
    @Test
    public void testGetLevelPresenter() {
        addon.onEnable();
        assertNotNull(addon.getLevelPresenter());
    }

    /**
     * Test method for {@link world.bentobox.level.Level#setIslandLevel(org.bukkit.World, java.util.UUID, long)}.
     */
    @Test
    public void testSetIslandLevel() {
        addon.onEnable();
        addon.setIslandLevel(world, uuid, 345L);
        assertEquals(345L, addon.getIslandLevel(world, uuid));
        verify(plugin, never()).logError(anyString());
    }

    /**
     * Test method for {@link world.bentobox.level.Level#getInitialIslandLevel(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testGetInitialIslandLevel() {
        addon.onEnable();
        addon.setInitialIslandLevel(island, 40);
        verify(plugin, never()).logError(anyString());
        assertEquals(40, addon.getInitialIslandLevel(island));

    }

    /**
     * Test method for {@link world.bentobox.level.Level#getHandler()}.
     */
    @Test
    public void testGetHandler() {
        addon.onEnable();
        assertNotNull(addon.getHandler());
    }


}
