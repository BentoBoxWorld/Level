package world.bentobox.level.calculators;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Material;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class Results {
    List<String> report;
    final Multiset<Material> mdCount = HashMultiset.create();
    final Multiset<Material> uwCount = HashMultiset.create();
    final Multiset<Material> ncCount = HashMultiset.create();
    final Multiset<Material> ofCount = HashMultiset.create();
    // AtomicLong and AtomicInteger must be used because they are changed by multiple concurrent threads
    AtomicLong rawBlockCount = new AtomicLong(0);
    AtomicLong underWaterBlockCount = new AtomicLong(0);
    AtomicLong level = new AtomicLong(0);
    AtomicInteger deathHandicap = new AtomicInteger(0);
    AtomicLong pointsToNextLevel = new AtomicLong(0);
    AtomicLong initialLevel = new AtomicLong(0);

    /**
     * @return the deathHandicap
     */
    public int getDeathHandicap() {
        return deathHandicap.get();
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

    public long getInitialLevel() {
        return initialLevel.get();
    }

    public void setInitialLevel(long initialLevel) {
        this.initialLevel.set(initialLevel);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Results [report=" + report + ", mdCount=" + mdCount + ", uwCount=" + uwCount + ", ncCount="
                + ncCount + ", ofCount=" + ofCount + ", rawBlockCount=" + rawBlockCount + ", underWaterBlockCount="
                + underWaterBlockCount + ", level=" + level + ", deathHandicap=" + deathHandicap
                + ", pointsToNextLevel=" + pointsToNextLevel + ", initialLevel=" + initialLevel + "]";
    }

}