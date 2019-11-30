package world.bentobox.level.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * @author tastybento
 *
 */
public class TopTenDataTest {

    private final Map<UUID, Long> topTen = new LinkedHashMap<>();
    private TopTenData ttd;
    private final UUID uuid = UUID.randomUUID();

    @Before
    public void setUp() {
        // Create a top ten map
        for (long i = 0; i < 100; i++) {
            topTen.put(UUID.randomUUID(), i);
        }
        // Add the top player
        topTen.put(uuid,  100L);
        // Add negative values
        for (long i = 0; i < 100; i++) {
            topTen.put(UUID.randomUUID(), - i);
        }
        ttd = new TopTenData();
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#getTopTen()}.
     */
    @Test
    public void testGetTopTen() {
        assertTrue(ttd.getTopTen().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#setTopTen(java.util.Map)}.
     */
    @Test
    public void testSetAndGetTopTen() {
        ttd.setTopTen(topTen);
        // Ten only
        assertEquals(10, ttd.getTopTen().size());
        // Check order
        long i = 100;
        for (long l : ttd.getTopTen().values()) {

            assertEquals(i--, l);
        }
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        assertTrue(ttd.getUniqueId().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#setUniqueId(java.lang.String)}.
     */
    @Test
    public void testSetUniqueId() {
        ttd.setUniqueId("unique");
        assertEquals("unique", ttd.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#addLevel(java.util.UUID, java.lang.Long)}.
     */
    @Test
    public void testAddAndGetLevel() {
        topTen.forEach(ttd::addLevel);
        topTen.keySet().forEach(k -> assertEquals((long) topTen.get(k), ttd.getLevel(k)));
    }

    /**
     * Test method for {@link world.bentobox.level.objects.TopTenData#remove(java.util.UUID)}.
     */
    @Test
    public void testRemove() {
        ttd.remove(uuid);
        // Check order
        long i = 99;
        for (long l : ttd.getTopTen().values()) {
            assertEquals(i--, l);
        }
    }

}
