package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.level.objects.IslandLevels;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class })
public class PlaceholderManagerTest {

    @Mock
    private Level addon;
    @Mock
    private GameModeAddon gm;
    @Mock
    private BentoBox plugin;

    private PlaceholderManager phm;
    @Mock
    private PlaceholdersManager bpm;
    @Mock
    private LevelsManager lm;
    @Mock
    private World world;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private User user;
    private static final Map<UUID, String> names = new LinkedHashMap<>();
    static {
	names.put(UUID.randomUUID(), "tasty");
	names.put(UUID.randomUUID(), "bento");
	names.put(UUID.randomUUID(), "fred");
	names.put(UUID.randomUUID(), "bonne");
	names.put(UUID.randomUUID(), "cyprien");
	names.put(UUID.randomUUID(), "mael");
	names.put(UUID.randomUUID(), "joe");
	names.put(UUID.randomUUID(), "horacio");
	names.put(UUID.randomUUID(), "steph");
	names.put(UUID.randomUUID(), "vicky");
    }
    private Map<String, Island> islands = new HashMap<>();
    private Map<String, Long> map = new LinkedHashMap<>();
    private Map<Island, Long> map2 = new LinkedHashMap<>();
    private @NonNull IslandLevels data;
    @Mock
    private PlayersManager pm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(addon.getPlugin()).thenReturn(plugin);
        
        // Users
        when(addon.getPlayers()).thenReturn(pm);
        
        // Users
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(mock(Location.class));
        
        int i = 0;
        for (Entry<UUID, String> n : names.entrySet()) {
            UUID uuid = UUID.randomUUID(); // Random island ID
            Long value = (long)(100 - i++);
            map.put(uuid.toString(), value); // level
            Island is = new Island();
            is.setUniqueId(uuid.toString());
            is.setOwner(n.getKey());
            is.setName(n.getValue() + "'s island");
            islands.put(uuid.toString(), is);
            map2.put(is, value);
        }
        // Sort
        map = map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        when(pm.getName(any())).thenAnswer((Answer<String>) invocation -> names.getOrDefault(invocation.getArgument(0, UUID.class), "unknown"));
        Map<UUID, Integer> members = new HashMap<>();
        names.forEach((uuid, l) -> members.put(uuid, RanksManager.MEMBER_RANK));
        islands.values().forEach(is -> is.setMembers(members));
        
                
        // Placeholders manager for plugin
        when(plugin.getPlaceholdersManager()).thenReturn(bpm);
        
        // Game mode
        AddonDescription desc = new AddonDescription.Builder("bentobox", "AOneBlock", "1.3").description("test").authors("tasty").build();
        when(gm.getDescription()).thenReturn(desc);
        when(gm.getOverWorld()).thenReturn(world);
        when(gm.inWorld(world)).thenReturn(true);
        
        // Islands
        when(im.getIsland(any(World.class), any(User.class))).thenReturn(island);
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.of(island));
        when(im.getIslandById(anyString())).thenAnswer((Answer<Optional<Island>>) invocation -> Optional.of(islands.get(invocation.getArgument(0, String.class))));
        when(im.getIslands(any(), any(UUID.class))).thenReturn(new ArrayList<>(islands.values()));
        when(addon.getIslands()).thenReturn(im);
        
        // Levels Manager
        when(lm.getIslandLevel(any(), any())).thenReturn(1234567L);
        when(lm.getIslandLevelString(any(), any())).thenReturn("1234567");
        when(lm.getPointsToNextString(any(), any())).thenReturn("1234567");
        when(lm.getIslandMaxLevel(any(), any())).thenReturn(987654L);
        when(lm.getTopTen(world, Level.TEN)).thenReturn(map);
        when(lm.getWeightedTopTen(world, Level.TEN)).thenReturn(map2);
        when(lm.formatLevel(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, Long.class).toString());
        
        data = new IslandLevels("uniqueId");
        data.setTotalPoints(12345678);
        when(lm.getLevelsData(island)).thenReturn(data);
        when(addon.getManager()).thenReturn(lm);

        phm = new PlaceholderManager(addon);
    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#PlaceholderManager(world.bentobox.level.Level)}.
     */
    @Test
    public void testPlaceholderManager() {
	verify(addon).getPlugin();
    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#registerPlaceholders(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testRegisterPlaceholders() {
	phm.registerPlaceholders(gm);
	// Island Level
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_island_level"), any());
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_island_level_raw"), any());
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_island_total_points"), any());

	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_points_to_next_level"), any());
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_island_level_max"), any());

	// Visited Island Level
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_visited_island_level"), any());

	// Register Top Ten Placeholders
	for (int i = 1; i < 11; i++) {
	    // Name
	    verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_top_name_" + i), any());
	    // Island Name
	    verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_top_island_name_" + i), any());
	    // Members
	    verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_top_members_" + i), any());
	    // Level
	    verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_top_value_" + i), any());
	}

	// Personal rank
	verify(bpm).registerPlaceholder(eq(addon), eq("aoneblock_rank_value"), any());

    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getRankName(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankName() {
	// Test extremes
	assertEquals("tasty", phm.getRankName(world, 0, false));
	assertEquals("vicky", phm.getRankName(world, 100, false));
	// Test the ranks
	int rank = 1;
	for (String name : names.values()) {
	    assertEquals(name, phm.getRankName(world, rank++, false));
	}

    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getRankIslandName(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankIslandName() {
	// Test extremes
	assertEquals("tasty's island", phm.getRankIslandName(world, 0, false));
	assertEquals("vicky's island", phm.getRankIslandName(world, 100, false));
	// Test the ranks
	int rank = 1;
	for (String name : names.values()) {
	    assertEquals(name + "'s island", phm.getRankIslandName(world, rank++, false));
	}

    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getRankMembers(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankMembers() {
	// Test extremes
	check(1, phm.getRankMembers(world, 0, false));
	check(2, phm.getRankMembers(world, 100, false));
	// Test the ranks
	for (int rank = 1; rank < 11; rank++) {
	    check(3, phm.getRankMembers(world, rank, false));
	}
    }

    void check(int indicator, String list) {
	for (String n : names.values()) {
	    assertTrue(n + " is missing for test " + indicator, list.contains(n));
	}
    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getRankLevel(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankLevel() {
	// Test extremes
	assertEquals("100", phm.getRankLevel(world, 0, false));
	assertEquals("91", phm.getRankLevel(world, 100, false));
	// Test the ranks
	for (int rank = 1; rank < 11; rank++) {
	    assertEquals(String.valueOf(101 - rank), phm.getRankLevel(world, rank, false));
	}

    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getRankLevel(org.bukkit.World, int)}.
     */
    @Test
    public void testGetWeightedRankLevel() {
	// Test extremes
	assertEquals("100", phm.getRankLevel(world, 0, true));
	assertEquals("91", phm.getRankLevel(world, 100, true));
	// Test the ranks
	for (int rank = 1; rank < 11; rank++) {
	    assertEquals(String.valueOf(101 - rank), phm.getRankLevel(world, rank, true));
	}

    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelNullUser() {
	assertEquals("", phm.getVisitedIslandLevel(gm, null));

    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelUserNotInWorld() {
        // Another world
        when(user.getWorld()).thenReturn(mock(World.class));
        assertEquals("", phm.getVisitedIslandLevel(gm, user));
        
    }

    /**
     * Test method for
     * {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevel() {
	assertEquals("1234567", phm.getVisitedIslandLevel(gm, user));

    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelNoIsland() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        assertEquals("0", phm.getVisitedIslandLevel(gm, user));
        
    }

}
