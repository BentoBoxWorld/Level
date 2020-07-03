/**
 *
 */
package world.bentobox.level.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * @author tastybento
 *
 */
public class DetailsGUITab implements Tab, ClickHandler {

    private final Level addon;
    private final World world;
    private final User user;

    public DetailsGUITab(Level addon, World world, User user) {
        this.addon = addon;
        this.world = world;
        this.user = user;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        return true;
    }

    @Override
    public PanelItem getIcon() {
        // TODO Auto-generated method stub
        return new PanelItemBuilder().icon(Material.GRASS_BLOCK).name("Blocks").build();
    }

    @Override
    public String getName() {
        return "Island Details";
    }

    @Override
    public List<@Nullable PanelItem> getPanelItems() {
        // Get the items from the report
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "";
    }

}
