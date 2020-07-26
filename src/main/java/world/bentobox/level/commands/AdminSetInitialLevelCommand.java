package world.bentobox.level.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class AdminSetInitialLevelCommand extends CompositeCommand {

    private @Nullable UUID targetUUID;
    private @Nullable Island island;
    private Level addon;

    public AdminSetInitialLevelCommand(Level addon, CompositeCommand parent) {
        super(parent, "sethandicap");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("admin.level.sethandicap");
        this.setOnlyPlayer(false);
        this.setParametersHelp("admin.level.sethandicap.parameters");
        this.setDescription("admin.level.sethandicap.description");
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        String initialLevel = String.valueOf(addon.getManager().getInitialLevel(island));
        long lv = Long.valueOf(args.get(1));
        addon.getManager().setInitialIslandLevel(island, lv);
        user.sendMessage("admin.level.sethandicap.changed", TextVariables.NUMBER, initialLevel, "[new_number]", String.valueOf(lv));
        return true;
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }
        targetUUID = getAddon().getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check value
        if (!Util.isInteger(args.get(1), true)) {
            user.sendMessage("admin.level.sethandicap.invalid-level");
            return false;
        }
        // Check island
        island = getAddon().getIslands().getIsland(getWorld(), targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        return true;
    }
}
