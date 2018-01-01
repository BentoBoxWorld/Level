package bskyblock.addon.level.event;

import java.util.UUID;

import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandPreLevelEvent extends IslandBaseEvent {
    
    private UUID targetPlayer;
    private long level;
    private long pointsToNextLevel;
    

    public IslandPreLevelEvent(UUID targetPlayer, Island island, long level) {
        super(island);
        this.targetPlayer = targetPlayer;
        this.level = level;
    }

    public long getPointsToNextLevel() {
        return pointsToNextLevel;
    }

    public void setPointsToNextLevel(long pointsToNextLevel) {
        this.pointsToNextLevel = pointsToNextLevel;
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public long getLevel() {
        return level;
    }

    public void setLevel(long level) {
        this.level = level;
    }

}
