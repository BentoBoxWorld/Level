package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.objects.LevelsData;
import world.bentobox.level.objects.TopTenData;

/**
 * @author tastybento
 *
 */
public class LevelsManagerTest extends CommonTestSetup {

    @Mock
    private AbstractDatabaseHandler<IslandLevels> handler;
    @Mock
    private AbstractDatabaseHandler<LevelsData> levelsDataHandler;
    @Mock
    private AbstractDatabaseHandler<TopTenData> topTenHandler;

    // Class under test
    private LevelsManager lm;
    @Mock
    private Pipeliner pipeliner;
    private CompletableFuture<Results> cf;

    private ConfigSettings settings;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Inventory inv;
    @Mock
    private IslandLevels levelsData;
    
    protected Object savedObject;
    private MockedStatic<DatabaseSetup> mockedDatabaseSetup;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Clear any lingering database
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
        // Database
        handler = mock(AbstractDatabaseHandler.class);
        levelsDataHandler = mock(AbstractDatabaseHandler.class);
        topTenHandler = mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(() -> DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(IslandLevels.class))).thenReturn(handler);
        when(dbSetup.getHandler(eq(LevelsData.class))).thenReturn(levelsDataHandler);
        when(dbSetup.getHandler(eq(TopTenData.class))).thenReturn(topTenHandler);

        this.databaseSetup(handler);
        this.databaseSetup(levelsDataHandler);
        this.databaseSetup(topTenHandler);
        savedObject = null;
        
        
        // Pipeliner
        when(addon.getPipeliner()).thenReturn(pipeliner);
        cf = new CompletableFuture<>();
        when(pipeliner.addIsland(any())).thenReturn(cf);

        // Player
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.hasPermission(anyString())).thenReturn(true);

        // Settings
        settings = new ConfigSettings();
        when(addon.getSettings()).thenReturn(settings);

        // User
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(eq("island.top.gui-heading"), eq("[name]"), anyString(), eq("[rank]"), anyString())).thenReturn("gui-heading");
        when(user.getTranslation(eq("island.top.island-level"),eq("[level]"), anyString())).thenReturn("island-level");
        when(user.getPlayer()).thenReturn(p);

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

        // Has perms
        when(p.hasPermission(anyString())).thenReturn(true);
        // Make island levels

        List<IslandLevels> islands = new ArrayList<>();
        for (long i = -5; i < 5; i ++) {
            IslandLevels il = new IslandLevels(UUID.randomUUID().toString());
            il.setInitialCount(null);
            il.setLevel(i);
            il.setPointsToNextLevel(3);
            il.setInitialLevel(26145L); // Legacy
            islands.add(il);
        }
        // Supply no island levels first (for migrate), then islands
        when(handler.loadObjects()).thenReturn(islands);
        when(handler.objectExists(anyString())).thenReturn(true);
        when(levelsData.getLevel()).thenReturn(-5L, -4L, -3L, -2L, -1L, 0L, 1L, 2L, 3L, 4L, 5L, 45678L);
        when(levelsData.getInitialLevel()).thenReturn(26145L, -4L, -3L, -2L, -1L, 0L, 1L, 2L, 3L, 4L, 5L, 45678L);
        when(levelsData.getInitialCount()).thenReturn(null);
        when(levelsData.getUniqueId()).thenReturn(uuid.toString());
        when(handler.loadObject(anyString())).thenReturn(levelsData );
        System.out.println("Hanlder = " + handler);

        // Island Manager
        when(island.getOwner()).thenReturn(uuid);
        when(island.getUniqueId()).thenReturn(uuid.toString());
        when(im.getIsland(world, uuid)).thenReturn(island);

        // Inventory GUI
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);

        lm = new LevelsManager(addon);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        handler.close();
        this.levelsDataHandler.close();
        this.topTenHandler.close();
    }
   
    @SuppressWarnings("unchecked")
    private void databaseSetup(AbstractDatabaseHandler h) throws Exception {
        // Save objects
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));        
        // Capture the parameter passed to saveObject() and store it in savedObject
        doAnswer(invocation -> {
            savedObject = invocation.getArgument(0);
            return CompletableFuture.completedFuture(true);
        }).when(h).saveObject(any());
 
        // Now when loadObject() is called, return the savedObject
        when(h.loadObject(any())).thenAnswer(invocation -> savedObject);
        
        // Delete object
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(h).deleteObject(any());
         
        doAnswer(invocation -> {
            savedObject = null;
            return null;
        }).when(h).deleteID(anyString());
     }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#calculateLevel(UUID, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testCalculateLevel() {
        Results results = new Results();
        results.setLevel(10000);
        results.setInitialCount(300L);
        lm.calculateLevel(uuid, island);
        // Complete the pipelined completable future
        cf.complete(results);

        assertEquals(10000L, lm.getLevelsData(island).getLevel());
        // Map<UUID, Long> tt = lm.getTopTen(world, 10);
        // assertEquals(1, tt.size());
        // assertTrue(tt.get(uuid) == 10000);
        assertEquals(10000L, lm.getIslandMaxLevel(world, uuid));

        results.setLevel(5000);
        lm.calculateLevel(uuid, island);
        // Complete the pipelined completable future
        cf.complete(results);
        assertEquals(5000L, lm.getLevelsData(island).getLevel());
        // Still should be 10000
        assertEquals(10000L, lm.getIslandMaxLevel(world, uuid));

    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getInitialCount(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testGetInitialCount() {
        assertEquals(2614500L, lm.getInitialCount(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getIslandLevel(org.bukkit.World, java.util.UUID)}.
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testGetIslandLevel() {
        assertEquals(-5, lm.getIslandLevel(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getPointsToNextString(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetPointsToNextString() {
        // No island player
        assertEquals("", lm.getPointsToNextString(world, UUID.randomUUID()));
        // Player has island
        assertEquals("0", lm.getPointsToNextString(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getIslandLevelString(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandLevelString() {
        assertEquals("-5", lm.getIslandLevelString(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getLevelsData(java.util.UUID)}.
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
        settings.setShorthand(true);
        assertEquals("123.5M", lm.formatLevel(123456789L));
        assertEquals("1.2k", lm.formatLevel(1234L));
        assertEquals("123.5G", lm.formatLevel(123456789352L));
        assertEquals("1.2T", lm.formatLevel(1234567893524L));
        assertEquals("12345.7T", lm.formatLevel(12345678345345349L));

    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetTopTenEmpty() {
        Map<String, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.isEmpty());
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetTopTen() {
        testLoadTopTens();
        Map<String, Long> tt = lm.getTopTen(world, Level.TEN);
        assertFalse(tt.isEmpty());
        assertEquals(1, tt.size());
        assertEquals(1, lm.getTopTen(world, 1).size());
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getWeightedTopTen(org.bukkit.World, int)}.
     */
    @Test
    public void testGetWeightedTopTen() {
        testLoadTopTens();
        Map<Island, Long> tt = lm.getWeightedTopTen(world, Level.TEN);
        assertFalse(tt.isEmpty());
        assertEquals(1, tt.size());
        assertEquals(1, lm.getTopTen(world, 1).size());
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#hasTopTenPerm(org.bukkit.World, java.util.UUID)}.
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
        verify(sch).runTaskAsynchronously(eq(plugin), task.capture()); // Capture the task in the scheduler
        task.getValue().run(); // run it
        verify(addon).log("Generating rankings");
        verify(addon).log("Generated rankings for bskyblock-world");

    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#removeEntry(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemoveEntry() {
        testLoadTopTens();
        Map<String, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.containsKey(uuid.toString()));
        lm.removeEntry(world, uuid.toString());
        tt = lm.getTopTen(world, Level.TEN);
        assertFalse(tt.containsKey(uuid.toString()));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#setInitialIslandLevel(world.bentobox.bentobox.database.objects.Island, long)}.
     */
    @Test
    public void testSetInitialIslandLevel() {
        lm.setInitialIslandCount(island, Level.TEN);
        assertEquals(Level.TEN, lm.getInitialCount(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#setIslandLevel(org.bukkit.World, java.util.UUID, long)}.
     */
    @Test
    public void testSetIslandLevel() {
        lm.setIslandLevel(world, uuid, 1234);
        assertEquals(1234, lm.getIslandLevel(world, uuid));

    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getRank(World, UUID)}
     */
    @Test
    public void testGetRank() {
        lm.createAndCleanRankings(world);
        Map<World, TopTenData> ttl = lm.getTopTenLists();
        Map<String, Long> tt = ttl.get(world).getTopTen();
        for (long i = 100; i < 150; i++) {
            tt.put(UUID.randomUUID().toString(), i);
        }
        // Put island as lowest rank
        tt.put(uuid.toString(), 10L);
        assertEquals(51, lm.getRank(world, uuid));
        // Put island as highest rank
        tt.put(uuid.toString(), 1000L);
        assertEquals(1, lm.getRank(world, uuid));
        // Unknown UUID - lowest rank + 1
        assertEquals(52, lm.getRank(world, UUID.randomUUID()));
    }

}
