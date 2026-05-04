package world.bentobox.level.panels;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.TemplateReader;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.util.Utils;

/**
 * Donation GUI panel. Players drag blocks from their inventory into the donation
 * slots. A confirm button finalizes the donation; cancel returns items.
 *
 * @author tastybento
 */
public class DonationPanel implements Listener {

    private static final String POINTS_PLACEHOLDER = "[points]";

    private final Level addon;
    private final World world;
    private final User user;
    private final Island island;
    private final Inventory inventory;
    private final DonationPanelLayout layout;
    private final Set<Integer> donationSlotSet;
    private boolean confirmed = false;

    private DonationPanel(Level addon, World world, User user, Island island) {
        this.addon = addon;
        this.world = world;
        this.user = user;
        this.island = island;
        this.layout = loadLayout(addon);
        this.donationSlotSet = HashSet.newHashSet(layout.donationSlots.length);
        for (int s : layout.donationSlots) {
            donationSlotSet.add(s);
        }

        // Create the inventory — use the title from the template (falls back to the
        // default translation key when no template title is set).
        Component title = removeDefaultItalic(
                Util.parseMiniMessageOrLegacy(user.getTranslation(layout.panelTitle)));
        this.inventory = Bukkit.createInventory(null, layout.size, title);

        // Fill borders if a border material is configured
        if (layout.borderMaterial != null && layout.borderMaterial != Material.AIR) {
            ItemStack border = createNamedItem(layout.borderMaterial, " ");
            for (int i = 0; i < layout.size; i++) {
                if (!donationSlotSet.contains(i)
                        && i != layout.infoSlot && i != layout.cancelSlot
                        && i != layout.previewSlot && i != layout.confirmSlot
                        && !layout.decorativeItems.containsKey(i)) {
                    inventory.setItem(i, border);
                }
            }
        }

        // Place decorative items from the template (non-button, non-border entries)
        layout.decorativeItems.forEach(inventory::setItem);

        // Info pane
        long currentDonated = addon.getManager().getDonatedPoints(island);
        ItemStack info = createNamedItem(layout.infoMaterial,
                user.getTranslation("island.donate.gui-info",
                        POINTS_PLACEHOLDER, Utils.formatNumber(user, currentDonated)));
        inventory.setItem(layout.infoSlot, info);

        // Cancel button
        String cancelKey = layout.cancelTitleOverride != null
                ? layout.cancelTitleOverride : "island.donate.cancel";
        ItemStack cancel = createNamedItem(layout.cancelMaterial,
                user.getTranslation(cancelKey));
        inventory.setItem(layout.cancelSlot, cancel);

        // Preview pane (starts at 0)
        updatePreview();

        // Confirm button
        String confirmKey = layout.confirmTitleOverride != null
                ? layout.confirmTitleOverride : "island.donate.confirm";
        ItemStack confirm = createNamedItem(layout.confirmMaterial,
                user.getTranslation(confirmKey));
        inventory.setItem(layout.confirmSlot, confirm);

        // Register listener
        Bukkit.getPluginManager().registerEvents(this, addon.getPlugin());
    }

    private static DonationPanelLayout loadLayout(Level addon) {
        try {
            File panelFolder = new File(addon.getDataFolder(), "panels");
            PanelTemplateRecord template = TemplateReader.readTemplatePanel("donation_panel", panelFolder);
            return DonationPanelLayout.fromTemplate(template);
        } catch (Exception e) {
            addon.logError("Could not load donation_panel template, using default layout.");
            return DonationPanelLayout.defaults();
        }
    }

    private void build() {
        user.getPlayer().openInventory(inventory);
    }

