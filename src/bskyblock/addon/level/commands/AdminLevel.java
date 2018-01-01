package bskyblock.addon.level.commands;

import java.util.List;
import java.util.UUID;

import bskyblock.addon.level.Level;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class AdminLevel extends CompositeCommand {
    
    private final Level levelPlugin;
    
    public AdminLevel(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
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
           } else {
               if (user.isPlayer()) {
                   levelPlugin.calculateIslandLevel(user, playerUUID, false); 
               } else {
                   levelPlugin.calculateIslandLevel(user, playerUUID, true);
               }
           }
       }
        return true;
    }

    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "admin.level");
        this.setOnlyPlayer(false);
        this.setParameters("admin.level.parameters");
        this.setDescription("admin.level.description");
        
    }

}
