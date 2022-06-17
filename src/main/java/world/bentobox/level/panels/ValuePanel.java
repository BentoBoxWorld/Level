package world.bentobox.level.panels;


import com.google.common.base.Enums;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lv.id.bonne.panelutils.PanelUtils;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.hooks.LangUtilsHook;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.level.Level;
import world.bentobox.level.util.ConversationUtils;
import world.bentobox.level.util.Utils;


/**
 * This class opens GUI that shows generator view for user.
 */
public class ValuePanel
{
    // ---------------------------------------------------------------------
    // Section: Internal Constructor
    // ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon Level object
     * @param world World where user is operating
     * @param user User who opens panel
     */
    private ValuePanel(Level addon,
        World world,
        User user)
    {
        this.addon = addon;
        this.world = world;
        this.user = user;

        this.activeFilter = Filter.NAME_ASC;
        this.materialRecordList = Arrays.stream(Material.values()).
            filter(Material::isBlock).
            filter(m -> !m.name().startsWith("LEGACY_")).
            map(material ->
            {
                Integer value = this.addon.getBlockConfig().getValue(this.world, material);
                Integer limit = this.addon.getBlockConfig().getBlockLimits().get(material);

                return new MaterialRecord(material,
                    value != null ? value : 0,
                    limit != null ? limit : 0);
            }).
            collect(Collectors.toList());

        this.elementList = new ArrayList<>(Material.values().length);
        this.searchText = "";

        this.updateFilters();
    }


    /**
     * This method builds this GUI.
     */
    private void build()
    {
        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(this.user);
        panelBuilder.world(this.user.getWorld());

        panelBuilder.template("value_panel", new File(this.addon.getDataFolder(), "panels"));

        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);
        panelBuilder.registerTypeBuilder("BLOCK", this::createMaterialButton);

