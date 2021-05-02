package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.objects.TopTenData;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, DatabaseSetup.class, PanelBuilder.class})
public class LevelsManagerTest {

    @Mock
    private static AbstractDatabaseHandler<Object> handler;
    @Mock
    Level addon;
    @Mock
    private BentoBox plugin;
    @Mock
    private Settings pluginSettings;


    // Class under test
    private LevelsManager lm;
    @Mock
    private Island island;
    @Mock
    private Pipeliner pipeliner;
    private CompletableFuture<Results> cf;
    private UUID uuid;
    @Mock
    private World world;
    @Mock
    private Player player;
    @Mock
    private ConfigSettings settings;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Inventory inv;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private PluginManager pim;
    @Mock
    private IslandLevels levelsData;
    @Mock
    private IslandsManager im;
    @Mock
    private BukkitScheduler scheduler;



    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
    }

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(addon.getPlugin()).thenReturn(plugin);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getWorld(anyString())).thenReturn(world);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // The database type has to be created one line before the thenReturn() to work!
        DatabaseType value = DatabaseType.JSON;
        when(plugin.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.getDatabaseType()).thenReturn(value);

        // Pipeliner
        when(addon.getPipeliner()).thenReturn(pipeliner);
        cf = new CompletableFuture<>();
        when(pipeliner.addIsland(any())).thenReturn(cf);

        // Island
        when(addon.getIslands()).thenReturn(im);
        uuid = UUID.randomUUID();
        ImmutableSet<UUID> iset = ImmutableSet.of(uuid);
        when(island.getMemberSet()).thenReturn(iset);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getUniqueId()).thenReturn(UUID.randomUUID().toString());
        // Default to uuid's being island owners
        when(im.isOwner(eq(world), any())).thenReturn(true);
        when(im.getOwner(any(), any(UUID.class))).thenAnswer(in -> in.getArgument(1, UUID.class));
        when(im.getIsland(eq(world), eq(uuid))).thenReturn(island);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(true);

        // World
        when(world.getName()).thenReturn("bskyblock-world");

        // Settings
        when(addon.getSettings()).thenReturn(settings);

        // User
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(eq("island.top.gui-heading"), eq("[name]"), anyString(), eq("[rank]"), anyString())).thenReturn("gui-heading");
        when(user.getTranslation(eq("island.top.island-level"),eq("[level]"), anyString())).thenReturn("island-level");
        when(user.getPlayer()).thenReturn(player);

        // Player Manager
        when(addon.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("player1",
                "player2",
                "player3",
                "player4",
                "player5",
                "player6",
                "player7",
                "player8",
                "player9",
                "player10"
                );
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

        // Has perms
        when(player.hasPermission(anyString())).thenReturn(true);
        // Make island levels

        List<Object> islands = new ArrayList<>();
        for (long i = -5; i < 5; i ++) {
            IslandLevels il = new IslandLevels(UUID.randomUUID().toString());
            il.setInitialLevel(3);
            il.setLevel(i);
            il.setPointsToNextLevel(3);
            islands.add(il);
        }
        // Supply no island levels first (for migrate), then islands
        when(handler.loadObjects()).thenReturn(Collections.emptyList(), islands);
        when(handler.objectExists(anyString())).thenReturn(true);
        when(levelsData.getLevel()).thenReturn(-5L, -4L, -3L, -2L, -1L, 0L, 1L, 2L, 3L, 4L, 5L, 45678L);
        when(levelsData.getUniqueId()).thenReturn(uuid.toString());
        when(handler.loadObject(anyString())).thenReturn(levelsData );


        // Inventory GUI
        when(Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        lm = new LevelsManager(addon);
        lm.migrate();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        deleteAll(new File("database"));
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
     * Test method for {@link world.bentobox.level.LevelsManager#calculateLevel(UUID, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testCalculateLevel() {
        Results results = new Results();
        results.setLevel(10000);
        results.setInitialLevel(3);
        lm.calculateLevel(uuid, island);
        // Complete the pipelined completable future
        cf.complete(results);

        assertEquals(10000L, lm.getLevelsData(island).getLevel());
        //Map<UUID, Long> tt = lm.getTopTen(world, 10);
        //assertEquals(1, tt.size());
        //assertTrue(tt.get(uuid) == 10000);

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getInitialLevel(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testGetInitialLevel() {
        assertEquals(0,lm.getInitialLevel(island));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getIslandLevel(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandLevel() {
        assertEquals(-5, lm.getIslandLevel(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getPointsToNextString(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetPointsToNextString() {
        // No island player
        assertEquals("", lm.getPointsToNextString(world, UUID.randomUUID()));
        // Player has island
        assertEquals("0", lm.getPointsToNextString(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getIslandLevelString(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandLevelString() {
        assertEquals("-5", lm.getIslandLevelString(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getLevelsData(java.util.UUID)}.
     */
    @Test
    public void testGetLevelsData() {
        assertEquals(levelsData, lm.getLevelsData(island));

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#formatLevel(long)}.
     */
    @Test
    public void testFormatLevel() {
        assertEquals("123456789", lm.formatLevel(123456789L));
        when(settings.isShorthand()).thenReturn(true);
        assertEquals("123.5M", lm.formatLevel(123456789L));
        assertEquals("1.2k", lm.formatLevel(1234L));
        assertEquals("123.5G", lm.formatLevel(123456789352L));
        assertEquals("1.2T", lm.formatLevel(1234567893524L));
        assertEquals("12345.7T", lm.formatLevel(12345678345345349L));

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetTopTenEmpty() {
        Map<UUID, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetTopTen() {
        testLoadTopTens();
        Map<UUID, Long> tt = lm.getTopTen(world, Level.TEN);
        assertFalse(tt.isEmpty());
        assertEquals(1, tt.size());
        assertEquals(1, lm.getTopTen(world, 1).size());
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetTopTenNoOwners() {
        when(im.isOwner(eq(world), any())).thenReturn(false);
        testLoadTopTens();
        Map<UUID, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#hasTopTenPerm(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testHasTopTenPerm() {
        assertTrue(lm.hasTopTenPerm(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#loadTopTens()}.
     */
    @Test
    public void testLoadTopTens() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        lm.loadTopTens();
        PowerMockito.verifyStatic(Bukkit.class); // 1
        Bukkit.getScheduler();
        verify(scheduler).runTaskAsynchronously(eq(plugin), task.capture());
        task.getValue().run();
        verify(addon).log(eq("Generating Top Ten Tables"));
        verify(addon).log(eq("Loaded top ten for bskyblock-world"));

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#removeEntry(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveEntry() {
        testLoadTopTens();
        Map<UUID, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.containsKey(uuid));
        lm.removeEntry(world, uuid);
        tt = lm.getTopTen(world, Level.TEN);
        assertFalse(tt.containsKey(uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#save()}.
     */
    @Test
    public void testSave() {
        lm.save();
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#setInitialIslandLevel(world.bentobox.bentobox.database.objects.Island, long)}.
     */
    @Test
    public void testSetInitialIslandLevel() {
        lm.setInitialIslandLevel(island, Level.TEN);
        assertEquals(Level.TEN, lm.getInitialLevel(island));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#setIslandLevel(org.bukkit.World, java.util.UUID, long)}.
     */
    @Test
    public void testSetIslandLevel() {
        lm.setIslandLevel(world, uuid, 1234);
        assertEquals(1234, lm.getIslandLevel(world, uuid));

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getGUI(org.bukkit.World, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetGUI() {
        lm.getGUI(world, user);
        verify(user).getTranslation(eq("island.top.gui-title"));
        verify(player).openInventory(inv);
        /*
        int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
        for (int i : SLOTS) {
            verify(inv).setItem(eq(i), any());
        }
         */
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#getRank(World, UUID)}
     */
    @Test
    public void testGetRank() {
        lm.createAndCleanRankings(world);
        Map<World, TopTenData> ttl = lm.getTopTenLists();
        Map<UUID, Long> tt = ttl.get(world).getTopTen();
        for (long i = 100; i < 150; i++) {
            tt.put(UUID.randomUUID(), i);
        }
        // Put player as lowest rank
        tt.put(uuid, 10L);
        assertEquals(51, lm.getRank(world, uuid));
        // Put player as highest rank
        tt.put(uuid, 1000L);
        assertEquals(1, lm.getRank(world, uuid));
        // Unknown UUID - lowest rank + 1
        assertEquals(52, lm.getRank(world, UUID.randomUUID()));
    }

}
