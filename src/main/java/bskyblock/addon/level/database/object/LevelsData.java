package bskyblock.addon.level.database.object;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import us.tastybento.bskyblock.database.objects.DataObject;

public class LevelsData implements DataObject {
    
    // uniqueId is the player's UUID
    @Expose
    private String uniqueId = "";
    
    // Map - world name, level
    @Expose
    private Map<String, Long> levels = new HashMap<>();

    /**
     * Create a level entry for target player
     * @param targetPlayer
     * @param level
     * @param world
     */
    public LevelsData(UUID targetPlayer, long level, World world) {
        uniqueId = targetPlayer.toString();
        levels.put(world.getName(), level);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.objects.DataObject#getUniqueId()
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.objects.DataObject#setUniqueId(java.lang.String)
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Long getLevel(World world) {
        return levels.getOrDefault(world.getName(), 0L);
    }
    
    /**
     * @return the levels
     */
    public Map<String, Long> getLevels() {
        return levels;
    }

    /**
     * @param levels the levels to set
     */
    public void setLevels(Map<String, Long> levels) {
        this.levels = levels;
    }

    public void setLevel(World world, Long lv) {
        levels.put(world.getName(),lv);
    }
}
