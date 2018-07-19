package bskyblock.addon.level.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;

import bskyblock.addon.level.Level;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class AdminLevel extends CompositeCommand {

    private final Level levelPlugin;

    public AdminLevel(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 2) {
            // Get world
            World world = null;
            if (getPlugin().getIWM().isOverWorld(args.get(0))) {
                world = getPlugin().getIWM().getIslandWorld(args.get(0));
            } else {
                user.sendMessage("commands.admin.top.unknown-world");
                return false;
            }
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            //getLogger().info("DEBUG: console player info UUID = " + playerUUID);
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return true;
            } else {
                if (user.isPlayer()) {
                    levelPlugin.calculateIslandLevel(world, user, playerUUID, false, getPermissionPrefix());
                } else {
                    levelPlugin.calculateIslandLevel(world, user, playerUUID, true, getPermissionPrefix());
                }
            }
            return true;
        } else {
            showHelp(this, user);
            return false;
        }
    }

    @Override
    public void setup() {
        this.setPermission("admin.level");
        this.setOnlyPlayer(false);
        this.setParameters("admin.level.parameters");
        this.setDescription("admin.level.description");

    }

}
