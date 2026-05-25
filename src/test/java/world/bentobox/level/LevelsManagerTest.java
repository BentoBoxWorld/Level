package world.bentobox.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
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
class LevelsManagerTest extends CommonTestSetup {

    @Mock
    private AbstractDatabaseHandler<Object> handler;
    @Mock
    private Settings pluginSettings;

    // Class under test
    private LevelsManager lm;
    @Mock
    private Pipeliner pipeliner;
    private CompletableFuture<Results> cf;
    @Mock
    private Player player;

    private ConfigSettings settings;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Inventory inv;
    @Mock
    private IslandLevels levelsData;
    //@Mock
    //private BukkitScheduler scheduler;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        MockedStatic<DatabaseSetup> mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(() -> DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
        when(addon.getPlugin()).thenReturn(plugin);

        // The database type has to be created one line before the thenReturn() to work!
        DatabaseType value = DatabaseType.JSON;
        when(plugin.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.getDatabaseType()).thenReturn(value);

        // Pipeliner
        when(addon.getPipeliner()).thenReturn(pipeliner);
        cf = new CompletableFuture<>();
        when(pipeliner.addIsland(any())).thenReturn(cf);

        // Island
        ImmutableSet<UUID> iset = ImmutableSet.of(uuid);
        when(island.getMemberSet()).thenReturn(iset);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getUniqueId()).thenReturn(uuid.toString());
        // Default to uuid's being island owners
        when(im.hasIsland(eq(world), any(UUID.class))).thenReturn(true);
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));
        when(im.getIslandById(anyString(), eq(false))).thenReturn(Optional.of(island));

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(true);

        // World
        when(world.getName()).thenReturn("bskyblock-world");

        // Settings
        settings = new ConfigSettings();
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
        // Has perms
        when(player.hasPermission(anyString())).thenReturn(true);
        // Make island levels

        List<Object> islands = new ArrayList<>();
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


        // Inventory GUI
        mockedBukkit.when(() -> Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);

        // No online players — hasTopTenPerm short-circuits to true via the null check.
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

        // IWM
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        lm = new LevelsManager(addon);

    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteAll(new File("database"));
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#calculateLevel(UUID, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    void testCalculateLevel() {
        Results results = new Results();
        results.setLevel(10000);
        results.setInitialCount(300L);
        lm.calculateLevel(uuid, island);
        // Complete the pipelined completable future
        cf.complete(results);

        assertEquals(10000L, lm.getLevelsData(island).getLevel());
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
    void testGetInitialCount() {
        assertEquals(2614500L, lm.getInitialCount(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getIslandLevel(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    void testGetIslandLevel() {
        assertEquals(-5, lm.getIslandLevel(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getPointsToNextString(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    void testGetPointsToNextString() {
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
    void testGetIslandLevelString() {
        assertEquals("-5", lm.getIslandLevelString(world, uuid));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getLevelsData(java.util.UUID)}.
     */
    @Test
    void testGetLevelsData() {
        assertEquals(levelsData, lm.getLevelsData(island));

    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#formatLevel(long)}.
     */
    @Test
    void testFormatLevel() {
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
    void testGetTopTenEmpty() {
        Map<String, Long> tt = lm.getTopTen(world, Level.TEN);
        assertTrue(tt.isEmpty());
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     */
    @Test
    void testGetTopTen() {
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
    void testGetWeightedTopTen() {
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
    void testHasTopTenPerm() {
        assertTrue(lm.hasTopTenPerm(world, uuid));
    }

    /**
     * Test method for {@link world.bentobox.level.LevelsManager#loadTopTens()}.
     */
    @Test
    void testLoadTopTens() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        lm.loadTopTens();
        mockedBukkit.verify(() -> Bukkit.getScheduler());
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
    void testRemoveEntry() {
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
    void testSetInitialIslandLevel() {
        lm.setInitialIslandCount(island, Level.TEN);
        assertEquals(Level.TEN, lm.getInitialCount(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#setIslandLevel(org.bukkit.World, java.util.UUID, long)}.
     */
    @Test
    void testSetIslandLevel() {
        lm.setIslandLevel(world, uuid, 1234);
        assertEquals(1234, lm.getIslandLevel(world, uuid));

    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getTopTen(org.bukkit.World, int)}.
     * Verifies that the top ten is sorted in descending order by level.
     */
    @Test
    void testGetTopTenSortOrder() {
        lm.createAndCleanRankings(world);
        Map<World, TopTenData> ttl = lm.getTopTenLists();
        Map<String, Long> tt = ttl.get(world).getTopTen();
        // Add islands in non-sorted order, mimicking the reported issue
        String island65 = UUID.randomUUID().toString();
        String island1065 = UUID.randomUUID().toString();
        String island500 = UUID.randomUUID().toString();
        String island200 = UUID.randomUUID().toString();
        // Insert in arbitrary order
        tt.put(island65, 65L);
        tt.put(island1065, 1065L);
        tt.put(island500, 500L);
        tt.put(island200, 200L);
        when(im.getIslandById(island65)).thenReturn(Optional.of(island));
        when(im.getIslandById(island1065)).thenReturn(Optional.of(island));
        when(im.getIslandById(island500)).thenReturn(Optional.of(island));
        when(im.getIslandById(island200)).thenReturn(Optional.of(island));

        Map<String, Long> topTen = lm.getTopTen(world, Level.TEN);
        // Verify descending order
        long previousLevel = Long.MAX_VALUE;
        for (Long level : topTen.values()) {
            assertTrue(level <= previousLevel,
                    "Top ten not in descending order: " + level + " should be <= " + previousLevel);
            previousLevel = level;
        }
        // Verify highest is first
        assertEquals(1065L, topTen.values().iterator().next().longValue());
    }

    /**
     * Test method for the zero-scan deferred-credit drain. Pins down the
     * contract that the chunk listener relies on: while a zero scan is
     * active, listener credits are routed into the deferred map; at drain
     * time, chunks the scan visited are dropped (already in totalPoints)
     * and chunks the scan missed are summed and returned for the caller
     * to add to the initial count.
     */
    @Test
    void testZeroScanDeferralAndDrain() {
        String w = "bskyblock-world";
        // Before beginZeroScan: tryDefer returns false (no active scan), so
        // the listener takes the normal addHandicapChunk path.
        assertFalse(lm.tryDeferZeroScanCredit(island, w, 0, 0, 99L));

        lm.beginZeroScan(island);

        // While scan is active: tryDefer returns true, capturing the value.
        assertTrue(lm.tryDeferZeroScanCredit(island, w, 1, 1, 50L));
        assertTrue(lm.tryDeferZeroScanCredit(island, w, 2, 2, 30L));
        assertTrue(lm.tryDeferZeroScanCredit(island, w, 3, 3, 20L));

        // Scan visits chunk (1,1) and (3,3). The drain should drop those
        // and only return (2,2) which the scan missed.
        lm.recordScanVisitedChunk(island, w, 1, 1);
        lm.recordScanVisitedChunk(island, w, 3, 3);

        Map<String, Long> missed = lm.drainZeroScanDeferred(island);
        assertEquals(1, missed.size());
        assertEquals(30L, missed.values().iterator().next().longValue());

        // After drain: scan state is cleared, tryDefer falls back to false.
        assertFalse(lm.tryDeferZeroScanCredit(island, w, 4, 4, 100L));
        // Drain on a non-active scan returns an empty map.
        assertTrue(lm.drainZeroScanDeferred(island).isEmpty());
    }

    /**
     * Pin down the self-healing handicap contract:
     * <ul>
     * <li>A zero-scan reconciliation overwrites the persisted per-chunk map
     * with the scanned values and does not touch initialCount (caller does
     * the hard reset separately via setInitialIslandCount).</li>
     * <li>A regular scan on an island whose persisted map is empty but whose
     * initialCount &gt; 0 is treated as legacy migration: seed the map from
     * the scan, leave initialCount alone (no double-credit).</li>
     * <li>A regular scan on an already-tracked island adds <em>only</em> the
     * chunks the persisted map didn't already know about into both the map
     * and initialCount, and freezes existing entries even if the scan saw a
     * different value (player builds keep showing as level progress).</li>
     * </ul>
     */
    @Test
    void testReconcileHandicapChunksContract() throws Exception {
        // First call ever — empty map, zero initialCount, regular scan: every
        // scanned chunk is "new" and gets credited. This is the normal fresh-
        // island path when zero scan hasn't happened (defensive — exercises
        // the "no legacy migration" branch).
        IslandLevels fresh = new IslandLevels(uuid.toString());
        when(handler.loadObject(uuid.toString())).thenReturn(fresh);

        Map<String, Long> firstScan = Map.of("world:1", 100L, "world:2", 50L);
        long delta = lm.reconcileHandicapChunks(island, firstScan, false);
        assertEquals(150L, delta, "every chunk is new on a fresh island");
        assertEquals(150L, fresh.getInitialCount(), "initialCount += delta");
        assertEquals(2, fresh.getHandicapChunks().size());

        // Frozen value: re-scan the same chunks at a different value (e.g.
        // player built more blocks in chunk world:1). The persisted map
        // doesn't update, the delta is 0, and player progress goes through
        // the live scan total instead of into the handicap.
        Map<String, Long> rescan = Map.of("world:1", 500L, "world:2", 50L);
        long delta2 = lm.reconcileHandicapChunks(island, rescan, false);
        assertEquals(0L, delta2, "known chunks are frozen — no double-credit");
        assertEquals(150L, fresh.getInitialCount(), "initialCount stays put");
        assertEquals(100L, fresh.getHandicapChunks().get("world:1"));

        // Self-heal: a new chunk shows up that the lazy-zero listener missed
        // (e.g. pre-generated before the island existed). Regular scan picks
        // it up and adds it to both the map and initialCount, so the level
        // result this scan computes is zero for that natural terrain.
        Map<String, Long> selfHeal = Map.of("world:1", 100L, "world:2", 50L, "world:3", 77L);
        long delta3 = lm.reconcileHandicapChunks(island, selfHeal, false);
        assertEquals(77L, delta3, "only the new chunk contributes to the delta");
        assertEquals(227L, fresh.getInitialCount());
        assertEquals(3, fresh.getHandicapChunks().size());
    }

    /**
     * Legacy migration: empty handicapChunks + non-zero initialCount means
     * an island that was zeroed before this feature existed. The current
     * scan seeds the map but does NOT change initialCount — that would
     * double-count the chunks already represented by the legacy count and
     * push the level negative.
     */
    @Test
    void testReconcileHandicapChunksLegacyMigration() throws Exception {
        IslandLevels legacy = new IslandLevels(uuid.toString());
        legacy.setInitialCount(5000L); // existing baseline from before the upgrade
        when(handler.loadObject(uuid.toString())).thenReturn(legacy);

        Map<String, Long> scan = Map.of("world:1", 100L, "world:2", 200L);
        long delta = lm.reconcileHandicapChunks(island, scan, false);
        assertEquals(0L, delta, "no delta on legacy migration");
        assertEquals(5000L, legacy.getInitialCount(),
                "initialCount preserved across the migration");
        assertEquals(2, legacy.getHandicapChunks().size(),
                "map seeded from the scan");
        assertEquals(100L, legacy.getHandicapChunks().get("world:1"));
    }

    /**
     * Zero-scan reconciliation hard-resets the map and returns 0 (the caller
     * rewrites initialCount separately via setInitialIslandCount).
     */
    @Test
    void testReconcileHandicapChunksZeroScanOverwrites() throws Exception {
        IslandLevels data = new IslandLevels(uuid.toString());
        data.setInitialCount(9999L);
        data.getHandicapChunks().put("world:stale", 1234L);
        when(handler.loadObject(uuid.toString())).thenReturn(data);

        Map<String, Long> scan = Map.of("world:1", 10L, "world:2", 20L);
        long delta = lm.reconcileHandicapChunks(island, scan, true);
        assertEquals(0L, delta, "zero scans return 0 — caller does the reset");
        assertEquals(9999L, data.getInitialCount(),
                "initialCount untouched — IslandActivitiesListeners sets it");
        assertEquals(2, data.getHandicapChunks().size(),
                "stale entries gone, new ones in");
        assertFalse(data.getHandicapChunks().containsKey("world:stale"));
    }

    /**
     * The listener-side {@link LevelsManager#addHandicapChunk} freezes the
     * chunk value the first time it's recorded — a second call with a
     * different value (e.g. the same chunk firing twice through some
     * external pregen pipeline) is a no-op for both the map and initialCount.
     */
    @Test
    void testAddHandicapChunkFreezesValue() throws Exception {
        IslandLevels data = new IslandLevels(uuid.toString());
        when(handler.loadObject(uuid.toString())).thenReturn(data);

        assertTrue(lm.addHandicapChunk(island, "world:42", 200L));
        assertEquals(200L, data.getInitialCount());

        // Second call: chunk already known, both map and initialCount stay put.
        assertFalse(lm.addHandicapChunk(island, "world:42", 999L));
        assertEquals(200L, data.getInitialCount());
        assertEquals(200L, data.getHandicapChunks().get("world:42"));
    }

    /**
     * Pin down that completePendingZero never lets the per-island counter drift
     * negative and removes the map entry when it reaches zero. Without these
     * guarantees the map would leak one AtomicInteger per island that ever had
     * a queued snapshot, and an extra complete (e.g. from a duplicated chunk
     * event) would push the counter below zero so awaitPendingZeros could
     * release a scan before later-queued snapshots had landed.
     */
    @Test
    void testPendingZeroClampsAndRemovesEntry() {
        // No add → complete is a no-op and leaves no entry behind.
        assertEquals(0, lm.getPendingZeroCount(island));
        lm.completePendingZero(island);
        assertEquals(0, lm.getPendingZeroCount(island));

        // Pair of add/complete: counter ends at 0 and the entry is dropped.
        lm.addPendingZero(island);
        assertEquals(1, lm.getPendingZeroCount(island));
        lm.completePendingZero(island);
        assertEquals(0, lm.getPendingZeroCount(island));

        // Extra complete after the counter is empty cannot push it negative.
        lm.completePendingZero(island);
        assertEquals(0, lm.getPendingZeroCount(island));

        // Add → complete → add still tracks the second add correctly even
        // though the first cycle removed the map entry mid-flight.
        lm.addPendingZero(island);
        lm.completePendingZero(island);
        lm.addPendingZero(island);
        assertEquals(1, lm.getPendingZeroCount(island));
        lm.completePendingZero(island);
        assertEquals(0, lm.getPendingZeroCount(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.LevelsManager#getRank(World, UUID)}
     */
    @Test
    void testGetRank() {
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
