package bentobox.addon.level.commands;

import java.util.List;
import java.util.UUID;

import bentobox.addon.level.Level;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;

public class AdminLevel extends CompositeCommand {

    private final Level levelPlugin;

    public AdminLevel(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() == 1) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            //getLogger().info("DEBUG: console player info UUID = " + playerUUID);
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return true;
            } else {
                levelPlugin.calculateIslandLevel(getWorld(), user, playerUUID);
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
        this.setParametersHelp("admin.level.parameters");
        this.setDescription("admin.level.description");

    }

}
