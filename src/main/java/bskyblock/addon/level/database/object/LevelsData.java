package bskyblock.addon.level.database.object;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;

public class LevelsData implements DataObject {

    // uniqueId is the player's UUID
    @Expose
    private String uniqueId = "";

    // Map - world name, level
    @Expose
    private Map<String, Long> levels = new HashMap<>();
    @Expose
    private long initialIslandLevel = 0;

    public LevelsData() {} // For Bean loading

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
     * @return island level, less the initialIslandLevel
     */
    public Long getLevel(World world) {
        return world == null ? -initialIslandLevel : levels.getOrDefault(world.getName(), 0L) - initialIslandLevel;
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
     * @return the initialIslandLevel
     */
    public long getInitialIslandLevel() {
        return initialIslandLevel;
    }

    /**
     * @param initialIslandLevel the initialIslandLevel to set
     */
    public void setInitialIslandLevel(long initialIslandLevel) {
        this.initialIslandLevel = initialIslandLevel;
    }
}