        panelBuilder.registerTypeBuilder("FILTER", this::createFilterButton);
        panelBuilder.registerTypeBuilder("SEARCH", this::createSearchButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


    /**
     * This method updates filter of elements based on tabs.
     */
    private void updateFilters()
    {
        Comparator<MaterialRecord> sorter;

        switch (this.activeFilter)
        {
            case VALUE_ASC ->
            {
                sorter = (o1, o2) ->
                {
                    if (o1.value().equals(o2.value()))
                    {
                        String o1Name = Utils.prettifyObject(o1.material(), this.user);
                        String o2Name = Utils.prettifyObject(o2.material(), this.user);

                        return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                    }
                    else
                    {
                        return Integer.compare(o1.value(), o2.value());
                    }
                };
            }
            case VALUE_DESC ->
            {
                sorter = (o1, o2) ->
                {
                    if (o1.value().equals(o2.value()))
                    {
                        String o1Name = Utils.prettifyObject(o1.material(), this.user);
                        String o2Name = Utils.prettifyObject(o2.material(), this.user);

                        return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                    }
                    else
                    {
                        return Integer.compare(o2.value(), o1.value());
                    }
                };
            }
            case NAME_DESC ->
            {
                sorter = (o1, o2) ->
                {
                    String o1Name = Utils.prettifyObject(o1.material(), this.user);
                    String o2Name = Utils.prettifyObject(o2.material(), this.user);

                    return String.CASE_INSENSITIVE_ORDER.compare(o2Name, o1Name);
                };
            }
            default ->
            {
                sorter = (o1, o2) ->
                {
                    String o1Name = Utils.prettifyObject(o1.material(), this.user);
                    String o2Name = Utils.prettifyObject(o2.material(), this.user);

                    return String.CASE_INSENSITIVE_ORDER.compare(o1Name, o2Name);
                };
            }
        }

        this.materialRecordList.sort(sorter);

        if (!this.searchText.isBlank())
        {
            this.elementList = new ArrayList<>(this.materialRecordList.size());
            final String text = this.searchText.toLowerCase();

            this.materialRecordList.forEach(record ->
            {
                if (record.material.name().toLowerCase().contains(text) ||
                    Utils.prettifyObject(record.material(), this.user).toLowerCase().contains(text))
                {
                    this.elementList.add(record);
                }
            });
        }
        else
        {
            this.elementList = this.materialRecordList;
        }

        this.pageIndex = 0;
    }


// ---------------------------------------------------------------------
// Section: Tab Button Type
// ---------------------------------------------------------------------


    /**
     * Create tab button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    private PanelItem createSearchButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            // Set icon
            builder.icon(template.icon().clone());
        }

        if (template.title() != null)
        {
            // Set title
            builder.name(this.user.getTranslation(this.world, template.title(), "[text]", this.searchText));
        }

        if (template.description() != null)
        {
            // Set description
            builder.description(this.user.getTranslation(this.world, template.description(), "[text]", this.searchText));
        }

        // Get only possible actions, by removing all inactive ones.
        List<ItemTemplateRecord.ActionRecords> activeActions = new ArrayList<>(template.actions());

        activeActions.removeIf(action ->
            "CLEAR".equalsIgnoreCase(action.actionType()) && this.searchText.isBlank());

        // Add Click handler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            for (ItemTemplateRecord.ActionRecords action : activeActions)
            {
                if (clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                {
                    if ("CLEAR".equalsIgnoreCase(action.actionType()))
                    {
                        this.searchText = "";

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    }
                    else if ("INPUT".equalsIgnoreCase(action.actionType()))
                    {
                        // Create consumer that process description change
                        Consumer<String> consumer = value ->
                        {
                            if (value != null)
                            {
                                this.searchText = value;
                                this.updateFilters();
                            }

                            this.build();
                        };

                        // start conversation
                        ConversationUtils.createStringInput(consumer,
                            user,
                            user.getTranslation("level.conversations.write-search"),
                            user.getTranslation("level.conversations.search-updated"));
                    }
                }
            }

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = activeActions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        builder.glow(!this.searchText.isBlank());

        return builder.build();
    }


    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    private PanelItem createFilterButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            // Set icon
            builder.icon(template.icon().clone());
        }

        String filterName = String.valueOf(template.dataMap().get("filter"));

        final String reference = "level.gui.buttons.filters.";

        if (template.title() != null)
        {
            // Set title
            builder.name(this.user.getTranslation(this.world, template.title()));
        }
        else
        {
            builder.name(this.user.getTranslation(this.world, reference + filterName.toLowerCase() + ".name"));
        }

        if (template.description() != null)
        {
            // Set description
            builder.description(this.user.getTranslation(this.world, template.description()));
        }
        else
        {
            builder.name(this.user.getTranslation(this.world, reference + filterName.toLowerCase() + ".description"));
        }

        // Get only possible actions, by removing all inactive ones.
        List<ItemTemplateRecord.ActionRecords> activeActions = new ArrayList<>(template.actions());

        activeActions.removeIf(action -> {
            if (this.activeFilter.name().startsWith(filterName))
            {
                return this.activeFilter.name().endsWith("ASC") && "ASC".equalsIgnoreCase(action.actionType()) ||
                    this.activeFilter.name().endsWith("DESC") && "DESC".equalsIgnoreCase(action.actionType());
            }
            else
            {
                return "DESC".equalsIgnoreCase(action.actionType());
            }
        });

        // Add Click handler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            for (ItemTemplateRecord.ActionRecords action : activeActions)
            {
                if (clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                {
                    if ("ASC".equalsIgnoreCase(action.actionType()))
                    {
                        this.activeFilter = Enums.getIfPresent(Filter.class, filterName + "_ASC").or(Filter.NAME_ASC);

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    }
                    else if ("DESC".equalsIgnoreCase(action.actionType()))
                    {
                        this.activeFilter = Enums.getIfPresent(Filter.class, filterName + "_DESC").or(Filter.NAME_DESC);

                        // Update filters.
                        this.updateFilters();
                        this.build();
                    }
                }
            }

            return true;
        });

        // Collect tooltips.
        List<String> tooltips = activeActions.stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
            // Empty line and tooltips.
            builder.description("");
            builder.description(tooltips);
        }

        builder.glow(this.activeFilter.name().startsWith(filterName.toUpperCase()));

        return builder.build();
    }


// ---------------------------------------------------------------------
// Section: Create common buttons
// ---------------------------------------------------------------------


    /**
     * Create next button panel item.
     *
     * @param template the template
     * @param slot the slot
     * @return the panel item
     */
    private PanelItem createNextButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        long size = this.elementList.size();

        if (size <= slot.amountMap().getOrDefault("BLOCK", 1) ||
            1.0 * size / slot.amountMap().getOrDefault("BLOCK", 1) <= this.pageIndex + 1)
        {
            // There are no next elements
            return null;
        }

        int nextPageIndex = this.pageIndex + 2;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(nextPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                "[number]", String.valueOf(nextPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            for (ItemTemplateRecord.ActionRecords action : template.actions())
            {
                if (clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                {
                    if ("NEXT".equalsIgnoreCase(action.actionType()))
                    {
                        this.pageIndex++;
                        this.build();
                    }
                }
            }

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
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
     * @param slot the slot
     * @return the panel item
     */
    private PanelItem createPreviousButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.pageIndex == 0)
        {
            // There are no next elements
            return null;
        }

        int previousPageIndex = this.pageIndex;

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            ItemStack clone = template.icon().clone();

            if ((Boolean) template.dataMap().getOrDefault("indexing", false))
            {
                clone.setAmount(previousPageIndex);
            }

            builder.icon(clone);
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title()));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                "[number]", String.valueOf(previousPageIndex)));
        }

        // Add ClickHandler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            for (ItemTemplateRecord.ActionRecords action : template.actions())
            {
                if (clickType == action.clickType() || ClickType.UNKNOWN.equals(action.clickType()))
                {
                    if ("PREVIOUS".equalsIgnoreCase(action.actionType()))
                    {
                        this.pageIndex--;
                        this.build();
                    }
                }
            }

            // Always return true.
            return true;
        });

        // Collect tooltips.
        List<String> tooltips = template.actions().stream().
            filter(action -> action.tooltip() != null).
            map(action -> this.user.getTranslation(this.world, action.tooltip())).
            filter(text -> !text.isBlank()).
            collect(Collectors.toCollection(() -> new ArrayList<>(template.actions().size())));

        // Add tooltips.
        if (!tooltips.isEmpty())
        {
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
     * @param slot the slot
     * @return the panel item
     */
    private PanelItem createMaterialButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot)
    {
        if (this.elementList.isEmpty())
        {
            // Does not contain any generators.
            return null;
        }

        int index = this.pageIndex * slot.amountMap().getOrDefault("BLOCK", 1) + slot.slot();

        if (index >= this.elementList.size())
        {
            // Out of index.
            return null;
        }

        return this.createMaterialButton(template, this.elementList.get(index));
    }


    /**
     * This method creates button for material.
     *
     * @param template the template of the button
     * @param materialRecord materialRecord which button must be created.
     * @return PanelItem for generator tier.
     */
    private PanelItem createMaterialButton(ItemTemplateRecord template,
        MaterialRecord materialRecord)
    {
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else
        {
            builder.icon(PanelUtils.getMaterialItem(materialRecord.material()));
        }

        if (materialRecord.value() <= 64 && materialRecord.value() > 0)
        {
            builder.amount(materialRecord.value());
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title(),
                "[material]", Utils.prettifyObject(materialRecord.material(), this.user)));
        }

        String description = Utils.prettifyDescription(materialRecord.material(), this.user);

        final String reference = "level.gui.buttons.material.";
        String blockId = this.user.getTranslationOrNothing(reference + "id",
            "[id]", materialRecord.material().name());

        String value = this.user.getTranslationOrNothing(reference + "value",
            "[number]", String.valueOf(materialRecord.value()));

        String underWater;

        if (this.addon.getSettings().getUnderWaterMultiplier() > 1.0)
        {
            underWater = this.user.getTranslationOrNothing(reference + "underwater",
                "[number]", String.valueOf(materialRecord.value() * this.addon.getSettings().getUnderWaterMultiplier()));
        }
        else
        {
            underWater = "";
        }

        String limit = materialRecord.limit() > 0 ? this.user.getTranslationOrNothing(reference + "limit",
            "[number]",  String.valueOf(materialRecord.limit())) : "";

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                    "[description]", description,
                    "[id]", blockId,
                    "[value]", value,
                    "[underwater]", underWater,
                    "[limit]", limit).
                replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|"));
        }

        builder.clickHandler((panel, user1, clickType, i) -> {
            System.out.println("Material: " + materialRecord.material());
            return true;
        });

        return builder.build();
    }


