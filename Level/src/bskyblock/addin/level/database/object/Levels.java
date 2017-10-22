package bskyblock.addin.level.database.object;

import java.util.HashMap;
import java.util.UUID;

import us.tastybento.bskyblock.database.objects.DataObject;

public class Levels extends DataObject {
    
    private String uniqueId = "addon-levels";
    private HashMap<UUID, Long> islandLevel = new HashMap<>();

    @Override
    public String getUniqueId() {
        return "addon-levels";
    }

    @Override
    public void setUniqueId(String uniqueId) {
        // do nothing
    }

    public HashMap<UUID, Long> getIslandLevel() {
        return islandLevel;
    }

    public void setIslandLevel(HashMap<UUID, Long> islandLevel) {
        this.islandLevel = islandLevel;
    }

    public void addLevel(UUID uuid, Long level) {
        this.islandLevel.put(uuid, level);
    }
    
    public Long getLevel(UUID uuid) {
        if (islandLevel.containsKey(uuid))
            return (long)islandLevel.get(uuid);
        return 0L;
    }
}
