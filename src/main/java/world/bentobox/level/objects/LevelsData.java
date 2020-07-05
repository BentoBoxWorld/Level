package world.bentobox.level.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;

import com.google.common.collect.Multiset;
import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 * Stores the levels data of the user.
 * A note - if this class is extended to support new exposed fields and legacy data doesn't include those fields
 * they will be set to null by GSON. They will not be initialized and if any attempt is made to use them, then
 * the JVM will give up WITHOUT AN ERROR!!! That is why there are null checks throughout this class.
 *
 * @author tastybento
 *
 */
@Table(name = "LevelsData")
public class LevelsData implements DataObject {

    // uniqueId is the player's UUID
    @Expose
    private String uniqueId = "";

    /**
     * Map of world name and island level
     */
    @Expose
    private Map<String, Long> levels;
    /**
     * Map of world name to island initial level
     */
    @Expose
    private Map<String, Long> initialLevel;
    /**
     * Map of world name to points to next level
     */
    @Expose
    private Map<String, Long> pointsToNextLevel;

    @Expose
    private Map<String, Map<Material, Integer>> uwCount;

    @Expose
    private Map<String, Map<Material, Integer>> mdCount;

    /**
     * Create a level entry for target player
     * @param targetPlayer - target player
     * @param level - level
     * @param world - world
     */
    public LevelsData(UUID targetPlayer) {
        uniqueId = targetPlayer.toString();
        levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        pointsToNextLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        uwCount = new HashMap<>();
        mdCount = new HashMap<>();
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
        if (levels == null) levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return world == null ? 0L : levels.getOrDefault(world.getName(), 0L);
    }

    /**
     * @return the levels
     */
    public Map<String, Long> getLevels() {
        if (levels == null) levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return levels;
    }

    /**
     * @param levels the levels to set
     */
    public void setLevels(Map<String, Long> levels) {
        if (levels == null) levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.levels = levels;
    }

    /**
     * Sets the island level to level - the initial level
     * @param world - world where island is
     * @param lv - level
     */
    public void setLevel(World world, Long lv) {
        if (levels == null) levels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String name = world.getName().toLowerCase(Locale.ENGLISH);
        levels.put(name, lv - this.initialLevel.getOrDefault(name, 0L));
    }

    /**
     * Set the initial level of the island for this world
     * @param world - world
     * @param level - level
     */
    public void setInitialLevel(World world, long level) {
        if (initialLevel == null) initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.initialLevel.put(world.getName().toLowerCase(Locale.ENGLISH), level);
    }

    /**
     * @return the initialLevel
     */
    public Map<String, Long> getInitialLevel() {
        if (initialLevel == null) initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return initialLevel;
    }

    /**
     * @param initialLevel the initialLevel to set
     */
    public void setInitialLevel(Map<String, Long> initialLevel) {
        if (initialLevel == null) initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.initialLevel = initialLevel;
    }

    /**
     * Get the initial island level for this world
     * @param world - world
     * @return initial island level or 0 by default
     */
    public long getInitialLevel(World world) {
        if (initialLevel == null) initialLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return initialLevel.getOrDefault(world.getName().toLowerCase(Locale.ENGLISH), 0L);
    }

    /**
     * Remove a world from a player's data
     * @param world - world to remove
     */
    public void remove(World world) {
        this.levels.remove(world.getName().toLowerCase(Locale.ENGLISH));
        this.initialLevel.remove(world.getName().toLowerCase(Locale.ENGLISH));
        this.pointsToNextLevel.remove(world.getName().toLowerCase(Locale.ENGLISH));
        this.mdCount.remove(world.getName().toLowerCase(Locale.ENGLISH));
        this.uwCount.remove(world.getName().toLowerCase(Locale.ENGLISH));
    }

    /**
     * @return the pointsToNextLevel
     */
    public Map<String, Long> getPointsToNextLevel() {
        if (pointsToNextLevel == null) pointsToNextLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return pointsToNextLevel;
    }

    /**
     * @param pointsToNextLevel the pointsToNextLevel to set
     */
    public void setPointsToNextLevel(Map<String, Long> pointsToNextLevel) {
        if (pointsToNextLevel == null) pointsToNextLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.pointsToNextLevel = pointsToNextLevel;
    }

    /**
     * Sets the island points to next level.
     * This is calculated the last time the level was calculated and will not change dynamically.
     * @param world - world where island is
     * @param points - points to next level
     */
    public void setPointsToNextLevel(World world, Long points) {
        if (pointsToNextLevel == null) pointsToNextLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        pointsToNextLevel.put(world.getName().toLowerCase(Locale.ENGLISH), points);
    }

    /**
     * Get the points required to get to the next island level for this world.
     * This is calculated when the island level is calculated and will not change dynamically.
     * @param world - world
     * @return points to next level or zero if unknown
     */
    public long getPointsToNextLevel(World world) {
        if (pointsToNextLevel == null) pointsToNextLevel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return pointsToNextLevel.getOrDefault(world.getName().toLowerCase(Locale.ENGLISH), 0L);
    }

    /**
     * @param uwCount the uwCount to set
     */
    public void setUwCount(World world, Multiset<Material> uwCount) {
        if (this.uwCount == null) this.uwCount = new HashMap<>();
        Map<Material, Integer> count = new HashMap<>();
        uwCount.forEach(m -> count.put(m, uwCount.count(m)));

        this.uwCount.put(world.getName(), count);
    }

    /**
     * @param mdCount the mdCount to set
     */
    public void setMdCount(World world, Multiset<Material> mdCount) {
        if (this.mdCount == null) this.mdCount = new HashMap<>();
        Map<Material, Integer> count = new HashMap<>();
        mdCount.forEach(m -> count.put(m, mdCount.count(m)));

        this.mdCount.put(world.getName(), count);

    }

    /**
     * Get the underwater block count for world
     * @return the uwCount
     */
    public Map<Material, Integer> getUwCount(World world) {
        if (this.uwCount == null) this.uwCount = new HashMap<>();
        return uwCount.getOrDefault(world.getName(), Collections.emptyMap());
    }

    /**
     * Get the over-water block count for world
     * @return the mdCount
     */
    public Map<Material, Integer> getMdCount(World world) {
        if (this.mdCount == null) this.mdCount = new HashMap<>();
        return mdCount.getOrDefault(world.getName(), Collections.emptyMap());
    }


}
