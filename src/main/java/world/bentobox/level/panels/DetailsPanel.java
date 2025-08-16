package world.bentobox.level.panels;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Tag;
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
import world.bentobox.bentobox.hooks.ItemsAdderHook;
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
                spawnerCountMap.putAll(
                        this.levelsData.getUwCount().entrySet().stream().filter(en -> en.getKey() instanceof EntityType)
                                .collect(Collectors.toMap(en -> (EntityType) en.getKey(), Map.Entry::getValue)));
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
                        * this.addon.getBlockConfig().getBlockValues().getOrDefault(o1.key().toString().toLowerCase(Locale.ENGLISH), 0);
                long o2Value = (long) o2Count
                        * this.addon.getBlockConfig().getBlockValues().getOrDefault(o2.key().toString().toLowerCase(Locale.ENGLISH), 0);

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

    private PanelItem createMaterialButton(ItemTemplateRecord template, BlockRec blockCount) {
        PanelItemBuilder builder = new PanelItemBuilder();

        // Set icon and amount based on the template and block count.
        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        }
        if (blockCount.value() < 64) {
            builder.amount(blockCount.value());
        }

        final String ref = "level.gui.buttons.material.";
        BlockDataRec d = getBlockData(blockCount.key());

        // Override icon from block data if available.
        if (d.icon() != null) {
            builder.icon(d.icon());
        }

        // Set name using the title translation if provided.
        if (template.title() != null) {
            builder.name(this.user.getTranslation(this.world, template.title(), TextVariables.NUMBER,
                    String.valueOf(blockCount.value()), "[material]", d.displayMaterial()));
        }

        // Build and set description if provided.
        if (template.description() != null) {
            String description = buildDescription(template.description(), blockCount, d, ref);
            builder.description(description);
        }

        return builder.build();
    }

    private String buildDescription(String descriptionTemplate, BlockRec blockCount, BlockDataRec d, String ref) {
        String desc = Utils.prettifyDescription(blockCount.key(), this.user) + d.extraDesc();
        String id = this.user.isOp() ? d.blockId() : "";
        String value = d.blockValue() > 0
                ? this.user.getTranslationOrNothing(ref + "value", TextVariables.NUMBER, String.valueOf(d.blockValue()))
                : "";

        long calculatedRaw = (long) Math.min(d.blockLimit() > 0 ? d.blockLimit() : Integer.MAX_VALUE,
                blockCount.value()) * d.blockValue();
        String calculated = calculatedRaw > 0
                ? this.user.getTranslationOrNothing(ref + "calculated", TextVariables.NUMBER,
                        String.valueOf(calculatedRaw))
                : "";

        String limit = d.blockLimit() > 0
                ? this.user.getTranslationOrNothing(ref + "limit", TextVariables.NUMBER, String.valueOf(d.blockLimit()))
                : "";

        String count = this.user.getTranslationOrNothing(ref + "count", TextVariables.NUMBER,
                String.valueOf(blockCount.value()));

        String translated = this.user.getTranslation(this.world, descriptionTemplate, "[description]", desc, "[id]", id,
                "[value]", value, "[calculated]", calculated, "[limit]", limit, "[count]", count);

        return translated.replaceAll("(?m)^[ \\t]*\\r?\\n", "").replaceAll("(?<!\\\\)\\|", "\n").replace("\\\\\\|",
                "|");
    }

    /**
     * Block data record
     * @param icon - itemstack
     * @param blockId - Block ID string
     * @param blockValue - int value
     * @param displayMaterial - user friendly name
     * @param extraDesc - extra description
     */
    private record BlockDataRec(ItemStack icon, String blockId, int blockValue, int blockLimit, String displayMaterial,
            String extraDesc) {
    }

    private BlockDataRec getBlockData(Object key) {
        final String ref = "level.gui.buttons.material.";
        if (key instanceof Material m) {
            if (!m.isItem()) {
                m = convertItem(m);
            }
            return new BlockDataRec(PanelUtils.getMaterialItem(m),
                    this.user.getTranslationOrNothing(ref + "id", "[id]", m.name()),
                    Objects.requireNonNullElse(this.addon.getBlockConfig().getValue(world, m), 0),
                    Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(m), 0),
                    Utils.prettifyObject(key, this.user), "");
        } else if (key instanceof EntityType e) {
            return new BlockDataRec(PanelUtils.getEntityEgg(e),
                    this.user.getTranslationOrNothing(ref + "id", "[id]", e.name().concat(SPAWNER)),
                    Objects.requireNonNullElse(this.addon.getBlockConfig().getValue(world, e), 0),
                    Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(e), 0),
                    Utils.prettifyObject(key, this.user),
                    this.user.getTranslation(this.world, "level.gui.buttons.spawner.block-name"));
        } else if (key instanceof String s && addon.isItemsAdder()) {
            Optional<ItemStack> opt = ItemsAdderHook.getItemStack(s);
            ItemStack icon = opt.orElse(new ItemStack(Material.PAPER));
            String disp = opt.filter(is -> is.getItemMeta().hasDisplayName())
                    .map(is -> is.getItemMeta().getDisplayName()).orElse(Utils.prettifyObject(key, this.user));
            return new BlockDataRec(icon, this.user.getTranslationOrNothing(ref + "id", "[id]", s),
                    this.addon.getBlockConfig().getBlockValues().getOrDefault(s, 0),
                    Objects.requireNonNullElse(this.addon.getBlockConfig().getLimit(s), 0), disp, "");
        }
        return new BlockDataRec(new ItemStack(Material.PAPER), "", 0, 0, Utils.prettifyObject(key, this.user), "");
    }


    // ---------------------------------------------------------------------
    // Section: Other Methods
    // ---------------------------------------------------------------------

    private Material convertItem(Material m) {
        if (Tag.CROPS.isTagged(m)) {
            return Material.FARMLAND;
        }
        if (Tag.CANDLE_CAKES.isTagged(m)) {
            return Material.CAKE;
        }
        if (Tag.CAULDRONS.isTagged(m)) {
            return Material.CAULDRON;
        }
        if (Tag.FLOWER_POTS.isTagged(m)) {
            return Material.FLOWER_POT;
        }
        if (Tag.SNOW.isTagged(m)) {
            return Material.SNOW_BLOCK;
        }
        if (Tag.WALL_CORALS.isTagged(m)) {
            // Wall corals end in _WALL_FAN
            return Objects.requireNonNullElse(Material.getMaterial(m.name().substring(0, m.name().length() - 9)),
                    Material.BRAIN_CORAL);
        }
        if (Tag.WALL_SIGNS.isTagged(m)) {
            // Wall signs end in _WALL_SIGN 
            return Objects.requireNonNullElse(
                    Material.getMaterial(m.name().substring(0, m.name().length() - 10) + "_SIGN"), Material.OAK_SIGN);
        }
        if (Tag.WALL_HANGING_SIGNS.isTagged(m)) {
            // Wall signs end in _HANGING_WALL_SIGN 
            return Objects.requireNonNullElse(
                    Material.getMaterial(m.name().substring(0, m.name().length() - 18) + "_SIGN"), Material.OAK_SIGN);
        }

        return switch (m) {
        case WATER -> Material.WATER_BUCKET;
        case LAVA -> Material.LAVA_BUCKET;
        case TRIPWIRE -> Material.TRIPWIRE_HOOK;
        case PISTON_HEAD, MOVING_PISTON -> Material.PISTON;
        case TALL_SEAGRASS -> Material.SEAGRASS;
        case FIRE, SOUL_FIRE -> Material.FLINT_AND_STEEL;
        case REDSTONE_WIRE -> Material.REDSTONE;
        case WALL_TORCH, REDSTONE_WALL_TORCH, SOUL_WALL_TORCH -> Material.TORCH;
        case NETHER_PORTAL, END_PORTAL, END_GATEWAY -> Material.ENDER_PEARL;
        case ATTACHED_PUMPKIN_STEM, ATTACHED_MELON_STEM, PUMPKIN_STEM, MELON_STEM -> Material.PUMPKIN_SEEDS;
        case COCOA -> Material.COCOA_BEANS;
        case FROSTED_ICE -> Material.ICE;
        case WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON -> Material.CAULDRON;
        case SKELETON_WALL_SKULL, WITHER_SKELETON_WALL_SKULL, ZOMBIE_WALL_HEAD, PLAYER_WALL_HEAD, CREEPER_WALL_HEAD,
                DRAGON_WALL_HEAD, PIGLIN_WALL_HEAD ->
            Material.SKELETON_SKULL;
        case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
        case WEEPING_VINES_PLANT, TWISTING_VINES_PLANT -> Material.VINE;
        case CAVE_VINES, CAVE_VINES_PLANT -> Material.GLOW_BERRIES;
        case BIG_DRIPLEAF_STEM -> Material.BIG_DRIPLEAF;
        case BAMBOO_SAPLING, POTTED_BAMBOO -> Material.BAMBOO;
        case CARROTS -> Material.CARROT;
        case POTATOES -> Material.POTATO;
        case BEETROOTS -> Material.BEETROOT;
        case TORCHFLOWER_CROP -> Material.TORCHFLOWER_SEEDS;
        case PITCHER_CROP -> Material.PITCHER_PLANT;
        case VOID_AIR, CAVE_AIR, BUBBLE_COLUMN -> Material.GLASS_BOTTLE;
        case KELP_PLANT -> Material.KELP;
        case POWDER_SNOW -> Material.SNOWBALL;
        default -> Material.PAPER;
        };
    }

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
