package bskyblock.addon.level.database.object;

import us.tastybento.bskyblock.database.objects.DataObject;

public class Levels extends DataObject {
    
    private String uniqueId = "";
    private long level = 0;

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

}
