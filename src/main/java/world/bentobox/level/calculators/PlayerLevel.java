package world.bentobox.level.calculators;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.addon.AddonEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.CalcIslandLevel.Results;
import world.bentobox.level.event.IslandLevelCalculatedEvent;
import world.bentobox.level.event.IslandPreLevelEvent;


/**
 * Gets the player's island level. For admin or players
 * @author tastybento
 *
 */
public class PlayerLevel {

    private final Level addon;

    private final Island island;
    private final World world;
    private final User asker;
    private final UUID targetPlayer;

    private final long oldLevel;

    private CalcIslandLevel calc;


    public PlayerLevel(final Level addon, final Island island, final UUID targetPlayer, @Nullable final User asker) {
        this.addon = addon;
        this.island = island;
        this.world = island.getCenter().getWorld();
        this.asker = asker;
        this.targetPlayer = targetPlayer;
        this.oldLevel = addon.getIslandLevel(world, targetPlayer);

        // Fire pre-level calc event
        IslandPreLevelEvent e = new IslandPreLevelEvent(targetPlayer, island);
        addon.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            // Calculate if not cancelled
            calc = new CalcIslandLevel(addon, island, this::fireIslandLevelCalcEvent);
        }
    }


    private void fireIslandLevelCalcEvent() {
        // Fire post calculation event
        IslandLevelCalculatedEvent ilce = new IslandLevelCalculatedEvent(targetPlayer, island, calc.getResult());
        addon.getServer().getPluginManager().callEvent(ilce);
        // This exposes these values to plugins via the event
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("eventName", "IslandLevelCalculatedEvent");
        keyValues.put("targetPlayer", targetPlayer);
        keyValues.put("islandUUID", island.getUniqueId());
        keyValues.put("level", calc.getResult().getLevel());
        keyValues.put("pointsToNextLevel", calc.getResult().getPointsToNextLevel());
        keyValues.put("deathHandicap", calc.getResult().getDeathHandicap());
        keyValues.put("initialLevel", calc.getResult().getInitialLevel());
        addon.getServer().getPluginManager().callEvent(new AddonEvent().builder().addon(addon).keyValues(keyValues).build());
        Results results = ilce.getResults();
        // Save the results
        island.getMemberSet().forEach(m -> addon.setIslandLevel(world, m, results.getLevel()));
        // Display result if event is not cancelled
        if (!ilce.isCancelled() && asker != null) {
            informPlayers(results);
        }
    }


    private void informPlayers(Results results) {
        // Tell the asker
        asker.sendMessage("island.level.island-level-is", "[level]", String.valueOf(addon.getIslandLevel(world, targetPlayer)));
        // Console
        if (!asker.isPlayer()) {
            results.getReport().forEach(asker::sendRawMessage);
            return;
        }
        // Player
        if (addon.getSettings().getDeathPenalty() != 0) {
            asker.sendMessage("island.level.deaths", "[number]", String.valueOf(results.getDeathHandicap()));
        }
        // Send player how many points are required to reach next island level
        if (results.getPointsToNextLevel() >= 0 && results.getPointsToNextLevel() < CalcIslandLevel.MAX_AMOUNT) {
            asker.sendMessage("island.level.required-points-to-next-level", "[points]", String.valueOf(results.getPointsToNextLevel()));
        }
        // Tell other team members
        if (addon.getIslandLevel(world, targetPlayer) != oldLevel) {
            island.getMemberSet().stream()
            .filter(u -> !u.equals(asker.getUniqueId()))
            .forEach(m -> User.getInstance(m).sendMessage("island.level.island-level-is", "[level]", String.valueOf(addon.getIslandLevel(world, targetPlayer))));
        }

    }


}
