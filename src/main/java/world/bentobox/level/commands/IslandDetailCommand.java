package world.bentobox.level.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
        Island island = getIslands().getIsland(getWorld(), user);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;

        }
        DetailsPanel.openPanel(this.addon, getWorld(), user);
        return true;
    }
}
