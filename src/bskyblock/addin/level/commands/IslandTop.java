package bskyblock.addin.level.commands;

import java.util.List;

import bskyblock.addin.level.Level;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

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
        this.setPermission(Settings.PERMPREFIX + "island.top");
        this.setDescription("island.top.description");

        
    }

}