// ---------------------------------------------------------------------
// Section: Other Methods
// ---------------------------------------------------------------------


    /**
     * This method is used to open UserPanel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param addon Level object
     * @param world World where user is operating
     * @param user User who opens panel
     */
    public static void openPanel(Level addon,
        World world,
        User user)
    {
        new ValuePanel(addon, world, user).build();
    }


// ---------------------------------------------------------------------
// Section: Enums
// ---------------------------------------------------------------------


    /**
     * Sorting order of blocks.
     */
    private enum Filter
    {
        /**
         * By name asc
         */
        NAME_ASC,
        /**
         * By name desc
         */
        NAME_DESC,
        /**
         * By value asc
         */
        VALUE_ASC,
        /**
         * By value desc
         */
        VALUE_DESC,
    }


    private record MaterialRecord(Material material, Integer value, Integer limit)
    {
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------

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
     * This variable stores the list of elements to display.
     */
    private final List<MaterialRecord> materialRecordList;

    /**
     * This variable stores the list of elements to display.
     */
    private List<MaterialRecord> elementList;

    /**
     * This variable holds current pageIndex for multi-page generator choosing.
     */
    private int pageIndex;

    /**
     * This variable stores which tab currently is active.
     */
    private String searchText;

    /**
     * This variable stores active filter for items.
     */
    private Filter activeFilter;
}
