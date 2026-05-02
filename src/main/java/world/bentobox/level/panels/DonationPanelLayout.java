package world.bentobox.level.panels;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;

/**
 * Resolved slot layout for the donation panel. Built from a
 * {@link PanelTemplateRecord} (loaded from {@code panels/donation_panel.yml}),
 * with a hardcoded fallback used when the template is missing or malformed.
 */
final class DonationPanelLayout {

    static final int DEFAULT_SIZE = 36;
    static final int DEFAULT_INFO_SLOT = 4;
    static final int DEFAULT_CANCEL_SLOT = 28;
    static final int DEFAULT_PREVIEW_SLOT = 31;
    static final int DEFAULT_CONFIRM_SLOT = 34;
    static final Material DEFAULT_BORDER_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    static final String DEFAULT_TITLE_REF = "island.donate.gui-title";
    static final int[] DEFAULT_DONATION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private static final String DATA_TYPE = "type";
    private static final String TYPE_INFO = "INFO";
    private static final String TYPE_CANCEL = "CANCEL";
    private static final String TYPE_PREVIEW = "PREVIEW";
    private static final String TYPE_CONFIRM = "CONFIRM";

    final int size;
    final int infoSlot;
    final int cancelSlot;
    final int previewSlot;
    final int confirmSlot;
    final int[] donationSlots;
    final Material borderMaterial;
    final Material infoMaterial;
    final Material cancelMaterial;
    final Material previewMaterial;
    final Material confirmMaterial;
    final String cancelTitleOverride;
    final String confirmTitleOverride;
    /** Translation key (or literal text) for the inventory title. */
    final String panelTitle;
    /**
     * Decorative items keyed by absolute slot index. These are template entries
     * whose {@code data.type} is absent or unrecognised. They reserve their slot
     * (keeping donated blocks out) and should be placed visibly in the inventory.
     */
    final Map<Integer, ItemStack> decorativeItems;

    private DonationPanelLayout(int size,
            int infoSlot, int cancelSlot, int previewSlot, int confirmSlot,
            int[] donationSlots,
            Material borderMaterial,
            Material infoMaterial, Material cancelMaterial,
            Material previewMaterial, Material confirmMaterial,
            String cancelTitleOverride, String confirmTitleOverride,
            String panelTitle,
            Map<Integer, ItemStack> decorativeItems) {
        this.size = size;
        this.infoSlot = infoSlot;
        this.cancelSlot = cancelSlot;
        this.previewSlot = previewSlot;
        this.confirmSlot = confirmSlot;
        this.donationSlots = donationSlots;
        this.borderMaterial = borderMaterial;
        this.infoMaterial = infoMaterial;
        this.cancelMaterial = cancelMaterial;
        this.previewMaterial = previewMaterial;
        this.confirmMaterial = confirmMaterial;
        this.cancelTitleOverride = cancelTitleOverride;
        this.confirmTitleOverride = confirmTitleOverride;
        this.panelTitle = panelTitle;
        this.decorativeItems = Collections.unmodifiableMap(decorativeItems);
    }

    /**
     * The hardcoded 4-row layout used before the template was added, and as a
     * fallback when the template cannot be parsed.
     */
    static DonationPanelLayout defaults() {
        return new DonationPanelLayout(
                DEFAULT_SIZE,
                DEFAULT_INFO_SLOT, DEFAULT_CANCEL_SLOT, DEFAULT_PREVIEW_SLOT, DEFAULT_CONFIRM_SLOT,
                DEFAULT_DONATION_SLOTS.clone(),
                DEFAULT_BORDER_MATERIAL,
                Material.BOOK, Material.RED_STAINED_GLASS_PANE,
                Material.EXPERIENCE_BOTTLE, Material.LIME_STAINED_GLASS_PANE,
                null, null,
                DEFAULT_TITLE_REF,
                Collections.emptyMap());
    }

