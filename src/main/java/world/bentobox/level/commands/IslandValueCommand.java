package world.bentobox.level.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.hooks.OraxenHook;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.objects.IslandLevels;
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
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            showHelp(this, user);
            return false;
        }

        if (args.isEmpty()) {
            ValuePanel.openPanel(addon, getWorld(), user);
            return true;
        }

        String arg = args.get(0);
        if ("HAND".equalsIgnoreCase(arg)) {
            executeHandCommand(user);
            return true;
        }

        executeMaterialCommand(user, arg);
        return true;
    }

    private void executeHandCommand(User user) {
        Player player = user.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHandItem = inventory.getItemInMainHand();

        if (mainHandItem.getType() == Material.AIR) {
            Utils.sendMessage(user, user.getTranslation("level.conversations.empty-hand"));
            return;
        }

        // Oraxen
        if (BentoBox.getInstance().getHooks().getHook("Oraxen").isPresent()) {
            String id = OraxenHook.getIdByItem(mainHandItem);
            if (id != null) {
                printValue(user, "oraxen:" + id);
                return;
            }
        }
        // ItemsAdder
        if (addon.isItemsAdder()) {
            Optional<String> id = ItemsAdderHook.getNamespacedId(mainHandItem);
            if (id.isPresent()) {
                printValue(user, id.get());
                return;
            }
        }

        printValue(user, mainHandItem.getType());
    }

    private void executeMaterialCommand(User user, String arg) {
        Material material = Material.matchMaterial(arg);
        if (material == null) {
            Utils.sendMessage(user, user.getTranslation(getWorld(), "level.conversations.unknown-item", MATERIAL, arg));
        } else {
            printValue(user, material);
        }
    }

    /**
     * This method prints value of the given material in chat.
     * @param user User who receives the message.
     * @param material Material value.
     */
    private void printValue(User user, Object material)
    {
        Integer value = this.addon.getBlockConfig().getValue(getWorld(), material);

        if (value != null)
        {
            Utils.sendMessage(user, user.getTranslation(this.getWorld(), "level.conversations.value", "[value]",
                    String.valueOf(value), MATERIAL, Utils.prettifyObject(material, user)));

            double underWater = this.addon.getSettings().getUnderWaterMultiplier();

            if (underWater > 1.0) {
                Utils.sendMessage(user, user.getTranslation(this.getWorld(), "level.conversations.success-underwater",
                        "[value]", (underWater * value) + ""), MATERIAL, Utils.prettifyObject(material, user));
            }

            // Show how many have been placed and how many are allowed
            @NonNull
            IslandLevels lvData = this.addon.getManager()
                    .getLevelsData(getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()));
            int count = lvData.getMdCount().getOrDefault(material, 0) + lvData.getUwCount().getOrDefault(material, 0);
            user.sendMessage("level.conversations.you-have", TextVariables.NUMBER, String.valueOf(count));
            Integer limit = this.addon.getBlockConfig().getLimit(material);
            if (limit != null) {
                user.sendMessage("level.conversations.you-can-place", TextVariables.NUMBER, String.valueOf(limit));
            }
        }
        else {
            Utils.sendMessage(user, user.getTranslation(this.getWorld(), "level.conversations.no-value"));
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";

        if (args.isEmpty())
        {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }

        List<String> options = new ArrayList<>(
                Arrays.stream(Material.values()).filter(Material::isBlock).map(Material::name).map(String::toLowerCase).toList());

        options.add("HAND");

        return Optional.of(Util.tabLimit(options, lastArg));
    }
}