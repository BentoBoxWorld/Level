package world.bentobox.level.commands.island;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

import java.util.List;

public class IslandTopCommand extends CompositeCommand {

    private final Level plugin;

    public IslandTopCommand(Level plugin, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        setPermission("island.top");
        setDescription("island.top.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> list) {
        plugin.getTopTen().getGUI(getWorld(), user, getPermissionPrefix());
        return true;
    }
}
