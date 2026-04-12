package world.bentobox.level.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TopTenData}
 */
public class TopTenDataTest {

    private World world;
    private TopTenData topTenData;

    @BeforeEach
    public void setUp() {
        world = mock(World.class);
        when(world.getName()).thenReturn("BSkyBlock_world");
        topTenData = new TopTenData(world);
    }

    @Test
    public void testTopTenStartsEmpty() {
        assertNotNull(topTenData.getTopTen());
        assertTrue(topTenData.getTopTen().isEmpty());
    }

    @Test
    public void testSetAndGetTopTen() {
        Map<String, Long> newMap = new ConcurrentHashMap<>();
        newMap.put("island-uuid-1", 100L);
        newMap.put("island-uuid-2", 200L);

        topTenData.setTopTen(newMap);

        assertEquals(newMap, topTenData.getTopTen());
        assertEquals(100L, topTenData.getTopTen().get("island-uuid-1"));
        assertEquals(200L, topTenData.getTopTen().get("island-uuid-2"));
    }

    @Test
    public void testTopTenIsMutable() {
        topTenData.getTopTen().put("new-island", 50L);
        assertEquals(50L, topTenData.getTopTen().get("new-island"));
    }

    @Test
    public void testSetTopTenReplacesExisting() {
        topTenData.getTopTen().put("old-island", 10L);
        Map<String, Long> newMap = new ConcurrentHashMap<>();
        newMap.put("new-island", 500L);
        topTenData.setTopTen(newMap);
        assertEquals(1, topTenData.getTopTen().size());
        assertEquals(500L, topTenData.getTopTen().get("new-island"));
    }
}
