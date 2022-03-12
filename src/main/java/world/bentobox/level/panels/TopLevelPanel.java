///
// Created by BONNe
// Copyright - 2021
///

package world.bentobox.level.panels;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.permissions.PermissionAttachmentInfo;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.level.Level;


/**
 * This panel opens top likes panel
 */
public class TopLevelPanel
{
// ---------------------------------------------------------------------
// Section: Internal Constructor
// ---------------------------------------------------------------------


    /**
     * This is internal constructor. It is used internally in current class to avoid creating objects everywhere.
     *
     * @param addon Level object.
     * @param user User who opens Panel.
     * @param world World where gui is opened
     * @param permissionPrefix Permission Prefix
     */
    private TopLevelPanel(Level addon, User user, World world, String permissionPrefix)
    {
        this.addon = addon;
        this.user = user;
        this.world = world;

        this.iconPermission = permissionPrefix + "level.icon";

        this.topIslands = this.addon.getManager().getTopTen(this.world, 10).entrySet().stream().
            map(entry -> {
                Island island = this.addon.getIslandsManager().getIsland(this.world, entry.getKey());
                return new IslandTopRecord(island, entry.getValue());
            }).
            collect(Collectors.toList());
    }


    /**
     * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy to use and users can get nice
     * panels.
     */
    public void build()
    {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();

        panelBuilder.user(this.user);
        panelBuilder.world(this.world);

        panelBuilder.template("top_panel", new File(this.addon.getDataFolder(), "panels"));

        panelBuilder.registerTypeBuilder("VIEW", this::createViewerButton);
        panelBuilder.registerTypeBuilder("TOP", this::createPlayerButton);

        // Register unknown type builder.
        panelBuilder.build();
    }


// ---------------------------------------------------------------------
// Section: Methods
// ---------------------------------------------------------------------


    /**
     * Creates fallback based on template.
     * @param template Template record for fallback button.
     * @param index Place of the fallback.
     * @return Fallback panel item.
     */
    private PanelItem createFallback(ItemTemplateRecord template, long index)
    {
        if (template == null)
        {
            return null;
        }

        final String reference = "level.gui.buttons.island.";

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }

        if (template.title() != null)
        {
            builder.name(this.user.getTranslation(this.world, template.title(),
                "[name]", String.valueOf(index)));
        }
        else
        {
            builder.name(this.user.getTranslation(this.world, reference,
                "[name]", String.valueOf(index)));
        }

