package bskyblock.addon.level.event;

import java.util.UUID;

import world.bentobox.bbox.api.events.IslandBaseEvent;
import world.bentobox.bbox.database.objects.Island;

/**
 * Called when an island level is going to be calculated
 * @author tastybento
 *
 */
public class IslandPreLevelEvent extends IslandBaseEvent {

    private UUID targetPlayer;


    /**
     * Called when an island level is going to be calculated
     * @param targetPlayer - the player who is being tagetted (owner or team member)
     * @param island - the island
     */
    public IslandPreLevelEvent(UUID targetPlayer, Island island) {
        super(island);
        this.targetPlayer = targetPlayer;
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }

}
