package world.bentobox.level.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

public class AdminTopCommand extends CompositeCommand {

    private final Level levelPlugin;

    public AdminTopCommand(Level addon, CompositeCommand parent) {
        super(parent, "top", "topten");
        this.levelPlugin = addon;
        new AdminTopRemoveCommand(addon, this);
    }

    @Override
    public void setup() {
        this.setPermission("admin.top");
        this.setOnlyPlayer(false);
        this.setDescription("admin.top.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("island.top.gui-title");
        int rank = 0;
        for (Map.Entry<UUID, Long> topTen : levelPlugin.getManager().getTopTen(getWorld(), Level.TEN).entrySet()) {
            Island island = getPlugin().getIslands().getIsland(getWorld(), topTen.getKey());
            if (island != null) {
                rank++;
                user.sendMessage("admin.top.display",
                        "[rank]",
                        String.valueOf(rank),
                        "[name]",
                        this.getPlugin().getPlayers().getUser(island.getOwner()).getName(),
                        "[level]",
                        String.valueOf(topTen.getValue()));
            }
        }

        return true;
    }
}
