package world.bentobox.level.commands;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.calculators.Results.Result;

public class IslandLevelCommand extends CompositeCommand {

    private static final String ISLAND_LEVEL_IS = "island.level.island-level-is";
    private static final String LEVEL = "[level]";
    private final Level addon;

    public IslandLevelCommand(Level addon, CompositeCommand parent) {
        super(parent, "level");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("island.level");
        this.setParametersHelp("island.level.parameters");
        this.setDescription("island.level.description");
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
                return false;
            }
            // Ops, console and admin perms can request and calculate other player levels
            if (!user.isPlayer() || user.isOp() || user.hasPermission(this.getPermissionPrefix() + "admin.level")) {
                return scanIsland(user, playerUUID);
            }
            // Request for another player's island level
            if (!user.getUniqueId().equals(playerUUID) ) {
                user.sendMessage(ISLAND_LEVEL_IS, LEVEL, addon.getManager().getIslandLevelString(getWorld(), playerUUID));
                return true;
            }
        }
        if (!user.isPlayer()) {
            user.sendMessage("general.errors.use-in-game");
            return false;
        }
        // Self request
        // Check player cooldown
        int coolDown = this.addon.getSettings().getLevelWait();

        if (coolDown > 0) {
            // Check cool down
            if (checkCooldown(user)) return false;
            // Set cool down
            setCooldown(user.getUniqueId(), coolDown);
        }

        // Self level request
        return scanIsland(user, user.getUniqueId());

    }


    private boolean scanIsland(User user, UUID playerUUID) {
        Island island = getIslands().getIsland(getWorld(), playerUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;

        }
        int inQueue = addon.getPipeliner().getIslandsInQueue();
        user.sendMessage("island.level.calculating");
        user.sendMessage("island.level.estimated-wait", TextVariables.NUMBER, String.valueOf(addon.getPipeliner().getTime() * (inQueue + 1)));
        if (inQueue > 1) {
            user.sendMessage("island.level.in-queue", TextVariables.NUMBER, String.valueOf(inQueue + 1));
        }
        // Get the old level
        long oldLevel = addon.getManager().getIslandLevel(getWorld(), playerUUID);
        addon.getManager().calculateLevel(playerUUID, island).thenAccept(results -> {
            if (results == null) return; // island was deleted or become unowned
            if (results.getState().equals(Result.IN_PROGRESS)) {
                user.sendMessage("island.level.in-progress");
                return;
            } else if (results.getState().equals(Result.TIMEOUT)) {
                user.sendMessage("island.level.time-out");
                return;
            }
            showResult(user, playerUUID, island, oldLevel, results);
        });
        return true;

    }

    private void showResult(User user, UUID playerUUID, Island island, long oldLevel, Results results) {
        if (user.isPlayer()) {
            user.sendMessage(ISLAND_LEVEL_IS, LEVEL, addon.getManager().getIslandLevelString(getWorld(), playerUUID));
            // Player
            if (addon.getSettings().getDeathPenalty() != 0) {
                user.sendMessage("island.level.deaths", "[number]", String.valueOf(results.getDeathHandicap()));
            }
            // Send player how many points are required to reach next island level
            if (results.getPointsToNextLevel() >= 0) {
                user.sendMessage("island.level.required-points-to-next-level", "[points]", String.valueOf(results.getPointsToNextLevel()));
            }
            // Tell other team members
            if (results.getLevel() != oldLevel) {
                island.getMemberSet().stream()
                .filter(u -> !u.equals(user.getUniqueId()))
                .forEach(m -> User.getInstance(m).sendMessage(ISLAND_LEVEL_IS, LEVEL, addon.getManager().getIslandLevelString(getWorld(), playerUUID)));
            }
        } else if (this.addon.getSettings().isLogReportToConsole()) {
            results.getReport().forEach(BentoBox.getInstance()::log);
        }

    }


}
