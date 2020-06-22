package world.bentobox.level.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

public class AdminLevelStatusCommand extends CompositeCommand {

    private final Level addon;

    public AdminLevelStatusCommand(Level addon, CompositeCommand parent) {
        super(parent, "levelstatus");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("admin.levelstatus");
        this.setOnlyPlayer(false);
        this.setDescription("admin.levelstatus.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendRawMessage("Islands in queue: " + addon.getPipeliner().getIslandsInQueue());
        return true;
    }
}
