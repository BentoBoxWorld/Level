package world.bentobox.level.calculators;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Results class
 *
 */
public class Results {
    private int deathHandicap = 0;
    private long initialLevel = 0;
    private long level = 0;
    private final Multiset<Material> mdCount = HashMultiset.create();
    private final Multiset<Material> ncCount = HashMultiset.create();
    private final Multiset<Material> ofCount = HashMultiset.create();
    private long pointsToNextLevel = 0;
    private long rawBlockCount = 0;
    private List<String> report = new ArrayList<>();
    private long underWaterBlockCount = 0;
    private final Multiset<Material> uwCount = HashMultiset.create();

    /**
     * @return the deathHandicap
     */
    public int getDeathHandicap() {
        return deathHandicap;
    }

    public long getInitialLevel() {
        return initialLevel;
    }
    /**
     * @return the level
     */
    public long getLevel() {
        return level;
    }
    /**
     * @return the mdCount
     */
    public Multiset<Material> getMdCount() {
        return mdCount;
    }
    /**
     * @return the ncCount
     */
    public Multiset<Material> getNcCount() {
        return ncCount;
    }

    /**
     * @return the ofCount
     */
    public Multiset<Material> getOfCount() {
        return ofCount;
    }

    /**
     * @return the pointsToNextLevel
     */
    public long getPointsToNextLevel() {
        return pointsToNextLevel;
    }

    /**
     * @return the rawBlockCount
     */
    public long getRawBlockCount() {
        return rawBlockCount;
    }

    /**
     * @return the report
     */
    public List<String> getReport() {
        return report;
    }

    /**
     * @return the underWaterBlockCount
     */
    public long getUnderWaterBlockCount() {
        return underWaterBlockCount;
    }

    /**
     * @return the uwCount
     */
    public Multiset<Material> getUwCount() {
        return uwCount;
    }

    /**
     * @param deathHandicap the deathHandicap to set
     */
    public void setDeathHandicap(int deathHandicap) {
        this.deathHandicap = deathHandicap;
    }

    public void setInitialLevel(long initialLevel) {
        this.initialLevel = initialLevel;
    }

    /**
     * Set level
     * @param level - level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(long level) {
        this.level = level;
    }

    /**
     * @param pointsToNextLevel the pointsToNextLevel to set
     */
    public void setPointsToNextLevel(long pointsToNextLevel) {
        this.pointsToNextLevel = pointsToNextLevel;
    }

    /**
     * @param rawBlockCount the rawBlockCount to set
     */
    public void setRawBlockCount(long rawBlockCount) {
        this.rawBlockCount = rawBlockCount;
    }

    /**
     * @param report the report to set
     */
    public void setReport(List<String> report) {
        this.report = report;
    }

    /**
     * @param underWaterBlockCount the underWaterBlockCount to set
     */
    public void setUnderWaterBlockCount(long underWaterBlockCount) {
        this.underWaterBlockCount = underWaterBlockCount;
    }

    /**
     * Add to death handicap
     * @param deaths - number to add
     */
    public void addToDeathHandicap(int deaths) {
        this.deathHandicap += deaths;

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Results [report=" + report + ", mdCount=" + mdCount + ", uwCount=" + getUwCount() + ", ncCount="
                + ncCount + ", ofCount=" + ofCount + ", rawBlockCount=" + rawBlockCount + ", underWaterBlockCount="
                + getUnderWaterBlockCount() + ", level=" + level + ", deathHandicap=" + deathHandicap
                + ", pointsToNextLevel=" + pointsToNextLevel + ", initialLevel=" + initialLevel + "]";
    }

}