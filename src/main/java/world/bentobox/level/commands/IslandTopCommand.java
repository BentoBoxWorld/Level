package world.bentobox.level.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

public class IslandTopCommand extends CompositeCommand {

    private final Level addon;

    public IslandTopCommand(Level addon, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("island.top");
        setDescription("island.top.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> list) {
        addon.getManager().getGUI(getWorld(), user);
        return true;
    }
}
