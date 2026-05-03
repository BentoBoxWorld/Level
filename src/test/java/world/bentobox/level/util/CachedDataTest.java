package world.bentobox.level.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CachedData}
 */
class CachedDataTest {

    private Map<String, Long> initialMap;
    private Instant initialTime;
    private CachedData cachedData;

    @BeforeEach
    void setUp() {
        initialMap = new HashMap<>();
        initialMap.put("player1", 100L);
        initialMap.put("player2", 200L);
        initialTime = Instant.now();
        cachedData = new CachedData(initialMap, initialTime);
    }

    @Test
    void testConstructorStoresMap() {
        assertNotNull(cachedData.getCachedMap());
        assertEquals(initialMap, cachedData.getCachedMap());
    }

    @Test
    void testConstructorStoresInstant() {
        assertNotNull(cachedData.getLastUpdated());
        assertEquals(initialTime, cachedData.getLastUpdated());
    }

    @Test
    void testGetCachedMapReturnsCorrectEntries() {
        assertEquals(100L, cachedData.getCachedMap().get("player1"));
        assertEquals(200L, cachedData.getCachedMap().get("player2"));
    }

    @Test
    void testUpdateCacheReplacesMap() {
        Map<String, Long> newMap = new HashMap<>();
        newMap.put("player3", 300L);
        Instant newTime = Instant.now().plusSeconds(60);

        cachedData.updateCache(newMap, newTime);

        assertEquals(newMap, cachedData.getCachedMap());
        assertEquals(300L, cachedData.getCachedMap().get("player3"));
    }

    @Test
    void testUpdateCacheReplacesInstant() {
        Instant newTime = Instant.now().plusSeconds(120);
        cachedData.updateCache(new HashMap<>(), newTime);

        assertEquals(newTime, cachedData.getLastUpdated());
    }

    @Test
    void testUpdateCacheOldDataGone() {
        cachedData.updateCache(new HashMap<>(), Instant.now());
        assertEquals(0, cachedData.getCachedMap().size());
    }
}
