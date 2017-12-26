/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package bskyblock.addin.level;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import bskyblock.addin.level.database.object.Levels;
import bskyblock.addin.level.database.object.TopTenList;
import bskyblock.addin.level.event.TopTenClick;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

/**
 * Handles all Top Ten List functions
 * 
 * @author tastybento
 * 
 */
public class TopTen implements Listener {
    private Level plugin;
    // Top ten list of players
    private TopTenList topTenList;
    private final int GUISIZE = 27; // Must be a multiple of 9
    private final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private final boolean DEBUG = false;
    // Store this as a because it's the same for everyone and saves memory cleanup
    private Inventory gui;
    private BSBDatabase database;
    private AbstractDatabaseHandler<TopTenList> handler;

    @SuppressWarnings("unchecked")
    public TopTen(Level plugin) {
        this.plugin = plugin;
        // Set up database
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve the TopTenList class
        // Note that these are saved in the BSkyBlock database
        handler = (AbstractDatabaseHandler<TopTenList>) database.getHandler(BSkyBlock.getPlugin(), TopTenList.class);
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
        Player player = plugin.getServer().getPlayer(ownerUUID);
        if (player != null) {
            // Online
            if (!player.hasPermission(Settings.PERMPREFIX + "intopten")) {
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
        AbstractDatabaseHandler<Levels> levelHandler = plugin.getHandler();
        try {
            long index = 0;
            for (Levels lv : levelHandler.loadObjects()) {
                if (index++ % 1000 == 0) {
                    plugin.getLogger().info("Processed " + index + " players for top ten");
                }
                // Convert to UUID
                UUID playerUUID = UUID.fromString(lv.getUniqueId());
                // Check if the player is an owner or team leader
                if (BSkyBlock.getPlugin().getIslands().isOwner(playerUUID)) {
                    topTenList.addLevel(playerUUID, lv.getLevel());
                }
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        saveTopTen();
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public boolean getGUI(final Player player) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: GUI display");
        // New GUI display (shown by default)
        if (topTenList == null) create();
        // Create the top ten GUI if it does not exist
        if (gui == null) {
            gui = Bukkit.createInventory(null, GUISIZE, "topten.guiTitle");
            if (DEBUG)
                plugin.getLogger().info("DEBUG: creating GUI for the first time");
        }
        // Reset
        gui.clear();
        int i = 1;
        Iterator<Entry<UUID, Long>> it = topTenList.getTopTen().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> m = it.next();
            UUID playerUUID = m.getKey();
            if (DEBUG)
                plugin.getLogger().info("DEBUG: " + i + ": " + playerUUID);
            // Remove from TopTen if the player is online and has the permission
            Player entry = plugin.getServer().getPlayer(playerUUID);
            boolean show = true;
            if (entry != null) {
                if (!entry.hasPermission(Settings.PERMPREFIX + "intopten")) {
                    it.remove();
                    show = false;
                }
            } else {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: player not online, so no per check");

            }
            if (show) {
                gui.setItem(SLOTS[i-1], getSkull(i, m.getValue(), playerUUID));
                if (i++ == 10) break;
            }
        }

        player.openInventory(gui);
        player.updateInventory();

        return true;
    }

    private ItemStack getSkull(int rank, Long long1, UUID player){
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Getting the skull");
        String playerName = BSkyBlock.getPlugin().getPlayers().getName(player);
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: playername = " + playerName);

            plugin.getLogger().info("DEBUG: second chance = " + BSkyBlock.getPlugin().getPlayers().getName(player));
        }
        ItemStack playerSkull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (playerName == null) return null;
        SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
        //meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(player));
        meta.setOwner(playerName);
        meta.setDisplayName(("topten.guiHeading".replace("[name]", BSkyBlock.getPlugin().getIslands().getIslandName(player))).replace("[rank]", String.valueOf(rank)));
        //meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "<!> " + ChatColor.YELLOW + "Island: " + ChatColor.GOLD + ChatColor.UNDERLINE + plugin.getGrid().getIslandName(player) + ChatColor.GRAY + " (#" + rank + ")");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.YELLOW + "topten.islandLevel".replace("[level]", String.valueOf(long1)));
        if (BSkyBlock.getPlugin().getPlayers().inTeam(player)) {
            List<String> memberList = new ArrayList<>();
            for (UUID members : BSkyBlock.getPlugin().getIslands().getMembers(player)) {
                memberList.add(ChatColor.AQUA + BSkyBlock.getPlugin().getPlayers().getName(members));
            }
            lore.addAll(memberList);
        }
        //else lore.add(ChatColor.AQUA + playerName);

        meta.setLore(lore);
        playerSkull.setItemMeta(meta);
        return playerSkull;
    }

    public TopTenList getTopTenList() {
        return topTenList;
    }

    /**
     * Loads the top ten from the database
     */
    public void loadTopTen() {
        try {
            topTenList = handler.loadObject("topten");
            if (topTenList == null) {
                topTenList = new TopTenList();
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException | SQLException e) {
            e.printStackTrace();
        }
    }

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
            plugin.getServer().getPluginManager().callEvent(clickEvent);
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
                | InstantiationException | NoSuchMethodException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
