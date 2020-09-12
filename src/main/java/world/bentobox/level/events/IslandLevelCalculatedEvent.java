package world.bentobox.level.events;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.calculators.Results;

/**
 * This event is fired after the island level is calculated and before the results are saved.
 * If this event is cancelled, results will saved, but not communicated. i.e., the result will be silent.
 *
 * @author tastybento
 */
public class IslandLevelCalculatedEvent extends IslandBaseEvent {
    private Results results;

    private UUID targetPlayer;

    /**
     * @param targetPlayer - target player
     * @param island - island
     * @param results - results object to set
     */
    public IslandLevelCalculatedEvent(UUID targetPlayer, Island island, Results results) {
        super(island);
        this.targetPlayer = targetPlayer;
        this.results = results;
    }

    /**
     * Do NOT get this result if you are not a BentoBox addon!
     * @return the results
     */
    public Results getResults() {
        return results;
    }

    /**
     * @return death handicap value
     */
    public int getDeathHandicap() {
        return results.getDeathHandicap();
    }

    /**
     * Get the island's initial level. It may be zero if it was never calculated.
     * @return initial level of island as calculated when the island was created.
     */
    public long getInitialLevel() {
        return results.getInitialLevel();
    }

    /**
     * @return the level calculated
     */
    public long getLevel() {
        return results.getLevel();
    }


    /**
     * Overwrite the level. This level will be used instead of the calculated level.
     * @param level - the level to set
     */
    public void setLevel(long level) {
        results.setLevel(level);
    }

    /**
     * @return number of points required to next level
     */
    public long getPointsToNextLevel() {
        return results.getPointsToNextLevel();
    }

    /**
     * @return a human readable report explaining how the calculation was made
     */
    public List<String> getReport() {
        return results.getReport();
    }

    /**
     * @return the targetPlayer
     */
    public UUID getTargetPlayer() {
        return targetPlayer;
    }
    /**
     * Do not use this if you are not a BentoBox addon
     * @param results the results to set
     */
    public void setResults(Results results) {
        this.results = results;
    }

    /**
     * @param targetPlayer the targetPlayer to set
     */
    public void setTargetPlayer(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

}
