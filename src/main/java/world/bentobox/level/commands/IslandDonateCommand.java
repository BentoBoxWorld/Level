package world.bentobox.level.commands;

import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.panels.DonationPanel;
import world.bentobox.level.util.Utils;

/**
 * Command: /island donate [hand [amount]]
 * Opens a donation GUI or donates blocks from hand.
 *
 * @author tastybento
 */
public class IslandDonateCommand extends ConfirmableCommand {

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

        // Handle "hand" subcommand (accepts English "hand" or the localized keyword)
        if (!args.isEmpty() && isHandKeyword(user, args.get(0))) {
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

        final Material material = hand.getType();
        final Integer blockValue = addon.getBlockConfig().getValue(getWorld(), material);
        if (blockValue == null || blockValue <= 0) {
            user.sendMessage("island.donate.no-value");
            return false;
        }

        int requested = hand.getAmount();
        if (args.size() > 1) {
            if ("help".equalsIgnoreCase(args.get(1))) {
                showHelp(this, user);
                return true;
            }
            try {
                requested = Integer.parseInt(args.get(1));
                if (requested < 1) {
                    user.sendMessage("island.donate.invalid-amount");
                    return false;
                }
            } catch (NumberFormatException e) {
                user.sendMessage("island.donate.invalid-amount");
                return false;
            }
        }

        final int previewAmount = Math.min(requested, hand.getAmount());
        final long previewPoints = (long) previewAmount * blockValue;
        final int finalRequested = requested;

        String prompt = user.getTranslation("island.donate.hand.confirm-prompt",
                TextVariables.NUMBER, String.valueOf(previewAmount),
                "[material]", Utils.prettifyObject(material, user),
                "[points]", Utils.formatNumber(user, previewPoints));

        askConfirmation(user, prompt, () -> performHandDonation(user, island, material, blockValue, finalRequested));
        return true;
    }

    private void performHandDonation(User user, Island island, Material material, int blockValue, int requested) {
        ItemStack currentHand = user.getPlayer().getInventory().getItemInMainHand();
        if (currentHand.getType() != material || currentHand.getAmount() == 0) {
            user.sendMessage("island.donate.hand.not-block");
            return;
        }
        int amount = Math.min(requested, currentHand.getAmount());
        long points = (long) amount * blockValue;

        if (amount >= currentHand.getAmount()) {
            user.getPlayer().getInventory().setItemInMainHand(null);
        } else {
            currentHand.setAmount(currentHand.getAmount() - amount);
        }

        addon.getManager().donateBlocks(island, user.getUniqueId(), material.name(), amount, points);
        addon.getManager().recalculateAfterDonation(island);

        user.sendMessage("island.donate.hand.success",
                TextVariables.NUMBER, String.valueOf(amount),
                "[material]", Utils.prettifyObject(material, user),
                "[points]", Utils.formatNumber(user, points));
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        String handKeyword = user.getTranslation("island.donate.hand.keyword");
        if (args.size() <= 1) {
            return Optional.of(Util.tabLimit(List.of(handKeyword), lastArg));
        }
        if (args.size() == 2 && isHandKeyword(user, args.get(0)) && user.isPlayer()) {
            int held = user.getPlayer().getInventory().getItemInMainHand().getAmount();
            if (held > 0) {
                return Optional.of(Util.tabLimit(List.of(String.valueOf(held)), lastArg));
            }
        }
        return Optional.of(List.of());
    }

    private boolean isHandKeyword(User user, String arg) {
        String localized = user.getTranslation("island.donate.hand.keyword");
        return "hand".equalsIgnoreCase(arg) || localized.equalsIgnoreCase(arg);
    }
}