        if (template.description() != null)
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                "[number]", String.valueOf(index)));
        }

        builder.amount(index != 0 ? (int) index : 1);

        return builder.build();
    }


    /**
     * This method creates player icon with warp functionality.
     *
     * @return PanelItem for PanelBuilder.
     */
    private PanelItem createPlayerButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot)
    {
        int index = (int) template.dataMap().getOrDefault("index", 0);

        if (index < 1)
        {
            return this.createFallback(template.fallback(), index);
        }

        IslandTopRecord islandTopRecord = this.topIslands.size() < index ? null : this.topIslands.get(index - 1);

        if (islandTopRecord == null)
        {
            return this.createFallback(template.fallback(), index);
        }

        return this.createIslandIcon(template, islandTopRecord, index);
    }


    /**
     * This method creates button from template for given island top record.
     * @param template Icon Template.
     * @param islandTopRecord Island Top Record.
     * @param index Place Index.
     * @return PanelItem for PanelBuilder.
     */
    private PanelItem createIslandIcon(ItemTemplateRecord template, IslandTopRecord islandTopRecord, int index)
    {
        // Get player island.
        Island island = islandTopRecord.island();

        if (island == null)
        {
            return this.createFallback(template.fallback(), index);
        }

        PanelItemBuilder builder = new PanelItemBuilder();

        this.populateIslandIcon(builder, template, island);
        this.populateIslandTitle(builder, template, island);
        this.populateIslandDescription(builder, template, island, islandTopRecord, index);

        builder.amount(index);

        // Get only possible actions, by removing all inactive ones.
        List<ItemTemplateRecord.ActionRecords> activeActions = new ArrayList<>(template.actions());

        activeActions.removeIf(action ->
            "VIEW".equalsIgnoreCase(action.actionType()) && island.getOwner() == null &&
                island.getMemberSet(RanksManager.MEMBER_RANK).
                    contains(this.user.getUniqueId()));

        // Add Click handler
        builder.clickHandler((panel, user, clickType, i) ->
        {
            for (ItemTemplateRecord.ActionRecords action : activeActions)
            {
                if (clickType == action.clickType() && "VIEW".equalsIgnoreCase(action.actionType()))
                {
                    this.user.closeInventory();
                    // Open Detailed GUI.
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

        return builder.build();
    }


    /**
     * Populate given panel item builder name with values from template and island objects.
     *
     * @param builder the builder
     * @param template the template
     * @param island the island
     */
    private void populateIslandTitle(PanelItemBuilder builder, 
        ItemTemplateRecord template, 
        Island island)
    {
        final String reference = "level.gui.buttons.island.";

        // Get Island Name
        String nameText;

        if (island.getName() == null || island.getName().isEmpty())
        {
            nameText = this.user.getTranslation(reference + "owners-island",
                "[player]",
                island.getOwner() == null ?
                    this.user.getTranslation(reference + "unknown") :
                    this.addon.getPlayers().getName(island.getOwner()));
        }
        else
        {
            nameText = island.getName();
        }

        // Template specific title is always more important than custom one.
        if (template.title() != null && !template.title().isBlank())
        {
            builder.name(this.user.getTranslation(this.world, template.title(),
                "[name]", nameText));
        }
        else
        {
            builder.name(this.user.getTranslation(reference + "name", "[name]", nameText));
        }
    }


    /**
     * Populate given panel item builder icon with values from template and island objects.
     *
     * @param builder the builder
     * @param template the template
     * @param island the island
     */
    private void populateIslandIcon(PanelItemBuilder builder,
        ItemTemplateRecord template,
        Island island)
    {
        User owner = island.getOwner() == null ? null : User.getInstance(island.getOwner());
        
        // Get permission or island icon
        String permissionIcon = TopLevelPanel.getPermissionValue(owner, this.iconPermission);

        Material material;

        if (permissionIcon != null && !permissionIcon.equals("*"))
        {
            material = Material.matchMaterial(permissionIcon);
        }
        else
        {
            material = null;
        }

        if (material != null)
        {
            if (!material.equals(Material.PLAYER_HEAD))
            {
                builder.icon(material);
            }
            else
            {
                builder.icon(owner.getName());
            }
        }
        else if (template.icon() != null)
        {
            builder.icon(template.icon().clone());
        }
        else if (owner != null)
        {
            builder.icon(owner.getName());
        }
        else
        {
            builder.icon(Material.PLAYER_HEAD);
        }
    }


    /**
     * Populate given panel item builder description with values from template and island objects.
     *
     * @param builder the builder
     * @param template the template
     * @param island the island
     * @param islandTopRecord the top record object
     * @param index place index.
     */
    private void populateIslandDescription(PanelItemBuilder builder, 
        ItemTemplateRecord template,
        Island island, 
        IslandTopRecord islandTopRecord,
        int index)
    {
        final String reference = "level.gui.buttons.island.";

        // Get Owner Name
        String ownerText = this.user.getTranslation(reference + "owner",
            "[player]",
            island.getOwner() == null ?
                this.user.getTranslation(reference + "unknown") :
                this.addon.getPlayers().getName(island.getOwner()));

        // Get Members Text
        String memberText;

        if (island.getMemberSet().size() > 1)
        {
            StringBuilder memberBuilder = new StringBuilder(
                this.user.getTranslationOrNothing(reference + "members-title"));

            for (UUID uuid : island.getMemberSet())
            {
                User user = User.getInstance(uuid);

                if (memberBuilder.length() > 0)
                {
                    memberBuilder.append("\n");
                }

                memberBuilder.append(
                    this.user.getTranslationOrNothing(reference + "member",
                        "[player]", user.getName()));
            }

            memberText = memberBuilder.toString();
        }
        else
        {
            memberText = "";
        }

        String placeText = this.user.getTranslation(reference + "place",
            "[number]", String.valueOf(index));

        String levelText = this.user.getTranslation(reference + "level",
            "[number]", String.valueOf(islandTopRecord.level()));

        // Template specific description is always more important than custom one.
        if (template.description() != null && !template.description().isBlank())
        {
            builder.description(this.user.getTranslation(this.world, template.description(),
                    "[owner]", ownerText,
                    "[members]", memberText,
                    "[level]", levelText,
                    "[place]", placeText).
                replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|"));
        }
        else
        {
            // Now combine everything.
            String descriptionText = this.user.getTranslation(reference + "description",
                "[owner]", ownerText,
                "[members]", memberText,
                "[level]", levelText,
                "[place]", placeText);

            builder.description(descriptionText.
                replaceAll("(?m)^[ \\t]*\\r?\\n", "").
                replaceAll("(?<!\\\\)\\|", "\n").
                replaceAll("\\\\\\|", "|"));
        }
    }
    

    /**
     * Create viewer button panel item.
     *
     * @return PanelItem for PanelBuilder.
     */
    private PanelItem createViewerButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot)
    {
        Island island = this.addon.getIslands().getIsland(this.world, this.user);

        if (island == null || island.getOwner() == null)
        {
            // Player do not have an island.
            return null;
        }

        int place = this.addon.getManager().getRank(this.world, this.user.getUniqueId());
        long level = this.addon.getIslandLevel(this.world, island.getOwner());

        IslandTopRecord record = new IslandTopRecord(island, level);

        return this.createIslandIcon(template, record, place);
    }
    

    /**
     * This method is used to open UserPanel outside this class. It will be much easier to open panel with single method
     * call then initializing new object.
     *
     * @param addon Level Addon object
     * @param user User who opens panel
     * @param world World where gui is opened
     * @param permissionPrefix Permission Prefix
     */
    public static void openPanel(Level addon, User user, World world, String permissionPrefix)
    {
        new TopLevelPanel(addon, user, world, permissionPrefix).build();
    }


    /**
     * This method gets string value of given permission prefix. If user does not have given permission or it have all
     * (*), then return default value.
     *
     * @param user User who's permission should be checked.
     * @param permissionPrefix Prefix that need to be found.
     * @return String value that follows permissionPrefix.
     */
    private static String getPermissionValue(User user, String permissionPrefix)
    {
        if (user != null && user.isPlayer())
        {
            if (permissionPrefix.endsWith("."))
            {
                permissionPrefix = permissionPrefix.substring(0, permissionPrefix.length() - 1);
            }

            String permPrefix = permissionPrefix + ".";

            List<String> permissions = user.getEffectivePermissions().stream().
                map(PermissionAttachmentInfo::getPermission).
                filter(permission -> permission.startsWith(permPrefix)).
                collect(Collectors.toList());

            for (String permission : permissions)
            {
                if (permission.contains(permPrefix + "*"))
                {
                    // * means all. So continue to search more specific.
                    continue;
                }

                String[] parts = permission.split(permPrefix);

                if (parts.length > 1)
                {
                    return parts[1];
                }
            }
        }

        return null;
    }


// ---------------------------------------------------------------------
// Section: Record
// ---------------------------------------------------------------------


    /**
     * This record is used internally. It converts user -> level to island -> level.
     */
    private record IslandTopRecord(Island island, Long level) {}


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
     * Location to icon permission.
     */
    private final String iconPermission;

    /**
     * List of top 10 island records.
     */
    private final List<IslandTopRecord> topIslands;
}
