package world.bentobox.level.objects;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Material;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 * Stores the levels data of the island.
 * A note - if this class is extended to support new exposed fields and legacy data doesn't include those fields
 * they will be set to null by GSON. They will not be initialized and if any attempt is made to use them, then
 * the JVM will give up WITHOUT AN ERROR!!! That is why there are null checks throughout this class.
 *
 * @author tastybento
 *
 */
@Table(name = "IslandLevels")
public class IslandLevels implements DataObject {

    /**
     * uniqueId is the island's UUID
     */
    @Expose
    private String uniqueId = "";

    /**
     * Island level
     */
    @Expose
    private long level;
    /**
     * Initial level
     */
    @Expose
    private long initialLevel;
    /**
     * Points to next level
     */
    @Expose
    private long pointsToNextLevel;
    /**
     * The maximum level this island has ever had
     */
    @Expose
    private long maxLevel;

    /**
     * Total points
     */
    @Expose
    private long totalPoints;

    /**
     * Underwater count
     */
    @Expose
    private Map<Material, Integer> uwCount;

    /**
     * MaterialData count - count of all blocks
     */
    @Expose
    private Map<Material, Integer> mdCount;

    /**
     * Constructor for new island
     * @param islandUUID - island UUID
     */
    public IslandLevels(String islandUUID) {
        uniqueId = islandUUID;
        uwCount = new EnumMap<>(Material.class);
        mdCount = new EnumMap<>(Material.class);
    }

    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the level
     */
    public long getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(long level) {
        this.level = level;
        // Track maximum level
        if (level > this.maxLevel) {
            maxLevel = level;
        }
    }

    /**
     * @return the initialLevel
     */
    public long getInitialLevel() {
        return initialLevel;
    }

    /**
     * @param initialLevel the initialLevel to set
     */
    public void setInitialLevel(long initialLevel) {
        this.initialLevel = initialLevel;
    }

    /**
     * @return the pointsToNextLevel
     */
    public long getPointsToNextLevel() {
        return pointsToNextLevel;
    }

    /**
     * @param pointsToNextLevel the pointsToNextLevel to set
     */
    public void setPointsToNextLevel(long pointsToNextLevel) {
        this.pointsToNextLevel = pointsToNextLevel;
    }

    /**
     * @return the totalPoints
     */
    public long getTotalPoints() {
        return totalPoints;
    }

    /**
     * @param totalPoints the totalPoints to set
     */
    public void setTotalPoints(long totalPoints) {
        this.totalPoints = totalPoints;
    }
        
    /**
     * Get the maximum level ever set using {@link #setLevel(long)}
     * @return the maxLevel
     */
    public long getMaxLevel() {
        return maxLevel;
    }

    /**
     * @return the uwCount
     */
    public Map<Material, Integer> getUwCount() {
        return uwCount;
    }

    /**
     * @param uwCount the uwCount to set
     */
    public void setUwCount(Map<Material, Integer> uwCount) {
        this.uwCount = uwCount;
    }

    /**
     * @return the mdCount
     */
    public Map<Material, Integer> getMdCount() {
        return mdCount;
    }

    /**
     * @param mdCount the mdCount to set
     */
    public void setMdCount(Map<Material, Integer> mdCount) {
        this.mdCount = mdCount;
    }


}
