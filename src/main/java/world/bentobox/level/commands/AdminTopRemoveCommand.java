package world.bentobox.level.commands;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * Removes a player from the top ten
 * @author tastybento
 *
 */
public class AdminTopRemoveCommand extends CompositeCommand {

    private final Level addon;
    private User target;

    public AdminTopRemoveCommand(Level addon, CompositeCommand parent) {
        super(parent, "remove", "delete");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("admin.top.remove");
        this.setOnlyPlayer(false);
        this.setParametersHelp("admin.top.remove.parameters");
        this.setDescription("admin.top.remove.description");
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            this.showHelp(this, user);
            return false;
        }
        target = getPlayers().getUser(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        addon.getManager().removeEntry(getWorld(), target.getUniqueId());
        user.sendMessage("general.success");
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(addon.getManager().getTopTen(getWorld(), Level.TEN).keySet().stream().map(addon.getPlayers()::getName)
                .filter(n -> !n.isEmpty()).collect(Collectors.toList()));
    }
}