    /**
     * Calculate the total point value of items in the donation slots.
     */
    private long calculateDonationValue() {
        long total = 0;
        for (int slot : layout.donationSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                Integer value = addon.getBlockConfig().getValue(world, item.getType());
                if (value != null && value > 0) {
                    total += (long) value * item.getAmount();
                }
            }
        }
        return total;
    }

    /**
     * Update the preview pane to show current point value.
     */
    private void updatePreview() {
        long points = calculateDonationValue();
        ItemStack preview = createNamedItem(layout.previewMaterial,
                user.getTranslation("island.donate.preview",
                        POINTS_PLACEHOLDER, Utils.formatNumber(user, points)));
        inventory.setItem(layout.previewSlot, preview);
    }

    /**
     * Check if a slot is a donation slot.
     */
    private boolean isDonationSlot(int slot) {
        return donationSlotSet.contains(slot);
    }

    /**
     * Process the donation - consume items and record them. Items with no
     * configured value are returned to the player rather than consumed.
     */
    private void processDonation() {
        Map<String, Integer> donations = new HashMap<>();
        long totalPoints = 0;
        Player player = user.getPlayer();

        for (int slot : layout.donationSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                Material mat = item.getType();
                Integer value = addon.getBlockConfig().getValue(world, mat);
                if (value != null && value > 0) {
                    int count = item.getAmount();
                    long points = (long) value * count;
                    donations.merge(mat.name(), count, Integer::sum);
                    totalPoints += points;
                    // Record each material type as a separate donation log entry
                    addon.getManager().donateBlocks(island, user.getUniqueId(), mat.name(), count, points);
                    // Clear the slot - items are consumed
                    inventory.setItem(slot, null);
                } else {
                    // Return valueless items to the player rather than consuming them
                    inventory.setItem(slot, null);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    overflow.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                }
            }
        }

        if (donations.isEmpty()) {
            user.sendMessage("island.donate.empty");
        } else {
            user.sendMessage("island.donate.success",
                    POINTS_PLACEHOLDER, Utils.formatNumber(user, totalPoints),
                    TextVariables.NUMBER, String.valueOf(donations.values().stream().mapToInt(Integer::intValue).sum()));
            // Queue a full level recalculation so the donation is reflected immediately
            addon.getManager().recalculateAfterDonation(island);
        }
    }

    /**
     * Return all items in donation slots to the player.
     */
    private void returnItems() {
        Player player = user.getPlayer();
        for (int slot : layout.donationSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                // Drop any items that don't fit
                overflow.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                inventory.setItem(slot, null);
            }
        }
    }

    private void cleanup() {
        HandlerList.unregisterAll(this);
    }

    // ---- Event Handlers ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(user.getUniqueId())) return;

        int slot = event.getRawSlot();

        if (slot >= layout.size) {
            handlePlayerInventoryClick(event);
            return;
        }
        if (slot == layout.cancelSlot) {
            handleCancel(event, player);
            return;
        }
        if (slot == layout.confirmSlot) {
            handleConfirm(event, player);
            return;
        }
        if (isDonationSlot(slot)) {
            handleDonationSlotClick(slot, player);
            return;
        }
        // Borders, info, preview - cancel
        event.setCancelled(true);
    }

    /**
     * Clicks in the player's own inventory: allow normal pickups, but intercept
     * shift-clicks to distribute valid blocks into donation slots.
     */
    private void handlePlayerInventoryClick(InventoryClickEvent event) {
        if (!event.isShiftClick()) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (!isValidDonationItem(clicked)) {
            return;
        }

        ItemStack remaining = distributeIntoDonationSlots(clicked.clone());
        event.setCurrentItem(remaining.getAmount() <= 0 ? null : remaining);
        updatePreview();
    }

    /**
     * Distribute {@code remaining} across the donation slots in order, mutating
     * its amount as items are consumed. Returns the same stack for convenience.
     */
    private ItemStack distributeIntoDonationSlots(ItemStack remaining) {
        int idx = 0;
        while (idx < layout.donationSlots.length && remaining.getAmount() > 0) {
            int ds = layout.donationSlots[idx];
            ItemStack existing = inventory.getItem(ds);
            if (existing == null || existing.getType().isAir()) {
                inventory.setItem(ds, remaining.clone());
                remaining.setAmount(0);
            } else if (existing.isSimilar(remaining) && existing.getAmount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                int transfer = Math.min(space, remaining.getAmount());
                ItemStack updated = existing.clone();
                updated.setAmount(updated.getAmount() + transfer);
                inventory.setItem(ds, updated);
                remaining.setAmount(remaining.getAmount() - transfer);
            }
            idx++;
        }
        return remaining;
    }

    private void handleCancel(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        returnItems();
        confirmed = true; // prevent double-return on close
        player.closeInventory();
        user.sendMessage("island.donate.cancelled");
    }

    private void handleConfirm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (calculateDonationValue() <= 0) {
            user.sendMessage("island.donate.empty");
            return;
        }
        confirmed = true;
        processDonation();
        player.closeInventory();
    }

    /**
     * Donation slots allow placement. Validate on the next tick and eject any
     * item that is not a configured block donation.
     */
    private void handleDonationSlotClick(int slot, Player player) {
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
            ItemStack inSlot = inventory.getItem(slot);
            if (inSlot != null && !inSlot.getType().isAir() && !isValidDonationItem(inSlot)) {
                inventory.setItem(slot, null);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(inSlot);
                overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                user.sendMessage("island.donate.invalid-item");
            }
            updatePreview();
        });
    }

    private boolean isValidDonationItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.getType().isBlock()) {
            return false;
        }
        Integer value = addon.getBlockConfig().getValue(world, item.getType());
        return value != null && value > 0;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        // Only allow drags into donation slots
        for (int slot : event.getRawSlots()) {
            if (slot < layout.size && !isDonationSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        // Update preview after drag
        Bukkit.getScheduler().runTask(addon.getPlugin(), this::updatePreview);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!player.getUniqueId().equals(user.getUniqueId())) return;

        // Return items if not confirmed
        if (!confirmed) {
            returnItems();
        }
        cleanup();
    }

    // ---- Helper Methods ----

    /**
     * Create a named item. If the text contains '|' characters, the first segment
     * becomes the display name and the rest become lore lines.
     */
    private static ItemStack createNamedItem(Material material, String text) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> parts = splitWithStyleCarryover(text);
            meta.displayName(removeDefaultItalic(Util.parseMiniMessageOrLegacy(parts.get(0))));
            if (parts.size() > 1) {
                List<Component> lore = new ArrayList<>(parts.size() - 1);
                for (int i = 1; i < parts.size(); i++) {
                    lore.add(removeDefaultItalic(Util.parseMiniMessageOrLegacy(parts.get(i))));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Splits a legacy-coded string on '|' while carrying the active color and
     * formatting codes from the previous segment to the next. Without this,
     * a span like "§cWarning|destroyed" would split into "§cWarning" and
     * "destroyed" — the second line would lose its red color, since the
     * componentToLegacy walker only emits §c once for a contiguous run.
     */
    private static List<String> splitWithStyleCarryover(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String activeColor = "";
        StringBuilder activeFormats = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '|') {
                result.add(current.toString());
                current = new StringBuilder();
                current.append(activeColor).append(activeFormats);
                i++;
            } else if (c == '\u00A7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String pair = "\u00A7" + code;
                current.append(pair);
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    activeColor = pair;
                    activeFormats.setLength(0);
                } else if ("klmno".indexOf(code) >= 0) {
                    activeFormats.append(pair);
                } else if (code == 'r') {
                    activeColor = "";
                    activeFormats.setLength(0);
                }
                i += 2;
            } else {
                current.append(c);
                i++;
            }
        }
        result.add(current.toString());
        return result;
    }

    private static Component removeDefaultItalic(Component component) {
        if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            return component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
        return component;
    }

    /**
     * Open the donation panel for a player.
     *
     * @param addon  Level addon
     * @param world  world context
     * @param user   the user opening the panel
     * @param island the island to donate to
     */
    public static void openPanel(Level addon, World world, User user, Island island) {
        new DonationPanel(addon, world, user, island).build();
    }
}
