package bskyblock.addon.level.commands;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import bskyblock.addon.level.Level;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

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
                    + user.getTranslation("topten.islandLevel", "[level]", String.valueOf(topTen.getValue()));
            user.sendRawMessage(item);
        }

        return true;
    }

    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "admin.top");
        this.setOnlyPlayer(false);
        this.setDescription("admin.top.description");
    }

}
