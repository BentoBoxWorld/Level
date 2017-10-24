package bskyblock.addin.level.database.object;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import us.tastybento.bskyblock.database.objects.DataObject;

/**
 * This class stores and sorts the top ten.
 * @author ben
 *
 */
public class TopTenList extends DataObject {
    
    private String uniqueId = "topten";
    private HashMap<UUID, Long> topTen = new HashMap<>();

    public HashMap<UUID, Long> getTopTen() {
        return topTen;
    }

    public void setTopTen(HashMap<UUID, Long> topTen) {
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
        sortTopTen();
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
    
    /**
     * Sorts the top ten and limits it to 10 entries
     */
    void sortTopTen() {
        topTen = (HashMap<UUID, Long>) topTen.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
