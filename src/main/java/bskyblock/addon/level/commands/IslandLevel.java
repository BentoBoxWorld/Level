package bskyblock.addon.level.commands;

import java.util.List;
import java.util.UUID;

import bskyblock.addon.level.Level;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class IslandLevel extends CompositeCommand {
    
    private final Level levelPlugin;
    
    public IslandLevel(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!args.isEmpty()) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            //getLogger().info("DEBUG: console player info UUID = " + playerUUID);
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return true;
            } else if (user.getUniqueId().equals(playerUUID) ) {
                // Self level request
                levelPlugin.calculateIslandLevel(getWorld(), user, user.getUniqueId(), false, this.getPermissionPrefix());
            } else {
                user.sendMessage("island.level.island-level-is", "[level]", String.valueOf(levelPlugin.getIslandLevel(getWorld(), playerUUID)));
                return true;
            }
        } else {
            // Self level request
            levelPlugin.calculateIslandLevel(getWorld(), user, user.getUniqueId(), false, this.getPermissionPrefix());
        }
        return false;
    }

    @Override
    public void setup() {
        this.setPermission("island.level");
        this.setParameters("island.level.parameters");
        this.setDescription("island.level.description");
        this.setOnlyPlayer(true);  
    }

}
