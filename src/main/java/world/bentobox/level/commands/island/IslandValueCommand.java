package world.bentobox.level.commands.island;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

import java.util.List;

public class IslandValueCommand extends CompositeCommand {
    private final Level plugin;

    public IslandValueCommand(Level plugin, CompositeCommand parent) {
        super(parent, "value");
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        this.setPermission("island.value");
        this.setDescription("island.value.description");
        this.setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Player player = user.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (!inventory.getItemInMainHand().getType().equals(Material.AIR)) {
            Material material = inventory.getItemInMainHand().getType();
            if (plugin.getConfig().get("blocks." + material.toString()) != null) {
                int value = plugin.getConfig().getInt("blocks." + material.toString());
                user.sendMessage("island.value.success", "[value]", value + "");
                if (plugin.getConfig().get("underwater") != null) {
                    Double underWater = plugin.getConfig().getDouble("underwater");
                    if (underWater > 1.0) {
                        user.sendMessage("island.value.success-underwater", "[value]", (underWater * value) + "");
                    }
                }
            } else {
                user.sendMessage("island.value.success", "[value]",  "0");
            }
        } else {
            user.sendMessage("island.value.empty-hand");
        }
        return true;
    }
}
