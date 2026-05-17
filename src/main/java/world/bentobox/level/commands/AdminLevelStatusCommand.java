package world.bentobox.level.commands;

import java.util.List;
import java.util.Map;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.IslandLevelCalculator;

public class AdminLevelStatusCommand extends CompositeCommand {

    private final Level addon;

    public AdminLevelStatusCommand(Level addon, CompositeCommand parent) {
        super(parent, "levelstatus");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("admin.levelstatus");
        this.setOnlyPlayer(false);
        this.setDescription("admin.levelstatus.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        int total = addon.getPipeliner().getIslandsInQueue();
        user.sendMessage("admin.levelstatus.islands-in-queue", TextVariables.NUMBER, String.valueOf(total));
        if (total == 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Map<IslandLevelCalculator, Long> inProcess = addon.getPipeliner().getInProcessQueue();
        inProcess.forEach((calc, started) -> user.sendMessage(buildDetailKey(calc),
                "[world]", worldName(calc),
                "[xyz]", xyz(calc),
                "[type]", typeKey(user, calc),
                "[elapsed]", formatElapsed(now - started),
                "[scanned]", String.valueOf(calc.getScannedChunks()),
                "[total]", String.valueOf(calc.getTotalChunksToScan())));
        for (IslandLevelCalculator calc : addon.getPipeliner().getToProcessQueue()) {
            user.sendMessage("admin.levelstatus.island-queued",
                    "[world]", worldName(calc),
                    "[xyz]", xyz(calc),
                    "[type]", typeKey(user, calc));
        }
        return true;
    }

    private String buildDetailKey(IslandLevelCalculator calc) {
        return "admin.levelstatus.island-detail";
    }

    private String worldName(IslandLevelCalculator calc) {
        Island island = calc.getIsland();
        return island.getWorld() == null ? "?" : island.getWorld().getName();
    }

    private String xyz(IslandLevelCalculator calc) {
        Island island = calc.getIsland();
        if (island.getCenter() == null) {
            return "?";
        }
        return Util.xyz(island.getCenter().toVector());
    }

    private String typeKey(User user, IslandLevelCalculator calc) {
        return user.getTranslation(calc.isZeroIsland()
                ? "admin.levelstatus.type-zero" : "admin.levelstatus.type-regular");
    }

    private String formatElapsed(long ms) {
        long s = Math.max(0, ms / 1000);
        long m = s / 60;
        s = s % 60;
        return m > 0 ? (m + "m" + s + "s") : (s + "s");
    }
}
