package bskyblock.addon.level.commands;

import java.util.List;
import java.util.Map.Entry;

import bskyblock.addon.level.Level;

import java.util.UUID;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class AdminTop extends CompositeCommand {
    
    private final Level levelPlugin;
    
    public AdminTop(Level levelPlugin, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.levelPlugin = levelPlugin;
    }

    @Override
    public boolean execute(User user, List<String> args) {
        int rank = 0;
        for (Entry<UUID, Long> topTen : levelPlugin.getTopTen().getTopTenList().getTopTen().entrySet()) {
            UUID player = topTen.getKey();
            rank++;
            String item = String.valueOf(rank) + ":" + BSkyBlock.getInstance().getIslands().getIslandName(player) + " "
                    + "topten.islandLevel" +  String.valueOf(topTen.getValue());
            user.sendLegacyMessage(item);
        }

        return true;
    }

    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "admin.top");
        this.setOnlyPlayer(false);
        this.setParameters("admin.top.usage");
    }

}
