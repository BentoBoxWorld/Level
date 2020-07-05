/**
 *
 */
package world.bentobox.level.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.objects.LevelsData;

/**
 * @author tastybento
 *
 */
public class DetailsGUITab implements Tab, ClickHandler {

    public enum DetailsType {
        ABOVE_SEA_LEVEL_BLOCKS,
        ALL_BLOCKS,
        SPAWNERS,
        UNDERWATER_BLOCKS
    }

    /**
     * Converts block materials to item materials
     */
    private static final Map<Material, Material> M2I;
    static {
        Map<Material, Material> m2i = new HashMap<>();
        m2i.put(Material.WATER, Material.WATER_BUCKET);
        m2i.put(Material.LAVA, Material.LAVA_BUCKET);
        m2i.put(Material.AIR, Material.BLACK_STAINED_GLASS_PANE);
        m2i.put(Material.VOID_AIR, Material.BLACK_STAINED_GLASS_PANE);
        m2i.put(Material.CAVE_AIR, Material.BLACK_STAINED_GLASS_PANE);
        m2i.put(Material.WALL_TORCH, Material.TORCH);
        m2i.put(Material.REDSTONE_WALL_TORCH, Material.REDSTONE_TORCH);
        m2i.put(Material.TALL_SEAGRASS, Material.SEAGRASS);
        m2i.put(Material.PISTON_HEAD, Material.PISTON);
        m2i.put(Material.MOVING_PISTON, Material.PISTON);
        m2i.put(Material.REDSTONE_WIRE, Material.REDSTONE);
        m2i.put(Material.NETHER_PORTAL, Material.MAGENTA_STAINED_GLASS_PANE);
        m2i.put(Material.END_PORTAL, Material.BLACK_STAINED_GLASS_PANE);
        m2i.put(Material.ATTACHED_MELON_STEM, Material.MELON_SEEDS);
        m2i.put(Material.ATTACHED_PUMPKIN_STEM, Material.PUMPKIN_SEEDS);
        m2i.put(Material.MELON_STEM, Material.MELON_SEEDS);
        m2i.put(Material.PUMPKIN_STEM, Material.PUMPKIN_SEEDS);
        m2i.put(Material.COCOA, Material.COCOA_BEANS);
        m2i.put(Material.TRIPWIRE, Material.STRING);
        m2i.put(Material.CARROTS, Material.CARROT);
        m2i.put(Material.POTATOES, Material.POTATO);
        m2i.put(Material.BEETROOTS, Material.BEETROOT);
        m2i.put(Material.END_GATEWAY, Material.BEDROCK);
        m2i.put(Material.FROSTED_ICE, Material.ICE);
        m2i.put(Material.KELP_PLANT, Material.KELP);
        m2i.put(Material.BUBBLE_COLUMN, Material.WATER_BUCKET);
        m2i.put(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES);
        m2i.put(Material.BAMBOO_SAPLING, Material.BAMBOO);
        m2i.put(Material.FIRE, Material.FLINT_AND_STEEL);
        // 1.16.1
        if (Enums.getIfPresent(Material.class, "WEEPING_VINES_PLANT").isPresent()) {
            m2i.put(Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES);
            m2i.put(Material.TWISTING_VINES_PLANT, Material.TWISTING_VINES);
            m2i.put(Material.SOUL_WALL_TORCH, Material.SOUL_TORCH);
        }


        M2I = Collections.unmodifiableMap(m2i);
    }
    private final Level addon;
    private final @Nullable Island island;
    private List<PanelItem> items;
    private DetailsType type;
    private final User user;
    private final World world;

    public DetailsGUITab(Level addon, World world, User user, DetailsType type) {
        this.addon = addon;
        this.world = world;
        this.user = user;
        this.island = addon.getIslands().getIsland(world, user);
        this.type = type;
        // Generate report
        generateReport(type);
    }

