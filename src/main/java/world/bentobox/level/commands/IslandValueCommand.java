package world.bentobox.level.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.panels.ValuePanel;
import world.bentobox.level.util.Utils;


public class IslandValueCommand extends CompositeCommand
{
    private static final String MATERIAL = "[material]";
    private final Level addon;


    public IslandValueCommand(Level addon, CompositeCommand parent)
    {
        super(parent, "value");
        this.addon = addon;
    }


    @Override
    public void setup()
    {
        this.setPermission("island.value");
        this.setParametersHelp("level.commands.value.parameters");
        this.setDescription("level.commands.value.description");
        this.setOnlyPlayer(true);
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        if (args.size() > 1)
        {
            this.showHelp(this, user);
            return false;
        }

        if (args.isEmpty())
        {
            ValuePanel.openPanel(this.addon, this.getWorld(), user);
        }
        else if (args.get(0).equalsIgnoreCase("HAND"))
        {
            Player player = user.getPlayer();
            PlayerInventory inventory = player.getInventory();

            if (!inventory.getItemInMainHand().getType().equals(Material.AIR))
            {
                this.printValue(user, inventory.getItemInMainHand().getType());
            }
            else
            {
                Utils.sendMessage(user, user.getTranslation("level.conversations.empty-hand"));
            }
        }
        else
        {
            Material material = Material.matchMaterial(args.get(0));

            if (material == null)
            {
                Utils.sendMessage(user,
                        user.getTranslation(this.getWorld(), "level.conversations.unknown-item",
                                MATERIAL, args.get(0)));
            }
            else
            {
                this.printValue(user, material);
            }
        }

        return true;
    }


    /**
     * This method prints value of the given material in chat.
     * @param user User who receives the message.
     * @param material Material value.
     */
    private void printValue(User user, Material material)
    {
        Integer value = this.addon.getBlockConfig().getValue(getWorld(), material);

        if (value != null)
        {
            Utils.sendMessage(user,
                    user.getTranslation(this.getWorld(), "level.conversations.value",
                            "[value]", String.valueOf(value),
                            MATERIAL, Utils.prettifyObject(material, user)));

            double underWater = this.addon.getSettings().getUnderWaterMultiplier();

            if (underWater > 1.0)
            {
                Utils.sendMessage(user,
                        user.getTranslation(this.getWorld(),"level.conversations.success-underwater",
                                "[value]", (underWater * value) + ""),
                        MATERIAL, Utils.prettifyObject(material, user));
            }
        }
        else
        {
            Utils.sendMessage(user,
                    user.getTranslation(this.getWorld(),"level.conversations.no-value"));
        }
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args)
    {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";

        if (args.isEmpty())
        {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }

        List<String> options = new ArrayList<>(Arrays.stream(Material.values()).
                filter(Material::isBlock).
                map(Material::name).toList());

        options.add("HAND");

        return Optional.of(Util.tabLimit(options, lastArg));
    }
}