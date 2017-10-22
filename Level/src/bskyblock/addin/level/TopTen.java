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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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

import bskyblock.addin.level.config.Settings;
import us.tastybento.bskyblock.BSkyBlock;

/**
 * Handles all Top Ten List functions
 * 
 * @author tastybento
 * 
 */
public class TopTen implements Listener{
    private static Level plugin;
    // Top ten list of players
    private static Map<UUID, Long> topTenList = new HashMap<UUID, Long>();
    private static final int GUISIZE = 27; // Must be a multiple of 9
    private static final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private static final boolean DEBUG = false;
    // Store this as a static because it's the same for everyone and saves memory cleanup
    private static Inventory gui;

    public TopTen(Level plugin) {
        TopTen.plugin = plugin;
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     * 
     * @param ownerUUID
     * @param l
     */
    public static void topTenAddEntry(UUID ownerUUID, long l) {
        // Special case for removals. If a level of zero is given the player
        // needs to be removed from the list
        if (l < 1) {
            if (topTenList.containsKey(ownerUUID)) {
                topTenList.remove(ownerUUID);
            }
            return;
        }
        // Try and see if the player is online
        Player player = plugin.getServer().getPlayer(ownerUUID);
        if (player != null) {
            // Online
            if (!player.hasPermission(Settings.PERMPREFIX + "intopten")) {
                topTenList.remove(ownerUUID);
                return;
            }
        }
        topTenList.put(ownerUUID, l);
        topTenList = MapUtil.sortByValue(topTenList);
    }

    /**
     * Removes ownerUUID from the top ten list
     * 
     * @param ownerUUID
     */
    public static void topTenRemoveEntry(UUID ownerUUID) {
        topTenList.remove(ownerUUID);
    }

    /**
     * Generates a sorted map of islands for the Top Ten list from all player
     * files
     */
    public static void topTenCreate() {
        topTenCreate(null);
    }

    /**
     * Creates the top ten list from scratch. Does not get the level of each island. Just
     * takes the level from the player's file.
     * Runs asynchronously from the main thread.
     * @param sender
     */
    public static void topTenCreate(final CommandSender sender) {
        // TODO
    }

    public static void topTenSave() {
        if (topTenList == null) {
            return;
        }
        plugin.getLogger().info("Saving top ten list");
        // Make file
        File topTenFile = new File(plugin.getDataFolder(), "topten.yml");
        // Make configuration
        YamlConfiguration config = new YamlConfiguration();
        // Save config

        int rank = 0;
        for (Map.Entry<UUID, Long> m : topTenList.entrySet()) {
            if (rank++ == 10) {
                break;
            }
            config.set("topten." + m.getKey().toString(), m.getValue());
        }
        try {
            config.save(topTenFile);
            plugin.getLogger().info("Saved top ten list");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save top ten list!");
            e.printStackTrace();
        }
    }

    /**
     * Loads the top ten from the file system topten.yml. If it does not exist
     * then the top ten is created
     */
    public static void topTenLoad() {
        topTenList.clear();
        // TODO
    }

    /**
     * Displays the Top Ten list if it exists in chat
     * 
     * @param player
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public static boolean topTenShow(final Player player) {

        if (DEBUG)
            plugin.getLogger().info("DEBUG: new GUI display");
        // New GUI display (shown by default)
        if (topTenList == null) topTenCreate();
        topTenList = MapUtil.sortByValue(topTenList);
        // Create the top ten GUI if it does not exist
        if (gui == null) {
            gui = Bukkit.createInventory(null, GUISIZE, plugin.getLocale(player.getUniqueId()).get("topTenGuiTitle"));
            if (DEBUG)
                plugin.getLogger().info("DEBUG: creating GUI for the first time");
        }
        // Reset
        gui.clear();
        int i = 1;
        Iterator<Entry<UUID, Long>> it = topTenList.entrySet().iterator();
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

        return true;
    }

    static ItemStack getSkull(int rank, Long long1, UUID player){
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
        meta.setOwner(playerName);
        meta.setDisplayName((plugin.getLocale(player).get("topTenGuiHeading").replace("[name]", BSkyBlock.getPlugin().getIslands().getIslandName(player))).replace("[rank]", String.valueOf(rank)));
        //meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "<!> " + ChatColor.YELLOW + "Island: " + ChatColor.GOLD + ChatColor.UNDERLINE + plugin.getGrid().getIslandName(player) + ChatColor.GRAY + " (#" + rank + ")");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.YELLOW + plugin.getLocale(player).get("levelislandLevel") + " " + long1);
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

    static void remove(UUID owner) {
        topTenList.remove(owner);
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
        if (!inventory.getTitle().equals(plugin.getLocale(player).get("topTenGuiTitle"))) {
            return;
        }
        event.setCancelled(true);
        player.updateInventory();
        if(event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.SKULL_ITEM) && event.getCurrentItem().hasItemMeta()){
            // TODO warp
            //Util.runCommand(player, "is warp " + ((SkullMeta)event.getCurrentItem().getItemMeta()).getOwner());
            player.closeInventory();
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
     * Get a sorted descending map of the top players
     * @return the topTenList - may be more or less than ten
     */
    public static Map<UUID, Long> getTopTenList() {
        return topTenList;
    }
}
