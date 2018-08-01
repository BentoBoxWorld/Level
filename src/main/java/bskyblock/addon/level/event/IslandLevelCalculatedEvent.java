package bskyblock.addon.level.event;

import java.util.UUID;

import bskyblock.addon.level.calculators.CalcIslandLevel.Results;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.database.objects.Island;

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
     * @param targetPlayer
     * @param island
     * @param results
     */
    public IslandLevelCalculatedEvent(UUID targetPlayer, Island island, Results results) {
        super(island);
        this.targetPlayer = targetPlayer;
        this.results = results;
    }

    /**
     * @return the results
     */
    public Results getResults() {
        return results;
    }

    /**
     * @return the targetPlayer
     */
    public UUID getTargetPlayer() {
        return targetPlayer;
    }
    /**
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
