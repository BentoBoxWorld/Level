package world.bentobox.level.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
 * Command: /island donate [hand [amount]] [inv]
 * Opens a donation GUI, or donates blocks from the player's hand, or
 * donates every donatable block from the player's inventory.
 *
 * @author tastybento
 */
public class IslandDonateCommand extends ConfirmableCommand {

    private static final String MATERIAL_PLACEHOLDER = "[material]";
    private static final String POINTS_PLACEHOLDER = "[points]";

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

        // Handle "inv" subcommand (accepts English "inv" or the localized keyword)
        if (!args.isEmpty() && isInvKeyword(user, args.get(0))) {
            return handleInvDonation(user, island);
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

        // Check for a custom block (Oraxen, Nexo, ItemsAdder) first
        final String customId = addon.getCustomBlockId(hand);

        // If not a custom block, require a vanilla block
        if (customId == null && (hand.getType().isAir() || !hand.getType().isBlock())) {
            user.sendMessage("island.donate.hand.not-block");
            return false;
        }

        final Material material = hand.getType();
        final Integer blockValue = customId != null
                ? addon.getBlockConfig().getValue(getWorld(), customId)
                : addon.getBlockConfig().getValue(getWorld(), material);
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

        Object displayKey = customId != null ? customId : material;
        String prompt = user.getTranslation("island.donate.hand.confirm-prompt",
                TextVariables.NUMBER, String.valueOf(previewAmount),
                MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user),
                POINTS_PLACEHOLDER, Utils.formatNumber(user, previewPoints));

        askConfirmation(user, prompt, () -> performHandDonation(user, island, material, customId, blockValue, finalRequested));
        return true;
    }

    private void performHandDonation(User user, Island island, Material material, String customId, int blockValue, int requested) {
        ItemStack currentHand = user.getPlayer().getInventory().getItemInMainHand();
        // Verify the item in hand is still the same
        if (customId != null) {
            if (!customId.equals(addon.getCustomBlockId(currentHand)) || currentHand.getAmount() == 0) {
                user.sendMessage("island.donate.hand.not-block");
                return;
            }
        } else if (currentHand.getType() != material || currentHand.getAmount() == 0) {
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

        String donationId = customId != null ? customId : material.name();
        addon.getManager().donateBlocks(island, user.getUniqueId(), donationId, amount, points);
        addon.getManager().recalculateAfterDonation(island);

        Object displayKey = customId != null ? customId : material;
        user.sendMessage("island.donate.hand.success",
                TextVariables.NUMBER, String.valueOf(amount),
                MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user),
                POINTS_PLACEHOLDER, Utils.formatNumber(user, points));
    }

    /**
     * Handle the /island donate inv subcommand. Scans the player's inventory for
     * blocks with a positive donation value, shows a per-material breakdown plus
     * the total, and asks for confirmation. Items with no value or that aren't
     * donatable blocks remain in the inventory.
     */
    private boolean handleInvDonation(User user, Island island) {
        Map<String, Integer> totals = collectDonatableTotals(user.getPlayer().getInventory());

        if (totals.isEmpty()) {
            user.sendMessage("island.donate.empty");
            return false;
        }

        long totalPoints = 0L;
        StringBuilder prompt = new StringBuilder(
                user.getTranslation("island.donate.inv.confirm-header"));
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            // Resolve back to Material when possible for a nicer display label
            Object displayKey;
            Material mat = Material.matchMaterial(e.getKey());
            displayKey = mat != null ? mat : e.getKey();
            int value = addon.getBlockConfig().getValue(getWorld(), displayKey);
            long points = (long) value * e.getValue();
            totalPoints += points;
            prompt.append('\n').append(user.getTranslation("island.donate.inv.confirm-line",
                    TextVariables.NUMBER, String.valueOf(e.getValue()),
                    MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user),
                    POINTS_PLACEHOLDER, Utils.formatNumber(user, points)));
        }
        prompt.append('\n').append(user.getTranslation("island.donate.inv.confirm-total",
                POINTS_PLACEHOLDER, Utils.formatNumber(user, totalPoints)));

        askConfirmation(user, prompt.toString(), () -> performInvDonation(user, island));
        return true;
    }

    private void performInvDonation(User user, Island island) {
        PlayerInventory pInv = user.getPlayer().getInventory();
        ItemStack[] contents = pInv.getStorageContents();
        Map<String, Integer> donated = new HashMap<>();
        long totalPoints = 0L;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            Integer value = donationValue(item);
            if (value == null) {
                continue;
            }
            int amount = item.getAmount();
            long points = (long) value * amount;
            String customId = addon.getCustomBlockId(item);
            String donationId = customId != null ? customId : item.getType().name();
            donated.merge(donationId, amount, Integer::sum);
            totalPoints += points;
            addon.getManager().donateBlocks(island, user.getUniqueId(), donationId, amount, points);
            contents[i] = null;
        }
        pInv.setStorageContents(contents);

        if (donated.isEmpty()) {
            user.sendMessage("island.donate.empty");
            return;
        }
        int totalBlocks = donated.values().stream().mapToInt(Integer::intValue).sum();
        user.sendMessage("island.donate.success",
                POINTS_PLACEHOLDER, Utils.formatNumber(user, totalPoints),
                TextVariables.NUMBER, String.valueOf(totalBlocks));
        addon.getManager().recalculateAfterDonation(island);
    }

    private Map<String, Integer> collectDonatableTotals(PlayerInventory pInv) {
        Map<String, Integer> totals = new HashMap<>();
        for (ItemStack item : pInv.getStorageContents()) {
            if (donationValue(item) != null) {
                String customId = addon.getCustomBlockId(item);
                String key = customId != null ? customId : item.getType().name();
                totals.merge(key, item.getAmount(), Integer::sum);
            }
        }
        return totals;
    }

    /**
     * @return the per-block donation value if the item is a donatable block with a
     *         positive configured value, or null otherwise
     */
    private Integer donationValue(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        // Check custom block plugins first (Oraxen, Nexo, ItemsAdder)
        String customId = addon.getCustomBlockId(item);
        if (customId != null) {
            Integer value = addon.getBlockConfig().getValue(getWorld(), customId);
            return (value != null && value > 0) ? value : null;
        }
        // Fall back to vanilla block check
        if (!item.getType().isBlock()) {
            return null;
        }
        Integer value = addon.getBlockConfig().getValue(getWorld(), item.getType());
        return (value != null && value > 0) ? value : null;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // BentoBox includes the command label as args.get(0); the user-typed args start at index 1.
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        String handKeyword = user.getTranslation("island.donate.hand.keyword");
        String invKeyword = user.getTranslation("island.donate.inv.keyword");

        // First user-arg slot: suggest "hand" and "inv".
        if (args.size() <= 2) {
            return Optional.of(Util.tabLimit(List.of(handKeyword, invKeyword), lastArg));
        }
        // Second user-arg slot after "hand": suggest the held count.
        if (args.size() == 3 && isHandKeyword(user, args.get(1)) && user.isPlayer()) {
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

    private boolean isInvKeyword(User user, String arg) {
        String localized = user.getTranslation("island.donate.inv.keyword");
        return "inv".equalsIgnoreCase(arg) || localized.equalsIgnoreCase(arg);
    }
}
