package world.bentobox.level.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;
import world.bentobox.level.panels.DetailsPanel;


public class IslandDetailCommand extends CompositeCommand {

    private final Level addon;

    public IslandDetailCommand(Level addon, CompositeCommand parent) {
        super(parent, "detail");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("island.detail");
        setDescription("island.detail.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> list) {
        if (getIslands().hasIsland(getWorld(), user)) {
            DetailsPanel.openPanel(this.addon, getWorld(), user);
        } else {
            user.sendMessage("general.errors.no-island");
        }
        return true;
    }
}
