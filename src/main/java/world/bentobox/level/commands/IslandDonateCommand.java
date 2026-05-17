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

        int previewAmount = Math.min(requested, hand.getAmount());
        final int finalRequested = requested;

        // Apply blockconfig limit to the preview so the confirm prompt shows the
        // amount that will actually be destroyed, not the raw request.
        Object displayKey = customId != null ? customId : material;
        String donationId = customId != null ? customId : material.name();
        Object blockObj = customId != null ? (Object) customId : material;
        Integer limit = addon.getBlockConfig().getLimit(blockObj);
        boolean limited = false;
        if (limit != null) {
            int already = addon.getManager().getDonatedBlocks(island).getOrDefault(donationId, 0);
            int remaining = Math.max(0, limit - already);
            if (remaining == 0) {
                user.sendMessage("island.donate.limit-reached",
                        MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user));
                return false;
            }
            if (previewAmount > remaining) {
                previewAmount = remaining;
                limited = true;
            }
        }

        long previewPoints = (long) previewAmount * blockValue;
        String prompt = user.getTranslation("island.donate.hand.confirm-prompt",
                TextVariables.NUMBER, String.valueOf(previewAmount),
                MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user),
                POINTS_PLACEHOLDER, Utils.formatNumber(user, previewPoints));
        if (limited) {
            // The limit-notice locale uses '|' as a lore line-break for the GUI;
            // translate to '\n' here so the chat confirmation prompt wraps cleanly.
            prompt = prompt + "\n"
                    + user.getTranslation("island.donate.limit-notice").replace('|', '\n');
        }

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

        // Apply blockconfig donation limit
        String donationId = customId != null ? customId : material.name();
        Object blockObj = customId != null ? (Object) customId : material;
        Object displayKey = customId != null ? customId : material;
        Integer limit = addon.getBlockConfig().getLimit(blockObj);
        if (limit != null) {
            int currentDonated = addon.getManager().getDonatedBlocks(island).getOrDefault(donationId, 0);
            int remaining = Math.max(0, limit - currentDonated);
            if (remaining == 0) {
                user.sendMessage("island.donate.limit-reached",
                        MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user));
                return;
            }
            amount = Math.min(amount, remaining);
        }

        long points = (long) amount * blockValue;

        if (amount >= currentHand.getAmount()) {
            user.getPlayer().getInventory().setItemInMainHand(null);
        } else {
            currentHand.setAmount(currentHand.getAmount() - amount);
        }

        addon.getManager().donateBlocks(island, user.getUniqueId(), donationId, amount, points);
        addon.getManager().recalculateAfterDonation(island);

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

        Map<String, Integer> alreadyDonated = addon.getManager().getDonatedBlocks(island);
        long totalPoints = 0L;
        boolean limited = false;
        StringBuilder prompt = new StringBuilder(
                user.getTranslation("island.donate.inv.confirm-header"));
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            // Vanilla keys are stored as Material.name() (e.g. "STONE"), but blockValues uses
            // the lowercase namespaced key (e.g. "stone"). Resolving to a Material first lets
            // getValue() derive the correct lowercase key via material.getKey().getKey().
            // Custom-block keys (e.g. "oraxen:my_block") do not match any Material, so they
            // are passed through as Strings and match blockValues directly.
            Material mat = Material.matchMaterial(e.getKey());
            Object displayKey = mat != null ? mat : e.getKey();
            Integer rawValue = addon.getBlockConfig().getValue(getWorld(), displayKey);
            if (rawValue == null) continue;

            String donationId = mat != null ? mat.name() : e.getKey();
            Object blockObj = mat != null ? mat : e.getKey();
            int amount = e.getValue();
            Integer limit = addon.getBlockConfig().getLimit(blockObj);
            if (limit != null) {
                int remaining = Math.max(0, limit - alreadyDonated.getOrDefault(donationId, 0));
                int accepted = Math.min(amount, remaining);
                if (accepted < amount) {
                    limited = true;
                }
                amount = accepted;
            }
            if (amount <= 0) {
                continue;
            }
            long points = (long) rawValue * amount;
            totalPoints += points;
            prompt.append('\n').append(user.getTranslation("island.donate.inv.confirm-line",
                    TextVariables.NUMBER, String.valueOf(amount),
                    MATERIAL_PLACEHOLDER, Utils.prettifyObject(displayKey, user),
                    POINTS_PLACEHOLDER, Utils.formatNumber(user, points)));
        }
        if (limited) {
            // The limit-notice locale uses '|' as a lore line-break for the GUI;
            // translate to '\n' here so the chat confirmation prompt wraps cleanly.
            prompt.append('\n').append(
                    user.getTranslation("island.donate.limit-notice").replace('|', '\n'));
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
            String customId = addon.getCustomBlockId(item);
            String donationId = customId != null ? customId : item.getType().name();
            Object blockObj = customId != null ? (Object) customId : item.getType();

            // Apply blockconfig donation limit: only take up to the remaining capacity
            int amount = item.getAmount();
            Integer limit = addon.getBlockConfig().getLimit(blockObj);
            if (limit != null) {
                // getDonatedBlocks returns the live cached map, already updated by earlier donateBlocks calls
                int alreadyDonated = addon.getManager().getDonatedBlocks(island).getOrDefault(donationId, 0);
                int remaining = Math.max(0, limit - alreadyDonated);
                if (remaining == 0) {
                    // Limit already reached for this material — leave item in inventory
                    continue;
                }
                amount = Math.min(amount, remaining);
            }

            // Remove accepted amount from inventory slot
            if (amount >= item.getAmount()) {
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - amount);
            }

            long points = (long) value * amount;
            donated.merge(donationId, amount, Integer::sum);
            totalPoints += points;
            addon.getManager().donateBlocks(island, user.getUniqueId(), donationId, amount, points);
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
