package bskyblock.addon.level.database.object;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;

import us.tastybento.bskyblock.database.objects.DataObject;

/**
 * This class stores and sorts the top ten.
 * @author ben
 *
 */
public class TopTenData implements DataObject {
    
    // UniqueId is the world name
    @Expose
    private String uniqueId = "";
    @Expose
    private Map<UUID, Long> topTen = new LinkedHashMap<>();
    
    public TopTenData() {}

    public Map<UUID, Long> getTopTen() {
        return topTen.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public void setTopTen(Map<UUID, Long> topTen) {
        this.topTen = topTen;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Add level for this island owner or team leader, sort the top ten and limit to ten entries
     * @param uuid - UUID of owner or team leader
     * @param level - island level
     */
    public void addLevel(UUID uuid, Long level) {
        this.topTen.put(uuid, level);
    }
    
    /**
     * Get the level for this UUID, or zero if the UUID is not found
     * @param uuid
     * @return island level
     */
    public long getLevel(UUID uuid) {
        if (topTen.containsKey(uuid))
            return topTen.get(uuid);
        return 0L;
    }

    /**
     * Removes ownerUUID from the top ten
     * @param ownerUUID
     */
    public void remove(UUID ownerUUID) {
        this.topTen.remove(ownerUUID);        
    }
    
}
