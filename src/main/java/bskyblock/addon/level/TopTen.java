package bskyblock.addon.level;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import bskyblock.addin.warps.Warp;
import bskyblock.addon.level.database.object.LevelsData;
import bskyblock.addon.level.database.object.TopTenData;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.panels.ClickType;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDatabase;

/**
 * Handles all Top Ten List functions
 * 
 * @author tastybento
 * 
 */
public class TopTen implements Listener {
    private Level addon;
    // Top ten list of players
    private TopTenData topTenList;
    private final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private final boolean DEBUG = false;
    private BSBDatabase database;
    private AbstractDatabaseHandler<TopTenData> handler;

    @SuppressWarnings("unchecked")
    public TopTen(Level addon) {
        this.addon = addon;
        // Set up database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve the TopTenList class
        // Note that these are saved in the BSkyBlock database
        handler = (AbstractDatabaseHandler<TopTenData>) database.getHandler(TopTenData.class);
        loadTopTen();
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     * 
     * @param ownerUUID
     * @param l
     */
    public void addEntry(UUID ownerUUID, long l) {
        // Try and see if the player is online
        Player player = addon.getServer().getPlayer(ownerUUID);
        if (player != null) {
            // Online
            if (!player.hasPermission(Constants.PERMPREFIX + "intopten")) {
                topTenList.remove(ownerUUID);
                return;
            }
        }
        topTenList.addLevel(ownerUUID, l);
        saveTopTen();
    }

    /**
     * Creates the top ten list from scratch. Does not get the level of each island. Just
     * takes the level from the player's file.
     * Runs asynchronously from the main thread.
     */
    public void create() {
        // Obtain all the levels for each known player
        AbstractDatabaseHandler<LevelsData> levelHandler = addon.getHandler();
        try {
            long index = 0;
            for (LevelsData lv : levelHandler.loadObjects()) {
                if (index++ % 1000 == 0) {
                    addon.getLogger().info("Processed " + index + " players for top ten");
                }
                // Convert to UUID
                UUID playerUUID = UUID.fromString(lv.getUniqueId());
                // Check if the player is an owner or team leader
                if (addon.getIslands().isOwner(playerUUID)) {
                    topTenList.addLevel(playerUUID, lv.getLevel());
                }
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        saveTopTen();
    }

    /**
     * Displays the Top Ten list
     * 
     * @param user
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public boolean getGUI(final User user) {
        if (DEBUG)
            addon.getLogger().info("DEBUG: GUI display");
        // New GUI display (shown by default)
        if (topTenList == null) create();

        PanelBuilder panel = new PanelBuilder()
                .name(user.getTranslation("island.top.gui-title"))
                .user(user);

        int i = 1;
        Iterator<Entry<UUID, Long>> it = topTenList.getTopTen().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> m = it.next();
            UUID topTenUUID = m.getKey();
            if (DEBUG)
                addon.getLogger().info("DEBUG: " + i + ": " + topTenUUID);
            // Remove from TopTen if the player is online and has the permission
            Player entry = addon.getServer().getPlayer(topTenUUID);
            boolean show = true;
            if (entry != null) {
                if (!entry.hasPermission(Constants.PERMPREFIX + "intopten")) {
                    it.remove();
                    show = false;
                }
            } else {
                if (DEBUG)
                    addon.getLogger().info("DEBUG: player not online, so no per check");

            }
            if (show) {
                panel.item(SLOTS[i-1], getHead(i, m.getValue(), topTenUUID, user));
                if (i++ == 10) break;
            }
        }
        panel.build();
        return true;
    }

    /**
     * Get the head panel item
     * @param rank - the top ten rank of this player/team. Can be used in the name of the island for vanity.
     * @param level - the level of the island
     * @param playerUUID - the UUID of the top ten player
     * @param asker - the asker of the top ten
     * @return PanelItem
     */
    private PanelItem getHead(int rank, Long level, UUID playerUUID, User asker) {
        final String name = addon.getPlayers().getName(playerUUID);
        List<String> description = new ArrayList<>();
        if (name != null) {
            description.add(asker.getTranslation("island.top.gui-heading", "[name]", addon.getIslands().getIslandName(playerUUID), "[rank]", String.valueOf(rank)));
            description.add(asker.getTranslation("island.top.island-level","[level]", String.valueOf(level)));
            if (addon.getPlayers().inTeam(playerUUID)) {
                List<String> memberList = new ArrayList<>();
                for (UUID members : addon.getIslands().getMembers(playerUUID)) {
                    memberList.add(ChatColor.AQUA + addon.getPlayers().getName(members));
                }
                description.addAll(memberList);
            }
        }
        PanelItemBuilder builder = new PanelItemBuilder()
                .icon(name)
                .name(name)
                .description(description);

        // If welcome warps is present then add warping
        addon.getAddonByName("BSkyBlock-WelcomeWarps").ifPresent(warp -> {

            if (((Warp)warp).getWarpSignsManager().hasWarp(playerUUID)) {

                builder.clickHandler(new ClickHandler() {

                    @Override
                    public boolean onClick(User user, ClickType click) {
                        if (click.equals(ClickType.LEFT)) {
                            user.sendMessage("island.top.warp-to", "[name]", name);


                            ((Warp)warp).getWarpSignsManager().warpPlayer(user, playerUUID);
                        }
                        return true;
                    }
                });

            } 
        });
        return builder.build();
    }

    public TopTenData getTopTenList() {
        return topTenList;
    }

    /**
     * Loads the top ten from the database
     */
    public void loadTopTen() {
        try {
            topTenList = handler.loadObject("topten");
            if (topTenList == null) {
                topTenList = new TopTenData();
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException e) {
            e.printStackTrace();
        }
    }
    /*
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory(); // The inventory that was clicked in
        if (inventory.getName() == null) {
            return;
        }
        // The player that clicked the item
        Player player = (Player) event.getWhoClicked();
        if (!inventory.getTitle().equals("topten.guiTitle")) {
            return;
        }
        event.setCancelled(true);
        player.updateInventory();
        if(event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.SKULL_ITEM) && event.getCurrentItem().hasItemMeta()){
            player.closeInventory();
            // Fire click event
            TopTenClick clickEvent = new TopTenClick(((SkullMeta)event.getCurrentItem().getItemMeta()).getOwningPlayer().getName());
            addon.getServer().getPluginManager().callEvent(clickEvent);
            return;
        }
        if (event.getSlotType().equals(SlotType.OUTSIDE)) {
            player.closeInventory();
            return;
        }
        if (event.getClick().equals(ClickType.SHIFT_RIGHT)) {
            player.closeInventory();
            return;
        }
    }
     */
    /**
     * Removes ownerUUID from the top ten list
     * 
     * @param ownerUUID
     */
    public void removeEntry(UUID ownerUUID) {
        topTenList.remove(ownerUUID);
    }

    public void saveTopTen() {
        //plugin.getLogger().info("Saving top ten list");
        if (topTenList == null) {
            //plugin.getLogger().info("DEBUG: toptenlist = null!");
            return;
        }
        try {
            handler.saveObject(topTenList);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
