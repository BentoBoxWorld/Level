package world.bentobox.level.commands;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

public class IslandLevelCommand extends CompositeCommand {

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
                return true;
            }
            // Ops, console and admin perms can request and calculate other player levels
            if (!user.isPlayer() || user.isOp() || user.hasPermission(this.getPermissionPrefix() + "admin.level")) {
                return scanIsland(user, playerUUID);
            }
            // Request for another player's island level
            if (!user.getUniqueId().equals(playerUUID) ) {
                user.sendMessage("island.level.island-level-is", "[level]", addon.getManager().getIslandLevelString(getWorld(), playerUUID));
                return true;
            }
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
        if (island != null) {
            user.sendMessage("island.level.calculating");
            int inQueue = addon.getPipeliner().getIslandsInQueue();
            if (inQueue > 1) {
                user.sendMessage("island.level.in-queue", TextVariables.NUMBER, String.valueOf(inQueue + 1));
            }
            // Get the old level
            long oldLevel = addon.getManager().getIslandLevel(getWorld(), playerUUID);
            addon.getManager().calculateLevel(playerUUID, island).thenAccept(results -> {
                if (results == null) return; // island was deleted or become unowned
                if (user.isPlayer()) {
                    user.sendMessage("island.level.island-level-is", "[level]", addon.getManager().getIslandLevelString(getWorld(), playerUUID));
                    // Player
                    if (addon.getSettings().getDeathPenalty() != 0) {
                        user.sendMessage("island.level.deaths", "[number]", String.valueOf(results.getDeathHandicap()));
                    }
                    // Send player how many points are required to reach next island level
                    if (results.getPointsToNextLevel() >= 0 && results.getPointsToNextLevel() < 10000) {
                        user.sendMessage("island.level.required-points-to-next-level", "[points]", String.valueOf(results.getPointsToNextLevel()));
                    }
                    // Tell other team members
                    if (results.getLevel() != oldLevel) {
                        island.getMemberSet().stream()
                        .filter(u -> !u.equals(user.getUniqueId()))
                        .forEach(m -> User.getInstance(m).sendMessage("island.level.island-level-is", "[level]", addon.getManager().getIslandLevelString(getWorld(), playerUUID)));
                    }
                } else {
                    results.getReport().forEach(BentoBox.getInstance()::log);
                }
            });
            return true;
        } else {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

    }


}
