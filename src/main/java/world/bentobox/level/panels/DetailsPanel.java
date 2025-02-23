package world.bentobox.level.panels;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Enums;

import lv.id.bonne.panelutils.PanelUtils;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.util.Utils;

/**
 * This class opens GUI that shows generator view for user.
 */
public class DetailsPanel {

    private static final String SPAWNER = "_SPAWNER";

    // ---------------------------------------------------------------------
    // Section: Enums
    // ---------------------------------------------------------------------

    /**
     * This enum holds possible tabs for current gui.
     */
    private enum Tab {
        /**
         * All block Tab
         */
        ALL_BLOCKS,
        /**
         * Blocks that have value
         */
        VALUE_BLOCKS,
        /**
         * Above Sea level Tab.
         */
        ABOVE_SEA_LEVEL,
        /**
         * Underwater Tab.
         */
        UNDERWATER,
        /**
         * Spawner Tab.
         */
        SPAWNER
    }

    /**
     * Sorting order of blocks.
     */
    private enum Filter {
        /**
         * By name
         */
        NAME,
        /**
         * By value
         */
        VALUE,
        /**
         * By number
         */
        COUNT
    }

    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * This variable holds targeted island.
     */
    private final Island island;

    /**
     * This variable holds targeted island level data.
     */
    private final IslandLevels levelsData;

    /**
     * This variable allows to access addon object.
     */
    private final Level addon;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    private final User user;

    /**
     * This variable holds a world to which gui referee.
     */
    private final World world;

    /**
     * Record that stores a Material or EntityType as a key and a value
     */
    private record BlockRec(Object key, Integer value, Integer limit) {
    }

    /**
     * This variable stores the list of elements to display.
     */
    private final List<BlockRec> blockCountList;

    /**
     * This variable holds current pageIndex for multi-page generator choosing.
     */
    private int pageIndex;

    /**
     * This variable stores which tab currently is active.
     */
    private Tab activeTab;

    /**
     * This variable stores active filter for items.
     */
    private Filter activeFilter;

    /**
     * This is internal constructor. It is used internally in current class to avoid
     * creating objects everywhere.
     *
     * @param addon Level object
     * @param world World where user is operating
     * @param user  User who opens panel
     */
    private DetailsPanel(Level addon, World world, User user) {
        this.addon = addon;
        this.world = world;
        this.user = user;

        this.island = this.addon.getIslands().getIsland(world, user);

        if (this.island != null) {
            this.levelsData = this.addon.getManager().getLevelsData(this.island);
        } else {
            this.levelsData = null;
        }

        // Default Filters
        this.activeTab = Tab.VALUE_BLOCKS;
        this.activeFilter = Filter.NAME;
        this.blockCountList = new ArrayList<>();

        this.updateFilters();
    }

    /**
     * This method builds this GUI.
     */
    private void build() {
        if (this.island == null || this.levelsData == null) {
            // Nothing to see.
            Utils.sendMessage(this.user, this.user.getTranslation("general.errors.no-island"));
            return;
        }

        if (this.levelsData.getMdCount().isEmpty() && this.levelsData.getUwCount().isEmpty()) {
            // Nothing to see.
            Utils.sendMessage(this.user, this.user.getTranslation("level.conversations.no-data"));
            return;
        }

        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        panelBuilder.template("detail_panel", new File(this.addon.getDataFolder(), "panels"));

        panelBuilder.parameters("[name]", this.user.getName());

        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);
        panelBuilder.registerTypeBuilder("BLOCK", this::createBlockButton);

        panelBuilder.registerTypeBuilder("FILTER", this::createFilterButton);

        // Register tabs
        panelBuilder.registerTypeBuilder("TAB", this::createTabButton);

