package world.bentobox.level.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

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
    private Long initialLevel;
    /**
     * Initial count
     */
    @Expose
    private Long initialCount;
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
     * Donated blocks - blocks permanently contributed to island level.
     * Key is the material name (String), value is the count donated.
     * Null-safe for backwards compatibility with legacy data.
     */
    @Expose
    private Map<String, Integer> donatedBlocks;

    /**
     * Total point value of all donated blocks.
     * Null-safe for backwards compatibility.
     */
    @Expose
    private Long donatedPoints;

    /**
     * Audit log of all donations made to this island.
     * Null-safe for backwards compatibility.
     */
    @Expose
    private List<DonationRecord> donationLog;

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
    /*
    public long getInitialLevel() {
        // TODO: Add Backwards compatibility 
        return initialLevel;
    }
    
    /**
     * @param initialLevel the initialLevel to set
     */
    /*
    public void setInitialLevel(long initialLevel) {
        this.initialLevel = initialLevel;
    }
    */
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
        // Loaded objects come in as strings, so need to be converted to Material Or EntityTypes
        uwCount = convertMap(uwCount);
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
        // Loaded objects come in as strings, so need to be converted to Material Or EntityTypes
        mdCount = convertMap(mdCount);
        return mdCount;
    }

    private Map<Object, Integer> convertMap(Map<Object, Integer> blockCountMap) {
        Map<Object, Integer> convertedMap = new HashMap<>();

        for (Map.Entry<Object, Integer> entry : blockCountMap.entrySet()) {
            Object key = entry.getKey();
            Integer value = entry.getValue();

            if (key instanceof String keyStr) {
                // First, try converting to Material
                Material material = Material.matchMaterial(keyStr);
                if (material != null) {
                    convertedMap.put(material, value);
                } else {
                    // Fallback to converting to EntityType (using uppercase as enum constants are uppercase)
                    try {
                        EntityType entityType = EntityType.valueOf(keyStr.toUpperCase(Locale.ENGLISH));
                        convertedMap.put(entityType, value);
                    } catch (IllegalArgumentException ex) {
                        // No valid Material or EntityType found.
                        convertedMap.put(key, value); // Leave the key unchanged.
                    }
                }
            } else {
                // If the key is not a String, add it directly.
                convertedMap.put(key, value);
            }
        }
        return convertedMap;
    }

    /**
     * All blocks except for underwater blocks
     * @param mdCount the mdCount to set
     */
    public void setMdCount(Map<Object, Integer> mdCount) {
        this.mdCount = mdCount;
    }

    /**
     * @return the initialCount
     */
    public Long getInitialCount() {
        return initialCount;
    }

    /**
     * @param initialCount the initialCount to set
     */
    public void setInitialCount(Long initialCount) {
        this.initialCount = initialCount;
    }

    // ---- Donation fields (null-safe for backwards compatibility) ----

    /**
     * Record for tracking individual donations for audit purposes.
     * @param timestamp when the donation was made (epoch millis)
     * @param donorUUID UUID of the player who donated
     * @param material the material name that was donated
     * @param count how many blocks were donated
     * @param points the point value at time of donation
     */
    public record DonationRecord(long timestamp, String donorUUID, String material, int count, long points) {
    }

    /**
     * Get the donated blocks map.
     * @return map of material name to count, never null
     */
    public Map<String, Integer> getDonatedBlocks() {
        if (donatedBlocks == null) {
            donatedBlocks = new HashMap<>();
        }
        return donatedBlocks;
    }

    /**
     * @param donatedBlocks the donatedBlocks to set
     */
    public void setDonatedBlocks(Map<String, Integer> donatedBlocks) {
        this.donatedBlocks = donatedBlocks;
    }

    /**
     * Get the total donated points for this island.
     * @return donated points, never null
     */
    public long getDonatedPoints() {
        return donatedPoints == null ? 0L : donatedPoints;
    }

    /**
     * @param donatedPoints the donatedPoints to set
     */
    public void setDonatedPoints(long donatedPoints) {
        this.donatedPoints = donatedPoints;
    }

    /**
     * Get the donation audit log.
     * @return list of donation records, never null
     */
    public List<DonationRecord> getDonationLog() {
        if (donationLog == null) {
            donationLog = new ArrayList<>();
        }
        return donationLog;
    }

    /**
     * @param donationLog the donationLog to set
     */
    public void setDonationLog(List<DonationRecord> donationLog) {
        this.donationLog = donationLog;
    }

    /**
     * Add a donation to this island's records.
     * @param donorUUID UUID of the player donating
     * @param material the material being donated
     * @param count how many blocks
     * @param points the point value of this donation
     */
    public void addDonation(String donorUUID, String material, int count, long points) {
        getDonatedBlocks().merge(material, count, Integer::sum);
        this.donatedPoints = getDonatedPoints() + points;
        getDonationLog().add(new DonationRecord(System.currentTimeMillis(), donorUUID, material, count, points));
    }

    /**
     * @return the initialLevel
     * @deprecated only used for backwards compatibility. Use {@link #getInitialCount()} instead
     */
    public Long getInitialLevel() {
        return initialLevel;
    }

    /**
     * @param initialLevel the initialLevel to set
     * @deprecated only used for backwards compatil
     */
    public void setInitialLevel(Long initialLevel) {
        this.initialLevel = initialLevel;
    }

}
