package world.bentobox.level.util;

import java.time.Instant;
import java.util.Map;

/**
 * Cache for top tens
 */
public class CachedData {
    private Map<String, Long> cachedMap;
    private Instant lastUpdated;

    public CachedData(Map<String, Long> cachedMap, Instant lastUpdated) {
        this.cachedMap = cachedMap;
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Long> getCachedMap() {
        return cachedMap;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void updateCache(Map<String, Long> newMap, Instant newUpdateTime) {
        this.cachedMap = newMap;
        this.lastUpdated = newUpdateTime;
    }
}
