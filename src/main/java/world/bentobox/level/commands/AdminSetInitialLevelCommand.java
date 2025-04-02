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
        long initialLevel = addon.getManager().getInitialCount(island);
        long lv = 0;
        if (args.get(1).startsWith("+")) {
            String change = args.get(1).substring(1);
            lv = initialLevel + Long.parseLong(change);
        } else if (args.get(1).startsWith("-")) {
            String change = args.get(1).substring(1);
            lv = initialLevel - Long.parseLong(change);
        } else {
            lv = Long.parseLong(args.get(1));
        }
        addon.getManager().setInitialIslandCount(island, lv); // TODO Enable level setting
        user.sendMessage("admin.level.sethandicap.changed", TextVariables.NUMBER, String.valueOf(initialLevel),
                "[new_number]", String.valueOf(lv));
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
        // Check if this is a add or remove
        if (args.get(1).startsWith("+") || args.get(1).startsWith("-")) {
            String change = args.get(1).substring(1);
            if (!Util.isInteger(change, true)) {
                user.sendMessage("admin.level.sethandicap.invalid-level");
                return false;
            }
            // Value is okay
        } else {
            // Check value
            if (!Util.isInteger(args.get(1), true)) {
                user.sendMessage("admin.level.sethandicap.invalid-level");
                return false;
            }
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
