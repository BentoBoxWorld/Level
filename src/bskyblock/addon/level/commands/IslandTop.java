package bskyblock.addon.level.commands;

import java.util.List;

import bskyblock.addon.level.Level;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

public class IslandTop extends CompositeCommand {

    private final Level plugin;

    public IslandTop(Level plugin, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(User user, List<String> list) {
        plugin.getTopTen().getGUI(user.getPlayer());
        return false;
    }

    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.top");
        this.setDescription("island.top.description");

        
    }

}
