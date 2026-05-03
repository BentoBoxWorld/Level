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

    private DonationPanelLayout(Builder b) {
        this.size = b.size;
        this.infoSlot = b.infoSlot;
        this.cancelSlot = b.cancelSlot;
        this.previewSlot = b.previewSlot;
        this.confirmSlot = b.confirmSlot;
        this.donationSlots = b.donationSlots;
        this.borderMaterial = b.borderMaterial;
        this.infoMaterial = b.infoMaterial;
        this.cancelMaterial = b.cancelMaterial;
        this.previewMaterial = b.previewMaterial;
        this.confirmMaterial = b.confirmMaterial;
        this.cancelTitleOverride = b.cancelTitleOverride;
        this.confirmTitleOverride = b.confirmTitleOverride;
        this.panelTitle = b.panelTitle;
        this.decorativeItems = Collections.unmodifiableMap(b.decorativeItems);
    }

    /**
     * The hardcoded 4-row layout used before the template was added, and as a
     * fallback when the template cannot be parsed.
     */
    static DonationPanelLayout defaults() {
        Builder b = new Builder();
        b.size = DEFAULT_SIZE;
        b.infoSlot = DEFAULT_INFO_SLOT;
        b.cancelSlot = DEFAULT_CANCEL_SLOT;
        b.previewSlot = DEFAULT_PREVIEW_SLOT;
        b.confirmSlot = DEFAULT_CONFIRM_SLOT;
        b.donationSlots = DEFAULT_DONATION_SLOTS.clone();
        b.borderMaterial = DEFAULT_BORDER_MATERIAL;
        return new DonationPanelLayout(b);
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
        int rowCount = computeRowCount(template);
        if (rowCount < 1) {
            return defaults();
        }

        Builder b = new Builder();
        ItemTemplateRecord[][] content = template.content();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 9; c++) {
                processCell(b, r * 9 + c, content[r][c]);
            }
        }

        if (b.infoSlot < 0 || b.cancelSlot < 0 || b.previewSlot < 0 || b.confirmSlot < 0) {
            // A required button is missing - fall back rather than render a broken UI.
            return defaults();
        }

        Material borderMat = materialOf(template.border() == null ? null : template.border().icon());
        boolean hasBorder = borderMat != null && borderMat != Material.AIR;
        b.borderMaterial = hasBorder ? borderMat : Material.AIR;
        b.size = rowCount * 9;
        b.donationSlots = computeDonationSlots(rowCount, b.occupied, hasBorder);
        b.panelTitle = (template.title() != null && !template.title().isBlank())
                ? template.title()
                : DEFAULT_TITLE_REF;
        return new DonationPanelLayout(b);
    }

    /**
     * Apply a single template cell to the builder: reserve its slot, then either
     * record it as a named button, decorative item, or unrecognised entry.
     */
    private static void processCell(Builder b, int slot, ItemTemplateRecord rec) {
        if (rec == null) {
            return;
        }
        // Any non-null template entry reserves the slot, even if its data.type is
        // unknown - admins shouldn't see decorative items overwritten by donated blocks.
        b.occupied.add(slot);
        Object typeObj = rec.dataMap().get(DATA_TYPE);
        if (typeObj == null) {
            addDecorative(b, slot, rec);
            return;
        }
        switch (String.valueOf(typeObj)) {
        case TYPE_INFO -> {
            b.infoSlot = slot;
            Material m = materialOf(rec.icon());
            if (m != null) b.infoMaterial = m;
        }
        case TYPE_CANCEL -> {
            b.cancelSlot = slot;
            Material m = materialOf(rec.icon());
            if (m != null) b.cancelMaterial = m;
            b.cancelTitleOverride = rec.title();
        }
        case TYPE_PREVIEW -> {
            b.previewSlot = slot;
            Material m = materialOf(rec.icon());
            if (m != null) b.previewMaterial = m;
        }
        case TYPE_CONFIRM -> {
            b.confirmSlot = slot;
            Material m = materialOf(rec.icon());
            if (m != null) b.confirmMaterial = m;
            b.confirmTitleOverride = rec.title();
        }
        default -> addDecorative(b, slot, rec);
        }
    }

    private static void addDecorative(Builder b, int slot, ItemTemplateRecord rec) {
        ItemStack icon = rec.icon();
        if (icon != null) {
            b.decorativeItems.put(slot, icon.clone());
        }
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
                boolean borderEdge = hasBorder
                        && (r == 0 || r == rowCount - 1 || c == 0 || c == 8);
                if (!reserved.contains(slot) && !borderEdge) {
                    tmp[n++] = slot;
                }
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    private static Material materialOf(ItemStack icon) {
        return icon == null ? null : icon.getType();
    }

    /**
     * Mutable accumulator used while resolving a template into a layout. Keeps
     * the resolution code free of long parameter lists and cross-method state
     * juggling.
     */
    private static final class Builder {
        int size = DEFAULT_SIZE;
        int infoSlot = -1;
        int cancelSlot = -1;
        int previewSlot = -1;
        int confirmSlot = -1;
        int[] donationSlots = DEFAULT_DONATION_SLOTS.clone();
        Material borderMaterial = DEFAULT_BORDER_MATERIAL;
        Material infoMaterial = Material.BOOK;
        Material cancelMaterial = Material.RED_STAINED_GLASS_PANE;
        Material previewMaterial = Material.EXPERIENCE_BOTTLE;
        Material confirmMaterial = Material.LIME_STAINED_GLASS_PANE;
        String cancelTitleOverride;
        String confirmTitleOverride;
        String panelTitle = DEFAULT_TITLE_REF;
        final Set<Integer> occupied = new LinkedHashSet<>();
        final Map<Integer, ItemStack> decorativeItems = new LinkedHashMap<>();
    }
}