    /**
     * Resolve a layout from a panel template. Returns {@link #defaults()} if the
     * template is null or doesn't define all four named buttons (INFO, CANCEL,
     * PREVIEW, CONFIRM) — partial layouts would be unusable.
     */
    static DonationPanelLayout fromTemplate(PanelTemplateRecord template) {
        if (template == null) {
            return defaults();
        }

        ItemTemplateRecord[][] content = template.content();
        int rowCount = computeRowCount(template);
        if (rowCount < 1) {
            return defaults();
        }

        int infoSlot = -1, cancelSlot = -1, previewSlot = -1, confirmSlot = -1;
        Material infoMat = Material.BOOK;
        Material cancelMat = Material.RED_STAINED_GLASS_PANE;
        Material previewMat = Material.EXPERIENCE_BOTTLE;
        Material confirmMat = Material.LIME_STAINED_GLASS_PANE;
        String cancelTitle = null, confirmTitle = null;

        Set<Integer> occupied = new LinkedHashSet<>();
        Map<Integer, ItemStack> decorativeItems = new LinkedHashMap<>();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 9; c++) {
                ItemTemplateRecord rec = content[r][c];
                if (rec == null) continue;
                int slot = r * 9 + c;
                // Any non-null template entry reserves the slot, even if its
                // data.type is unknown - admins shouldn't see decorative items
                // overwritten by donated blocks.
                occupied.add(slot);
                Object typeObj = rec.dataMap().get(DATA_TYPE);
                if (typeObj == null) {
                    // No data.type: treat as a decorative item.
                    ItemStack icon = rec.icon();
                    if (icon != null) {
                        decorativeItems.put(slot, icon.clone());
                    }
                    continue;
                }
                String type = String.valueOf(typeObj);
                switch (type) {
                case TYPE_INFO -> {
                    infoSlot = slot;
                    Material m = materialOf(rec.icon());
                    if (m != null) infoMat = m;
                }
                case TYPE_CANCEL -> {
                    cancelSlot = slot;
                    Material m = materialOf(rec.icon());
                    if (m != null) cancelMat = m;
                    cancelTitle = rec.title();
                }
                case TYPE_PREVIEW -> {
                    previewSlot = slot;
                    Material m = materialOf(rec.icon());
                    if (m != null) previewMat = m;
                }
                case TYPE_CONFIRM -> {
                    confirmSlot = slot;
                    Material m = materialOf(rec.icon());
                    if (m != null) confirmMat = m;
                    confirmTitle = rec.title();
                }
                default -> {
                    // Unknown data.type: reserve the slot and render the icon
                    // so admin-placed decorations are visible.
                    ItemStack icon = rec.icon();
                    if (icon != null) {
                        decorativeItems.put(slot, icon.clone());
                    }
                }
                }
            }
        }

        if (infoSlot < 0 || cancelSlot < 0 || previewSlot < 0 || confirmSlot < 0) {
            // A required button is missing - fall back rather than render a broken UI.
            return defaults();
        }

        Material borderMat = materialOf(template.border() == null ? null : template.border().icon());
        boolean hasBorder = borderMat != null && borderMat != Material.AIR;

        // Use the template's title key if it's set; fall back to the default.
        String titleKey = (template.title() != null && !template.title().isBlank())
                ? template.title()
                : DEFAULT_TITLE_REF;

        int size = rowCount * 9;
        int[] donationSlots = computeDonationSlots(rowCount, occupied, hasBorder);

        return new DonationPanelLayout(
                size, infoSlot, cancelSlot, previewSlot, confirmSlot, donationSlots,
                hasBorder ? borderMat : Material.AIR,
                infoMat, cancelMat, previewMat, confirmMat,
                cancelTitle, confirmTitle,
                titleKey,
                decorativeItems);
    }

    private static int computeRowCount(PanelTemplateRecord template) {
        int max = 0;
        boolean[] forced = template.forcedRows();
        if (forced != null) {
            for (int i = 0; i < forced.length; i++) {
                if (forced[i]) max = Math.max(max, i + 1);
            }
        }
        ItemTemplateRecord[][] content = template.content();
        for (int r = 0; r < content.length; r++) {
            for (int c = 0; c < content[r].length; c++) {
                if (content[r][c] != null) {
                    max = Math.max(max, r + 1);
                    break;
                }
            }
        }
        return Math.min(max, 6);
    }

    private static int[] computeDonationSlots(int rowCount, Set<Integer> reserved, boolean hasBorder) {
        int[] tmp = new int[rowCount * 9];
        int n = 0;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = r * 9 + c;
                if (reserved.contains(slot)) continue;
                if (hasBorder && (r == 0 || r == rowCount - 1 || c == 0 || c == 8)) continue;
                tmp[n++] = slot;
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    private static Material materialOf(ItemStack icon) {
        return icon == null ? null : icon.getType();
    }
}
