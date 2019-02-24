package world.bentobox.level.commands.island;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

public class IslandLevelCommand extends CompositeCommand {

    private final Level levelPlugin;

    public IslandLevelCommand(Level levelPlugin, CompositeCommand parent) {
        super(parent, "level");
        this.levelPlugin = levelPlugin;
    }

    @Override
    public void setup() {
        this.setPermission("island.level");
        this.setParametersHelp("island.level.parameters");
        this.setDescription("island.level.description");
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Asking for another player's level?
            // Convert name to a UUID
            final UUID playerUUID = getPlugin().getPlayers().getUUID(args.get(0));
            if (playerUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return true;
            } else if (user.getUniqueId().equals(playerUUID) ) {
                return this.calculateLevel(user);
            } else {
                user.sendMessage("island.level.island-level-is", "[level]", String.valueOf(levelPlugin.getIslandLevel(getWorld(), playerUUID)));
                return true;
            }
        } else {
            return this.calculateLevel(user);
        }
    }


    /**
     * This method calls island level calculation if it is allowed by cooldown.
     * @param user User which island level must be calculated.
     * @return True if le
     */
    private boolean calculateLevel(User user)
    {
        int coolDown = this.levelPlugin.getSettings().getLevelWait();

        if (coolDown > 0 && this.checkCooldown(user, null))
        {
            return false;
        }

        // Self level request
        this.levelPlugin.calculateIslandLevel(getWorld(), user, user.getUniqueId());

        if (coolDown > 0)
        {
            this.setCooldown(user.getUniqueId(), null, coolDown);
        }

        return true;
    }
}