    private void createItem(Material m, Integer count) {
        if (count == null || count <= 0) return;
        // Convert walls
        m = Enums.getIfPresent(Material.class, m.name().replace("WALL_", "")).or(m);
        // Tags
        if (Enums.getIfPresent(Material.class, "SOUL_CAMPFIRE").isPresent()) {
            if (Tag.FIRE.isTagged(m)) {
                items.add(new PanelItemBuilder()
                        .icon(Material.CAMPFIRE)
                        .name(Util.prettifyText(m.name()) + " x " + count)
                        .build());
                return;
            }
        }
        if (Tag.FLOWER_POTS.isTagged(m)) {
            m = Enums.getIfPresent(Material.class, m.name().replace("POTTED_", "")).or(m);
        }
        items.add(new PanelItemBuilder()
                .icon(M2I.getOrDefault(m, m))
                .name(user.getTranslation("island.level-details.syntax", TextVariables.NAME,
                        Util.prettifyText(m.name()), TextVariables.NUMBER, String.valueOf(count)))
                .build());

    }

    private void generateReport(DetailsType type) {
        items = new ArrayList<>();
        LevelsData ld = addon.getManager().getLevelsData(island.getOwner());
        // Get the items from the report
        Map<Material, Integer> sumTotal = new HashMap<>();
        sumTotal.putAll(ld.getMdCount(world));
        sumTotal.putAll(ld.getUwCount(world));
        switch(type) {
        case ABOVE_SEA_LEVEL_BLOCKS:
            ld.getMdCount(world).forEach(this::createItem);
            break;
        case SPAWNERS:
            sumTotal.entrySet().stream().filter(m -> m.getKey().equals(Material.SPAWNER)).forEach(e -> createItem(e.getKey(), e.getValue()));
            break;
        case UNDERWATER_BLOCKS:
            ld.getUwCount(world).forEach(this::createItem);
            break;
        default:
            sumTotal.forEach(this::createItem);
            break;
        }
        if (type.equals(DetailsType.ALL_BLOCKS) && items.isEmpty()) {
            // Nothing here - looks like they need to run level
            items.add(new PanelItemBuilder()
                    .name(user.getTranslation("island.level-details.hint")).icon(Material.WRITTEN_BOOK)
                    .build());
        }
    }

    @Override
    public PanelItem getIcon() {
        switch(type) {
        case ABOVE_SEA_LEVEL_BLOCKS:
            return new PanelItemBuilder().icon(Material.GRASS_BLOCK).name(user.getTranslation("island.level-details.above-sea-level-blocks")).build();
        case SPAWNERS:
            return new PanelItemBuilder().icon(Material.SPAWNER).name(user.getTranslation("island.level-details.spawners")).build();
        case UNDERWATER_BLOCKS:
            return new PanelItemBuilder().icon(Material.WATER_BUCKET).name(user.getTranslation("island.level-details.underwater-blocks")).build();
        default:
            return new PanelItemBuilder().icon(Material.GRASS_BLOCK).name(user.getTranslation("island.level-details.all-blocks")).build();
        }
    }

    @Override
    public String getName() {
        String name = user.getTranslation("island.level-details.no-island");
        if (island.getOwner() != null) {
            name = island.getName() != null ? island.getName() :
                user.getTranslation("island.level-details.names-island", TextVariables.NAME, addon.getPlayers().getName(island.getOwner()));
        }
        return name;
    }

    @Override
    public List<@Nullable PanelItem> getPanelItems() {
        return items;
    }

    @Override
    public String getPermission() {
        String permPrefix = addon.getPlugin().getIWM().getPermissionPrefix(world);
        switch(type) {
        case ABOVE_SEA_LEVEL_BLOCKS:
            return permPrefix + "island.level.details.above-sea-level";
        case SPAWNERS:
            return permPrefix + "island.level.details.spawners";
        case UNDERWATER_BLOCKS:
            return permPrefix + "island.level.details.underwater";
        default:
            return permPrefix + "island.level.details.blocks";

        }
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        return true;
    }

}
