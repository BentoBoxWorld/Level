package world.bentobox.level.objects;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

@Table(name = "LevelsData")
public class LevelsData implements DataObject {

    // uniqueId is the player's UUID
    @Expose
    private String uniqueId = "";

    /**
     * Map of world name and island level
     */
    @Expose
    private Map<String, Long> levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    /**
     * Map of world name to island initial level
     */
    @Expose
    private Map<String, Long> initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public LevelsData() {} // For Bean loading

    /**
     * Create a level entry for target player
     * @param targetPlayer - target player
     * @param level - level
     * @param world - world
     */
    public LevelsData(UUID targetPlayer, long level, World world) {
        uniqueId = targetPlayer.toString();
        levels.put(world.getName(), level);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bbox.database.objects.DataObject#getUniqueId()
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bbox.database.objects.DataObject#setUniqueId(java.lang.String)
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Get the island level for this world
     * @param world - world
     * @return island level
     */
    public Long getLevel(World world) {
        return world == null ? 0L : levels.getOrDefault(world.getName(), 0L);
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

    /**
     * Set the initial level of the island for this world
     * @param world - world
     * @param level - level
     */
    public void setInitialLevel(World world, long level) {
        this.initialLevel.put(world.getName(), level);
    }

    /**
     * @return the initialLevel
     */
    public Map<String, Long> getInitialLevel() {
        return initialLevel;
    }

    /**
     * @param initialLevel the initialLevel to set
     */
    public void setInitialLevel(Map<String, Long> initialLevel) {
        this.initialLevel = initialLevel;
    }

    /**
     * Get the initial island level for this world
     * @param world - world
     * @return initial island level or 0 by default
     */
    public long getInitialLevel(World world) {
        return initialLevel.getOrDefault(world.getName(), 0L);
    }
}
