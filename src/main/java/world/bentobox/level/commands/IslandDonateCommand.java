package world.bentobox.level.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.panels.DonationPanel;
import world.bentobox.level.util.Utils;

/**
 * Command: /island donate [hand [amount]]
 * Opens a donation GUI or donates blocks from hand.
 *
 * @author tastybento
 */
public class IslandDonateCommand extends CompositeCommand {

    private final Level addon;

    public IslandDonateCommand(Level addon, CompositeCommand parent) {
        super(parent, "donate");
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("island.donate");
        this.setOnlyPlayer(true);
        this.setParametersHelp("island.donate.parameters");
        this.setDescription("island.donate.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Check the player is on an island they are part of
        Island island = addon.getIslands().getIsland(getWorld(), user);
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // Check the player is on their island
        if (!island.onIsland(user.getLocation())) {
            user.sendMessage("island.donate.must-be-on-island");
            return false;
        }

        // Check flag permission
        if (!island.isAllowed(user, Level.BLOCK_DONATION)) {
            user.sendMessage("island.donate.no-permission");
            return false;
        }

        // Handle "hand" subcommand
        if (!args.isEmpty() && "hand".equalsIgnoreCase(args.get(0))) {
            return handleHandDonation(user, island, args);
        }

        // No args - open GUI
        DonationPanel.openPanel(addon, getWorld(), user, island);
        return true;
    }

    /**
     * Handle the /island donate hand [amount] subcommand.
     */
    private boolean handleHandDonation(User user, Island island, List<String> args) {
        ItemStack hand = user.getPlayer().getInventory().getItemInMainHand();
        if (hand.getType().isAir() || !hand.getType().isBlock()) {
            user.sendMessage("island.donate.hand.not-block");
            return false;
        }

        Material material = hand.getType();
        Integer blockValue = addon.getBlockConfig().getValue(getWorld(), material);
        if (blockValue == null || blockValue <= 0) {
            user.sendMessage("island.donate.no-value");
            return false;
        }

        // Determine amount
        int amount = hand.getAmount();
        if (args.size() > 1) {
            try {
                amount = Integer.parseInt(args.get(1));
                if (amount < 1) {
                    user.sendMessage("island.donate.invalid-amount");
                    return false;
                }
                amount = Math.min(amount, hand.getAmount());
            } catch (NumberFormatException e) {
                user.sendMessage("island.donate.invalid-amount");
                return false;
            }
        }

        // Calculate points
        long points = (long) amount * blockValue;

        // Remove items from hand
        if (amount >= hand.getAmount()) {
            user.getPlayer().getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - amount);
        }

        // Record the donation
        addon.getManager().donateBlocks(island, user.getUniqueId(), material.name(), amount, points);

        // Notify the player
        user.sendMessage("island.donate.hand.success",
                TextVariables.NUMBER, String.valueOf(amount),
                "[material]", Utils.prettifyObject(material, user),
                "[points]", String.valueOf(points));

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 1) {
            return Optional.of(List.of("hand"));
        }
        return Optional.of(new ArrayList<>());
    }
}
