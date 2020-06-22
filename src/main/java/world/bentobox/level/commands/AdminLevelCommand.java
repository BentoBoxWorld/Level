package world.bentobox.level.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class AdminLevelCommand extends IslandLevelCommand {

    public AdminLevelCommand(Level addon, CompositeCommand parent) {
        super(addon, parent);
    }

    @Override
    public void setup() {
        this.setPermission("admin.level");
        this.setOnlyPlayer(false);
        this.setParametersHelp("admin.level.parameters");
        this.setDescription("admin.level.description");
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