        // Register unknown type builder.
        panelBuilder.build();
    }

    /**
     * This method updates filter of elements based on tabs.
     */
    private void updateFilters() {
        this.blockCountList.clear();

        if (this.activeTab == Tab.SPAWNER) {
            if (this.addon.getBlockConfig().isNotHiddenBlock(Material.SPAWNER)) {
                Map<EntityType, Integer> spawnerCountMap = new EnumMap<>(EntityType.class);
                spawnerCountMap = this.levelsData.getMdCount().entrySet().stream()
                        .filter(en -> en.getKey() instanceof EntityType)
                        .collect(Collectors.toMap(en -> (EntityType) en.getKey(), Map.Entry::getValue));

                spawnerCountMap.entrySet().stream().sorted((Map.Entry.comparingByKey())).forEachOrdered(entry -> {
                    if (entry.getValue() > 0) {
                        this.blockCountList.add(new BlockRec(entry.getKey(), entry.getValue(), 0));
                    }
                });
            }
        } else {
            Map<Object, Integer> materialCountMap = new HashMap<>();

            if (this.activeTab != Tab.UNDERWATER) {
                // All above water blocks
                materialCountMap.putAll(this.levelsData.getMdCount());
            }
            if (this.activeTab != Tab.ABOVE_SEA_LEVEL) {
                // All underwater blocks.
                materialCountMap.putAll(this.levelsData.getUwCount());
            }
            // Remove any hidden blocks
            materialCountMap.keySet().removeIf(this.addon.getBlockConfig()::isHiddenBlock);
            // Remove any zero amount items
            materialCountMap.values().removeIf(i -> i < 1);

            if (this.activeTab == Tab.VALUE_BLOCKS) {
                // Remove zero-value blocks
                materialCountMap.entrySet().removeIf(en -> Optional
                        .ofNullable(this.addon.getBlockConfig().getValue(world, en.getKey())).orElse(0) == 0);
            }
            // All done filtering, add the left overs
            blockCountList.addAll(
                    materialCountMap.entrySet().stream()
                            .map(entry -> new BlockRec(entry.getKey(), entry.getValue(), 0))
                            .collect(Collectors.toList()));

        }
        // Sort and filter
        Comparator<BlockRec> sorter;

        switch (this.activeFilter) {
        case COUNT -> {
            sorter = (o1, o2) -> {
                if (o1.value().equals(o2.value())) {
                    String o1Name = Utils.prettifyObject(o1.key(), this.user);
                    String o2Name = Utils.prettifyObject(o2.key(), this.user);

                    return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                } else {
                    return Integer.compare(o2.value(), o1.value());
                }
            };
        }
        case VALUE -> {
            sorter = (o1, o2) -> {
                int blockLimit = Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(o1.key()), 0);
                int o1Count = blockLimit > 0 ? Math.min(o1.value(), blockLimit) : o1.value();

                blockLimit = Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(o2.key()), 0);
                int o2Count = blockLimit > 0 ? Math.min(o2.value(), blockLimit) : o2.value();

                long o1Value = (long) o1Count
                        * this.addon.getBlockConfig().getBlockValues().getOrDefault(o1.key(), 0);
                long o2Value = (long) o2Count
                        * this.addon.getBlockConfig().getBlockValues().getOrDefault(o2.key(), 0);

                if (o1Value == o2Value) {
                    String o1Name = Utils.prettifyObject(o1.key(), this.user);
                    String o2Name = Utils.prettifyObject(o2.key(), this.user);

                    return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                } else {
                    return Long.compare(o2Value, o1Value);
                }
            };
        }
        default -> {
            sorter = (o1, o2) -> {
                String o1Name = Utils.prettifyObject(o1.key(), this.user);
                String o2Name = Utils.prettifyObject(o2.key(), this.user);

                return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
            };
        }
        }

        this.blockCountList.sort(sorter);
        this.pageIndex = 0;
    }

    // ---------------------------------------------------------------------
    // Section: Tab Button Type
    // ---------------------------------------------------------------------

    /**
     * Create tab button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createTabButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            // Set icon
            builder.icon(template.icon().clone());
        }

        if (template.title() != null) {
            // Set title
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null) {
            // Set description
            builder.description(this.user.getTranslation(this.world, template.description()));
        }

        Tab tab = Enums.getIfPresent(Tab.class, String.valueOf(template.dataMap().get("tab"))).or(Tab.VALUE_BLOCKS);

        // Get only possible actions, by removing all inactive ones.
        List<ItemTemplateRecord.ActionRecords> activeActions = new ArrayList<>(template.actions());

        activeActions.removeIf(action -> "VIEW".equalsIgnoreCase(action.actionType()) && this.activeTab == tab);

        // Add Click handler
        builder.clickHandler((panel, user, clickType, i) -> {
            for (ItemTemplateRecord.ActionRecords action : activeActions) {
                if ((clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                        && "VIEW".equalsIgnoreCase(action.actionType())) {
                    this.activeTab = tab;

                    // Update filters.
                    this.updateFilters();
                    this.build();
                }
            }

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = activeActions.stream().filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.world, action.tooltip())).filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty()) {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        builder.glow(this.activeTab == tab);

        return builder.build();
    }

    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createFilterButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            // Set icon
            builder.icon(template.icon().clone());
        }

        Filter filter;

        if (slot.amountMap().getOrDefault("FILTER", 0) > 1) {
            filter = Enums.getIfPresent(Filter.class, String.valueOf(template.dataMap().get("filter"))).or(Filter.NAME);
        } else {
            filter = this.activeFilter;
        }

        final String reference = "level.gui.buttons.filters.";

        if (template.title() != null) {
            // Set title
            builder.name(this.user.getTranslation(this.world,
                    template.title().replace("[filter]", filter.name().toLowerCase())));
        } else {
            builder.name(this.user.getTranslation(this.world, reference + filter.name().toLowerCase() + ".name"));
        }

        if (template.description() != null) {
            // Set description
            builder.description(this.user.getTranslation(this.world,
                    template.description().replace("[filter]", filter.name().toLowerCase())));
        } else {
            builder.name(
                    this.user.getTranslation(this.world, reference + filter.name().toLowerCase() + ".description"));
        }

        // Get only possible actions, by removing all inactive ones.
        List<ItemTemplateRecord.ActionRecords> activeActions = new ArrayList<>(template.actions());

        // Add Click handler
        builder.clickHandler((panel, user, clickType, i) -> {
            for (ItemTemplateRecord.ActionRecords action : activeActions) {
                if (clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType())) {
                    if ("UP".equalsIgnoreCase(action.actionType())) {
                        this.activeFilter = Utils.getNextValue(Filter.values(), filter);

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    } else if ("DOWN".equalsIgnoreCase(action.actionType())) {
                        this.activeFilter = Utils.getPreviousValue(Filter.values(), filter);

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    } else if ("SELECT".equalsIgnoreCase(action.actionType())) {
                        this.activeFilter = filter;

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    }
                }
            }

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = activeActions.stream().filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.world, action.tooltip())).filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty()) {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        builder.glow(this.activeFilter == filter);

        return builder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Create common buttons
    // ---------------------------------------------------------------------

    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createNextButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        long size = this.blockCountList.size();

        if (size <= slot.amountMap().getOrDefault("BLOCK", 1)
                || 1.0 * size / slot.amountMap().getOrDefault("BLOCK", 1) <= this.pageIndex + 1) {
            // There are no next elements
            return null;
        }

        int nextPageIndex = this.pageIndex + 2;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            ItemStack clone = template.icon().clone();

            if (Boolean.TRUE.equals(template.dataMap().getOrDefault("indexing", false))) {
                clone.setAmount(nextPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null) {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null) {
            builder.description(this.user.getTranslation(this.world, template.description(), TextVariables.NUMBER,
                    String.valueOf(nextPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) -> {
            for (ItemTemplateRecord.ActionRecords action : template.actions()) {
                if ((clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                        && "NEXT".equalsIgnoreCase(action.actionType())) {
                    this.pageIndex++;
                    this.build();
                }
            }

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.world, action.tooltip())).filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty()) {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }

    /**
     * Create previous button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createPreviousButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        if (this.pageIndex == 0) {
            // There are no next elements
            return null;
        }

        int previousPageIndex = this.pageIndex;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            ItemStack clone = template.icon().clone();

            if (Boolean.TRUE.equals(template.dataMap().getOrDefault("indexing", false))) {
                clone.setAmount(previousPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null) {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null) {
            builder.description(this.user.getTranslation(this.world, template.description(), TextVariables.NUMBER,
                    String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) -> {
            for (ItemTemplateRecord.ActionRecords action : template.actions()) {
                if ((clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                        && "PREVIOUS".equalsIgnoreCase(action.actionType())) {
                    this.pageIndex--;
                    this.build();
                }
            }

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().filter(action -> action.tooltip() != null)
                .map(action -> this.user.getTranslation(this.world, action.tooltip())).filter(text -> !text.isBlank())
                .collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty()) {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        return builder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Create Material Button
    // ---------------------------------------------------------------------

    /**
     * Create material button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createBlockButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        if (this.blockCountList.isEmpty()) {
            // Nothing to show
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault("BLOCK", 1) + slot.slot();

        if (index >= this.blockCountList.size()) {
            // Out of index.
            return null;
        }

        return this.createMaterialButton(template, this.blockCountList.get(index));
    }

    /**
     * This method creates button for block.
     *
     * @param template      the template of the button
     * @param blockCount  count
     * @return PanelItem button
     */
    private PanelItem createMaterialButton(ItemTemplateRecord template, BlockRec blockCount) {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        }
        // Show amount if less than 64.
        if (blockCount.value() < 64) {
            builder.amount(blockCount.value());
        }

        final String reference = "level.gui.buttons.material.";
        Object key = blockCount.key();
        String description = Utils.prettifyDescription(key, this.user);
        String blockId = "";
        int blockValue = 0;
        int blockLimit = 0;
        String value = "";
        String limit = "";
        String displayMaterial = Utils.prettifyObject(key, this.user);

        if (key instanceof Material m) {
            // Material-specific settings.
            builder.icon(PanelUtils.getMaterialItem(m));
            blockId = this.user.getTranslationOrNothing(reference + "id", "[id]", m.name());
            blockValue = this.addon.getBlockConfig().getBlockValues().getOrDefault(m, 0);
            blockLimit = Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(m), 0);
        } else if (key instanceof EntityType e) {
            // EntityType-specific settings.
            builder.icon(PanelUtils.getEntityEgg(e));
            description += this.user.getTranslation(this.world, "level.gui.buttons.spawner.block-name"); // Put spawner on the end
            blockId = this.user.getTranslationOrNothing(reference + "id", "[id]", e.name().concat(SPAWNER));
            blockValue = this.addon.getBlockConfig().getSpawnerValues().getOrDefault(e, 0);
            blockLimit = Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(e), 0);
        }

        if (template.title() != null) {
            builder.name(this.user.getTranslation(this.world, template.title(), TextVariables.NUMBER,
                    String.valueOf(blockCount.value()), "[material]", displayMaterial));
        }

        value = blockValue > 0
                ? this.user.getTranslationOrNothing(reference + "value", TextVariables.NUMBER,
                        String.valueOf(blockValue))
                : "";
        limit = blockLimit > 0
                ? this.user.getTranslationOrNothing(reference + "limit", TextVariables.NUMBER,
                        String.valueOf(blockLimit))
                : "";

        String count = this.user.getTranslationOrNothing(reference + "count", TextVariables.NUMBER,
                String.valueOf(blockCount.value()));

        long calculatedValue = (long) Math.min(blockLimit > 0 ? blockLimit : Integer.MAX_VALUE,
                blockCount.value()) * blockValue;
        String valueText = calculatedValue > 0 ? this.user.getTranslationOrNothing(reference + "calculated",
                TextVariables.NUMBER, String.valueOf(calculatedValue)) : "";

        // Hide block ID unless user is an operator.
        if (!user.isOp()) {
            blockId = "";
        }
        if (template.description() != null) {
            builder.description(this.user
                    .getTranslation(this.world, template.description(), "[description]", description, "[id]", blockId,
                            "[value]", value, "[calculated]", valueText, "[limit]", limit, "[count]", count)
                    .replaceAll("(?m)^[ \\t]*\\r?\\n", "").replaceAll("(?<!\\\\)\\|", "\n").replace("\\\\\\|", "|"));
        }

        return builder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Other Methods
    // ---------------------------------------------------------------------

    /**
     * This method is used to open UserPanel outside this class. It will be much
     * easier to open panel with single method call then initializing new object.
     *
     * @param addon Level object
     * @param world World where user is operating
     * @param user  User who opens panel
     */
    public static void openPanel(Level addon, World world, User user) {
        new DetailsPanel(addon, world, user).build();
    }


}
