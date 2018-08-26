package bentobox.addon.level.calculators;

import java.util.UUID;

import org.bukkit.World;

import bentobox.addon.level.Level;
import bentobox.addon.level.calculators.CalcIslandLevel.Results;
import bentobox.addon.level.event.IslandLevelCalculatedEvent;
import bentobox.addon.level.event.IslandPreLevelEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


/**
 * Gets the player's island level. For admin or players
 * @author tastybento
 *
 */
public class PlayerLevel {

    private Level addon;

    private Island island;
    private World world;
    private User asker;
    private UUID targetPlayer;

    private long oldLevel;

    private CalcIslandLevel calc;


    public PlayerLevel(final Level addon, final Island island, final UUID targetPlayer, final User asker) {
        this.addon = addon;
        this.island = island;
        this.world = island != null ? island.getCenter().getWorld() : null;
        this.asker = asker;
        this.targetPlayer = targetPlayer;
        this.oldLevel = addon.getIslandLevel(world, targetPlayer);

        // Fire pre-level calc event
        IslandPreLevelEvent e = new IslandPreLevelEvent(targetPlayer, island);
        addon.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            // Calculate if not cancelled
            calc = new CalcIslandLevel(addon, island, ()-> fireIslandLevelCalcEvent());
        }
    }


    private void fireIslandLevelCalcEvent() {
        // Fire post calculation event
        IslandLevelCalculatedEvent ilce = new IslandLevelCalculatedEvent(targetPlayer, island, calc.getResult());
        addon.getServer().getPluginManager().callEvent(ilce);
        Results results = ilce.getResults();
        // Save the results
        island.getMemberSet().forEach(m -> addon.setIslandLevel(world, m, results.getLevel()));
        // Display result if event is not cancelled
        if (!ilce.isCancelled()) {
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
        if (results.getPointsToNextLevel() >= 0) {
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
