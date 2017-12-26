package bskyblock.addin.level.commands;

import java.util.List;
import java.util.UUID;

import bskyblock.addin.level.Level;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class IslandLevel extends CompositeCommand {
    
    private final Level levelPlugin;
    
    public IslandLevel(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
        this.setPermission(Settings.PERMPREFIX + "island.level");
        this.setUsage("island.level.usage");
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!args.isEmpty()) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0), true);
            //getLogger().info("DEBUG: console player info UUID = " + playerUUID);
            if (playerUUID == null) {
                user.sendMessage("error.UnknownPlayer");
                return true;
            } else if (user.getUniqueId().equals(playerUUID) ) {
                // Self level request
                levelPlugin.calculateIslandLevel(user, user.getUniqueId(), false);
            } else {
                user.sendMessage("addon.level.level-is", "[level]", String.valueOf(levelPlugin.getIslandLevel(playerUUID)));
                user.sendLegacyMessage("Level = " + String.valueOf(levelPlugin.getIslandLevel(playerUUID)));
                return true;
            }
        } else {
            // Self level request
            levelPlugin.calculateIslandLevel(user, user.getUniqueId(), false);
        }
        return false;
    }

}
