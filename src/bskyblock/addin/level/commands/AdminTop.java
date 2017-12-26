package bskyblock.addin.level.commands;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import bskyblock.addin.level.Level;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class AdminTop extends CompositeCommand {
    
    private final Level levelPlugin;
    
    public AdminTop(Level levelPlugin, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.levelPlugin = levelPlugin;
        this.setPermission(Settings.PERMPREFIX + "admin.top");
        this.setOnlyPlayer(false);
        this.setUsage("admin.top.usage");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        int rank = 0;
        for (Entry<UUID, Long> topTen : levelPlugin.getTopTen().getTopTenList().getTopTen().entrySet()) {
            UUID player = topTen.getKey();
            rank++;
            String item = String.valueOf(rank) + ":" + BSkyBlock.getPlugin().getIslands().getIslandName(player) + " "
                    + "topten.islandLevel" +  String.valueOf(topTen.getValue());
            user.sendLegacyMessage(item);
        }

        return true;
    }

}
