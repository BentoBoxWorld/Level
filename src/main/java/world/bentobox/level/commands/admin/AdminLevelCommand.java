package world.bentobox.level.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class AdminLevelCommand extends CompositeCommand {

    private final Level addon;

    public AdminLevelCommand(Level addon, CompositeCommand parent) {
        super(parent, "level");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("admin.level");
        this.setOnlyPlayer(false);
        this.setParametersHelp("admin.level.parameters");
        this.setDescription("admin.level.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
                return true;
            } else {
                addon.calculateIslandLevel(getWorld(), user, playerUUID);
            }
            return true;
        } else {
            showHelp(this, user);
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
