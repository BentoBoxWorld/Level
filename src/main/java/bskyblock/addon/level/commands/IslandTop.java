package bskyblock.addon.level.commands;

import java.util.List;

import bskyblock.addon.level.Level;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class IslandTop extends CompositeCommand {

    private final Level plugin;

    public IslandTop(Level plugin, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(User user, String label, List<String> list) {
        plugin.getTopTen().getGUI(getWorld(), user, getPermissionPrefix());
        return true;
    }

    @Override
    public void setup() {
        this.setPermission("island.top");
        this.setDescription("island.top.description");


    }

}
