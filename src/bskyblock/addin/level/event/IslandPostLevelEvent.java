package bskyblock.addin.level.event;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * This event is fired after ASkyBlock calculates an island level and when it sends notification to the player.
 * Use getLevel() to see the level calculated and getPointsToNextLevel() to see how much points are needed to reach next level.
 * Canceling this event will result in no notifications to the player.
 * 
 * @author Poslovitch, tastybento
 */
public class IslandPostLevelEvent extends IslandBaseEvent {
    private long level;
    private long pointsToNextLevel;

    /**
     * @param player
     * @param island
     * @param l
     */
    public IslandPostLevelEvent(UUID player, Island island, long l, long m) {
        super(island);
        this.level = l;
        this.pointsToNextLevel = m;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

    public long getPointsToNextLevel() {
        return pointsToNextLevel;
    }

    public void setPointsToNextLevel(long pointsToNextLevel) {
        this.pointsToNextLevel = pointsToNextLevel;
    }


}
