package world.bentobox.level.objects;

import java.util.HashMap;
import java.util.Map;

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
    private Map<Object, Integer> uwCount;

    /**
     * MaterialData count - count of all blocks excluding under water
     */
    @Expose
    private Map<Object, Integer> mdCount;

    /**
     * Constructor for new island
     * @param islandUUID - island UUID
     */
    public IslandLevels(String islandUUID) {
        uniqueId = islandUUID;
        uwCount = new HashMap<>();
        mdCount = new HashMap<>();
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
     * The count of underwater blocks
     * @return the uwCount
     */
    public Map<Object, Integer> getUwCount() {
        return uwCount;
    }

    /**
     * Underwater blocks
     * @param map the uwCount to set
     */
    public void setUwCount(Map<Object, Integer> map) {
        this.uwCount = map;
    }

    /**
     * All blocks count except for underwater blocks
     * @return the mdCount
     */
    public Map<Object, Integer> getMdCount() {
        return mdCount;
    }

    /**
     * All blocks except for underwater blocks
     * @param mdCount the mdCount to set
     */
    public void setMdCount(Map<Object, Integer> mdCount) {
        this.mdCount = mdCount;
    }


}
