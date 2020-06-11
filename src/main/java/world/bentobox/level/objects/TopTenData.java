package world.bentobox.level.objects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 * This class stores and sorts the top ten.
 * @author tastybento
 *
 */
@Table(name = "TopTenData")
public class TopTenData implements DataObject {

    // UniqueId is the world name
    @Expose
    private String uniqueId = "";
    @Expose
    private Map<UUID, Long> topTen = new LinkedHashMap<>();

    public Map<UUID, Long> getTopTen() {
        // Remove any entries that have level values less than 1
        //topTen.values().removeIf(l -> l < 1);
        // make copy
        Map<UUID, Long> snapTopTen = Collections.unmodifiableMap(topTen);
        return snapTopTen.entrySet().stream()
                .filter(l -> l.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Get the level for the rank
     * @param rank - rank
     * @return level value or 0 if none.
     */
    public long getTopTenLevel(int rank) {
        Map<UUID, Long> tt = getTopTen();
        return rank <= tt.size() ? (long)tt.values().toArray()[(rank-1)] : 0;
    }

    /**
     * Get the UUID of the rank
     * @param rank - rank
     * @return UUID or null
     */
    @Nullable
    public UUID getTopTenUUID(int rank) {
        Map<UUID, Long> tt = getTopTen();
        return rank <= tt.size() ? (UUID)tt.keySet().toArray()[(rank-1)] : null;
    }

    public void setTopTen(Map<UUID, Long> topTen) {
        this.topTen = topTen;
    }

    @Override
    public String getUniqueId() {
        // This is the world name
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        // This is the world name - make it always lowercase
        this.uniqueId = uniqueId.toLowerCase();
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
     * @param uuid - UUID to check
     * @return island level
     */
    public long getLevel(UUID uuid) {
        if (topTen.containsKey(uuid))
            return topTen.get(uuid);
        return 0L;
    }

    /**
     * Removes ownerUUID from the top ten
     * @param ownerUUID - UUID to remove
     */
    public void remove(UUID ownerUUID) {
        this.topTen.remove(ownerUUID);
    }

}
