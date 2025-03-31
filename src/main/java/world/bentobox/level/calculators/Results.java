package world.bentobox.level.calculators;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Where results are stored
 */
public class Results {
    public enum Result {
        /**
         * A level calc is already in progress
         */
        IN_PROGRESS,
        /**
         * Results will be available
         */
        AVAILABLE,
        /**
         * Result if calculation timed out
         */
        TIMEOUT
    }
    List<String> report;
    /**
     * MaterialData count anything above sea level
     */
    final Multiset<Object> mdCount = HashMultiset.create();
    /**
     * Underwater count
     */
    final Multiset<Object> uwCount = HashMultiset.create();
    /**
     * Not-in-config count - blocks not listed in the scoring config file
     */
    final Multiset<Object> ncCount = HashMultiset.create();
    /**
     * Blocks not counted because they exceeded limits
     */
    final Multiset<Object> ofCount = HashMultiset.create();
    // AtomicLong and AtomicInteger must be used because they are changed by multiple concurrent threads
    AtomicLong rawBlockCount = new AtomicLong(0);
    AtomicLong underWaterBlockCount = new AtomicLong(0);
    AtomicLong level = new AtomicLong(0);
    AtomicInteger deathHandicap = new AtomicInteger(0);
    AtomicLong pointsToNextLevel = new AtomicLong(0);
    //AtomicLong initialLevel = new AtomicLong(0);
    AtomicLong initialCount = new AtomicLong(0);
    /**
     * Total points before any death penalties
     */
    AtomicLong totalPoints = new AtomicLong(0);
    final Result state;

    public Results(Result state) {
        this.state = state;
    }

    public Results() {
        this.state = Result.AVAILABLE;
    }
    /**
     * @return the deathHandicap
     */
    public int getDeathHandicap() {
        return deathHandicap.get();
    }
    /**
     * Set the death handicap
     * @param handicap
     */
    public void setDeathHandicap(int handicap) {
        deathHandicap.set(handicap);
    }

    /**
     * @return the report
     */
    public List<String> getReport() {
        return report;
    }
    /**
     * Set level
     * @param level - level
     */
    public void setLevel(long level) {
        this.level.set(level);
    }
    /**
     * @return the level
     */
    public long getLevel() {
        return level.get();
    }
    /**
     * @return the pointsToNextLevel
     */
    public long getPointsToNextLevel() {
        return pointsToNextLevel.get();
    }

    /**
     * Set the points to next level
     * @param points
     */
    public void setPointsToNextLevel(long points) {
        pointsToNextLevel.set(points);
    }

    /**
     * @return the totalPoints
     */
    public long getTotalPoints() {
        return totalPoints.get();
    }

    /**
     * Set the total points
     * @param points
     */
    public void setTotalPoints(long points) {
        totalPoints.set(points);
    }

    /*
    public long getInitialLevel() {
        return initialLevel.get();
    }
    
    public void setInitialLevel(long initialLevel) {
        this.initialLevel.set(initialLevel);
    }
    */
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /*
    @Override
    public String toString() {
        return "Results [report=" + report + ", mdCount=" + mdCount + ", uwCount=" + uwCount + ", ncCount="
                + ncCount + ", ofCount=" + ofCount + ", rawBlockCount=" + rawBlockCount + ", underWaterBlockCount="
                + underWaterBlockCount + ", level=" + level + ", deathHandicap=" + deathHandicap
                + ", pointsToNextLevel=" + pointsToNextLevel + ", totalPoints=" + totalPoints + ", initialLevel=" + initialLevel + "]";
    }*/
    /**
     * @return the mdCount
     */
    public Multiset<Object> getMdCount() {
        return mdCount;
    }
    /**
     * @return the uwCount
     */
    public Multiset<Object> getUwCount() {
        return uwCount;
    }
    /**
     * @return the state
     */
    public Result getState() {
        return state;
    }

    /**
     * @return the initialCount
     */
    public long getInitialCount() {
        return initialCount.get();
    }

    /**
     * @param long1 the initialCount to set
     */
    public void setInitialCount(Long count) {
        this.initialCount.set(count);
    }

}
