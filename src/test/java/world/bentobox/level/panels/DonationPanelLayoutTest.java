package world.bentobox.level.panels;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;

/**
 * Pure layout-resolution tests for {@link DonationPanelLayout}. Avoids MockBukkit
 * by stubbing only the {@link ItemStack#getType()} contract that the resolver
 * uses.
 */
class DonationPanelLayoutTest {

    private static ItemStack iconOf(Material mat) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(mat);
        when(stack.clone()).thenReturn(stack);
        return stack;
    }

    private static ItemTemplateRecord namedButton(Material icon, String type, String title) {
        ItemTemplateRecord rec = new ItemTemplateRecord(iconOf(icon), title, null, null);
        rec.addData("type", type);
        return rec;
    }

    private static PanelTemplateRecord template(TemplateItem border, int forcedRows) {
        boolean[] forced = new boolean[6];
        for (int i = 0; i < forcedRows && i < 6; i++) forced[i] = true;
        return new PanelTemplateRecord(Panel.Type.INVENTORY, "title-key", border, null, forced);
    }

    private static TemplateItem border() {
        return new TemplateItem(iconOf(Material.BLACK_STAINED_GLASS_PANE));
    }

    @Test
    void nullTemplateUsesDefaults() {
        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(null);
        assertEquals(DonationPanelLayout.DEFAULT_SIZE, layout.size);
        assertEquals(DonationPanelLayout.DEFAULT_INFO_SLOT, layout.infoSlot);
        assertEquals(DonationPanelLayout.DEFAULT_CANCEL_SLOT, layout.cancelSlot);
        assertEquals(DonationPanelLayout.DEFAULT_PREVIEW_SLOT, layout.previewSlot);
        assertEquals(DonationPanelLayout.DEFAULT_CONFIRM_SLOT, layout.confirmSlot);
        assertArrayEquals(DonationPanelLayout.DEFAULT_DONATION_SLOTS, layout.donationSlots);
        assertEquals(DonationPanelLayout.DEFAULT_BORDER_MATERIAL, layout.borderMaterial);
    }

    @Test
    void defaultTemplateMatchesLegacyLayout() {
        // Mirrors the shipped panels/donation_panel.yml: 4 forced rows, INFO@(1,5),
        // CANCEL@(4,2), PREVIEW@(4,5), CONFIRM@(4,8), with a border.
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(36, layout.size);
        assertEquals(4, layout.infoSlot);
        assertEquals(28, layout.cancelSlot);
        assertEquals(31, layout.previewSlot);
        assertEquals(34, layout.confirmSlot);
        assertArrayEquals(DonationPanelLayout.DEFAULT_DONATION_SLOTS, layout.donationSlots);
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, layout.borderMaterial);
        assertEquals(Material.BOOK, layout.infoMaterial);
        assertEquals(Material.RED_STAINED_GLASS_PANE, layout.cancelMaterial);
        assertEquals(Material.EXPERIENCE_BOTTLE, layout.previewMaterial);
        assertEquals(Material.LIME_STAINED_GLASS_PANE, layout.confirmMaterial);
    }

    @Test
    void sixRowTemplateExpandsDonationGrid() {
        PanelTemplateRecord t = template(border(), 6);
        // Keep buttons in row 1 / row 6 corners so the middle 4 rows are donation
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(5, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(5, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(5, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(54, layout.size);
        // 4 inner rows of 7 columns each = 28 donation slots
        assertEquals(28, layout.donationSlots.length);
        // Spot check: first inner cell is slot 10, last is slot 43
        assertEquals(10, layout.donationSlots[0]);
        assertEquals(43, layout.donationSlots[layout.donationSlots.length - 1]);
    }

    @Test
    void singleRowTemplateHasNoDonationSlots() {
        // Pathological but should not crash: a 1-row template with all 4 buttons
        // packed into row 1. Border consumes the rest, leaving zero donation slots.
        PanelTemplateRecord t = template(border(), 1);
        t.addButtonTemplate(0, 1, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(0, 3, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(0, 5, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(0, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(9, layout.size);
        assertEquals(0, layout.donationSlots.length);
    }

    @Test
    void noBorderTemplateTurnsOuterRingIntoDonationSlots() {
        // No border item: every cell that isn't a named button becomes a donation slot.
        PanelTemplateRecord t = template(null, 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(36, layout.size);
        // 36 cells minus 4 named buttons = 32 donation slots
        assertEquals(32, layout.donationSlots.length);
        // borderMaterial is AIR sentinel when no border so the constructor skips fill
        assertEquals(Material.AIR, layout.borderMaterial);
    }

    @Test
    void missingButtonFallsBackToDefaults() {
        // Only 3 of the 4 required buttons defined; the resolver must not silently
        // produce a UI with no PREVIEW pane — instead it falls back to defaults.
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        // PREVIEW omitted on purpose
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(DonationPanelLayout.DEFAULT_SIZE, layout.size);
        assertEquals(DonationPanelLayout.DEFAULT_PREVIEW_SLOT, layout.previewSlot);
    }

    @Test
    void rowCountInferredFromContentWhenNoForcedRows() {
        // No force-shown set, but content occupies rows 1 and 5.
        PanelTemplateRecord t = template(border(), 0);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(4, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(4, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(4, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(45, layout.size);
        // The middle 3 rows (rows 2-4 = slots 9-35), inner 7 cols each = 21 donation slots
        assertEquals(21, layout.donationSlots.length);
    }

    @Test
    void titleOverridesArePropagated() {
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", "custom.cancel"));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", "custom.confirm"));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals("custom.cancel", layout.cancelTitleOverride);
        assertEquals("custom.confirm", layout.confirmTitleOverride);
    }

    @Test
    void unknownDataTypesAreIgnored() {
        // Spurious data.type values should not corrupt slot resolution.
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(1, 1, namedButton(Material.PAPER, "BOGUS", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(36, layout.size);
        // The BOGUS slot at row 2 col 2 (slot 10) is occupied by a non-recognised
        // template item, so it gets skipped from the donation slot set.
        assertNotEquals(0, layout.donationSlots.length);
        for (int s : layout.donationSlots) {
            assertNotEquals(10, s, "slot 10 must be reserved by the unknown template entry");
        }
    }

    @Test
    void decorativeItemsWithUnknownTypeAreStored() {
        // An entry with BOGUS type should appear in decorativeItems so
        // DonationPanel can place it visibly in the inventory.
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(1, 1, namedButton(Material.PAPER, "BOGUS", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertTrue(layout.decorativeItems.containsKey(10),
                "slot 10 must be in decorativeItems so it gets rendered");
        assertEquals(Material.PAPER, layout.decorativeItems.get(10).getType());
    }

    @Test
    void decorativeItemsWithNoTypeAreStored() {
        // An entry that has NO data section at all should also appear in
        // decorativeItems.
        PanelTemplateRecord t = template(border(), 4);
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        // Row 2 col 1 (slot 9): plain item with no data.type
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.getType()).thenReturn(Material.DIAMOND);
        when(diamond.clone()).thenReturn(diamond);
        t.addButtonTemplate(1, 0, new ItemTemplateRecord(diamond, null, null, null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertTrue(layout.decorativeItems.containsKey(9),
                "slot 9 must be in decorativeItems (no data.type set)");
        assertEquals(Material.DIAMOND, layout.decorativeItems.get(9).getType());
    }

    @Test
    void defaultsUsesDefaultTitleRef() {
        DonationPanelLayout layout = DonationPanelLayout.defaults();
        assertEquals(DonationPanelLayout.DEFAULT_TITLE_REF, layout.panelTitle);
    }

    @Test
    void templateTitleIsPropagatedToLayout() {
        PanelTemplateRecord t = new PanelTemplateRecord(Panel.Type.INVENTORY,
                "my.custom.title", border(), null,
                new boolean[]{true, true, true, true, false, false});
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals("my.custom.title", layout.panelTitle);
    }

    @Test
    void nullTemplateTitleFallsBackToDefaultTitleRef() {
        // title is null in the template — should default to DEFAULT_TITLE_REF.
        PanelTemplateRecord t = new PanelTemplateRecord(Panel.Type.INVENTORY,
                null, border(), null,
                new boolean[]{true, true, true, true, false, false});
        t.addButtonTemplate(0, 4, namedButton(Material.BOOK, "INFO", null));
        t.addButtonTemplate(3, 1, namedButton(Material.RED_STAINED_GLASS_PANE, "CANCEL", null));
        t.addButtonTemplate(3, 4, namedButton(Material.EXPERIENCE_BOTTLE, "PREVIEW", null));
        t.addButtonTemplate(3, 7, namedButton(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", null));

        DonationPanelLayout layout = DonationPanelLayout.fromTemplate(t);

        assertEquals(DonationPanelLayout.DEFAULT_TITLE_REF, layout.panelTitle);
    }

    // ---- YAML structure test (no MockBukkit needed — YamlConfiguration is pure SnakeYAML) ----

    @Test
    void shippedDonationPanelYmlUsesListForceShownAndHasAllFourButtons() {
        File ymlFile = new File("src/main/resources/panels/donation_panel.yml");
        assertTrue(ymlFile.exists(), "donation_panel.yml must exist under src/main/resources/panels/");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(ymlFile);

        // force-shown must be a list (consistent with top_panel.yml / detail_panel.yml)
        assertTrue(cfg.isList("donation_panel.force-shown"),
                "force-shown must be a list, e.g. [1,2,3,4], not a scalar integer");

        List<Integer> forcedRows = cfg.getIntegerList("donation_panel.force-shown");
        assertEquals(4, forcedRows.size(),
                "donation_panel.yml should force exactly 4 rows");

        // All four named buttons must be present in the content section
        assertNotNull(cfg.getString("donation_panel.content.1.5.data.type"),
                "INFO button must be at row 1 col 5");
        assertEquals("INFO", cfg.getString("donation_panel.content.1.5.data.type"));

        assertNotNull(cfg.getString("donation_panel.content.4.2.data.type"),
                "CANCEL button must be at row 4 col 2");
        assertEquals("CANCEL", cfg.getString("donation_panel.content.4.2.data.type"));

        assertNotNull(cfg.getString("donation_panel.content.4.5.data.type"),
                "PREVIEW button must be at row 4 col 5");
        assertEquals("PREVIEW", cfg.getString("donation_panel.content.4.5.data.type"));

        assertNotNull(cfg.getString("donation_panel.content.4.8.data.type"),
                "CONFIRM button must be at row 4 col 8");
        assertEquals("CONFIRM", cfg.getString("donation_panel.content.4.8.data.type"));

        // Title must match the default translation key
        assertEquals(DonationPanelLayout.DEFAULT_TITLE_REF,
                cfg.getString("donation_panel.title"));
    }
}
