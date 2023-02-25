package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@PrepareForTest({BentoBox.class})
public class PlaceholderManagerTest {

    @Mock
    private Level addon;
    @Mock
    private GameModeAddon gm;
    @Mock
    private BentoBox plugin;

    private PlaceholderManager pm;
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
    private Map<UUID, String> names = new HashMap<>();
    private static final List<String> NAMES = List.of("tasty", "bento", "fred", "bonne", "cyprien", "mael", "joe", "horacio", "steph", "vicky");
    private Map<UUID, Island> islands = new HashMap<>();
    private Map<UUID, Long> map = new HashMap<>();
    private @NonNull IslandLevels data;
    @Mock
    private PlayersManager players;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(addon.getPlugin()).thenReturn(plugin);
        
        // Users
        when(addon.getPlayers()).thenReturn(players);        
        // Users
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(mock(Location.class));
        
        for (int i = 0; i < Level.TEN; i++) {
            UUID uuid = UUID.randomUUID();
            names.put(uuid, NAMES.get(i));
            map.put(uuid, (long)(100 - i));
            Island is = new Island();
            is.setOwner(uuid);
            is.setName(NAMES.get(i) + "'s island");
            islands.put(uuid, is);
            
        }
        // Sort
        map = map.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        when(players.getName(any())).thenAnswer((Answer<String>) invocation -> names.getOrDefault(invocation.getArgument(0, UUID.class), "unknown"));
        Map<UUID, Integer> members = new HashMap<>();
        map.forEach((uuid, l) -> members.put(uuid, RanksManager.MEMBER_RANK));
        islands.values().forEach(i -> i.setMembers(members));
        
                
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
        when(im.getIsland(any(World.class), any(UUID.class))).thenAnswer((Answer<Island>) invocation -> islands.get(invocation.getArgument(1, UUID.class)));
        when(addon.getIslands()).thenReturn(im);
        
        // Levels Manager
        when(lm.getIslandLevel(any(), any())).thenReturn(1234567L);
        when(lm.getIslandLevelString(any(), any())).thenReturn("1234567");
        when(lm.getPointsToNextString(any(), any())).thenReturn("1234567");
        when(lm.getIslandMaxLevel(any(), any())).thenReturn(987654L);
        when(lm.getTopTen(world, Level.TEN)).thenReturn(map);
        when(lm.formatLevel(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, Long.class).toString());
        
        data = new IslandLevels("uniqueId");
        data.setTotalPoints(12345678);
        when(lm.getLevelsData(island)).thenReturn(data);
        when(addon.getManager()).thenReturn(lm);

        pm = new PlaceholderManager(addon);
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#PlaceholderManager(world.bentobox.level.Level)}.
     */
    @Test
    public void testPlaceholderManager() {
        verify(addon).getPlugin();
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#registerPlaceholders(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testRegisterPlaceholders() {
        pm.registerPlaceholders(gm);
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
     * Test method for {@link world.bentobox.level.PlaceholderManager#getRankName(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankName() {
        // Test extremes
        assertEquals("tasty", pm.getRankName(world, 0));
        assertEquals("vicky", pm.getRankName(world, 100));
        // Test the ranks
        int rank = 1;
        for (String name : NAMES) {
            assertEquals(name, pm.getRankName(world, rank++));
        }
        
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getRankIslandName(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankIslandName() {
        // Test extremes
        assertEquals("tasty's island", pm.getRankIslandName(world, 0));
        assertEquals("vicky's island", pm.getRankIslandName(world, 100));
        // Test the ranks
        int rank = 1;
        for (String name : NAMES) {
            assertEquals(name + "'s island", pm.getRankIslandName(world, rank++));
        }
        
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getRankMembers(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankMembers() {
        // Test extremes  
        check(1, pm.getRankMembers(world, 0));
        check(2, pm.getRankMembers(world, 100));
        // Test the ranks
        for (int rank = 1; rank < 11; rank++) {
            check(3, pm.getRankMembers(world, rank));
        }
    }
    
    void check(int indicator, String list) {
        for (String n : NAMES) {
            assertTrue(n + " is missing for twst " + indicator, list.contains(n));
        }
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getRankLevel(org.bukkit.World, int)}.
     */
    @Test
    public void testGetRankLevel() {
        // Test extremes
        assertEquals("100", pm.getRankLevel(world, 0));
        assertEquals("91", pm.getRankLevel(world, 100));
        // Test the ranks
        for (int rank = 1; rank < 11; rank++) {
            assertEquals(String.valueOf(101 - rank), pm.getRankLevel(world, rank));
        }
        
    }

    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelNullUser() {
        assertEquals("", pm.getVisitedIslandLevel(gm, null));
        
    }
    
    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelUserNotInWorld() {
        // Another world
        when(user.getWorld()).thenReturn(mock(World.class));
        assertEquals("", pm.getVisitedIslandLevel(gm, user));
        
    }
    
    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevel() {
        assertEquals("1234567", pm.getVisitedIslandLevel(gm, user));
        
    }
    
    /**
     * Test method for {@link world.bentobox.level.PlaceholderManager#getVisitedIslandLevel(world.bentobox.bentobox.api.addons.GameModeAddon, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetVisitedIslandLevelNoIsland() {
        when(im.getIslandAt(any(Location.class))).thenReturn(Optional.empty());
        assertEquals("0", pm.getVisitedIslandLevel(gm, user));
        
    }

}
