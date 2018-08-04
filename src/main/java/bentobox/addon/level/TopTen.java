package bentobox.addon.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import bentobox.addon.level.database.object.LevelsData;
import bentobox.addon.level.database.object.TopTenData;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.BSBDatabase;

/**
 * Handles all Top Ten List functions
 *
 * @author tastybento
 *
 */
public class TopTen implements Listener {
    private Level addon;
    // Top ten list of players
    private Map<World,TopTenData> topTenList;
    private final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private final boolean DEBUG = false;
    private BSBDatabase<TopTenData> handler;

    public TopTen(Level addon) {
        this.addon = addon;
        // Set up the database handler to store and retrieve the TopTenList class
        // Note that these are saved in the BSkyBlock database
        handler = new BSBDatabase<>(addon, TopTenData.class);
        loadTopTen();
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     *
     * @param ownerUUID
     * @param l
     */
    public void addEntry(World world, UUID ownerUUID, long l) {
        // Check if player is an island owner or not
        if (!addon.getIslands().isOwner(world, ownerUUID)) {
            return;
        }
        // Set up world data
        topTenList.putIfAbsent(world, new TopTenData());
        topTenList.get(world).setUniqueId(world.getName());

        // Try and see if the player is online
        Player player = addon.getServer().getPlayer(ownerUUID);
        if (player != null) {
            // Online
            if (!player.hasPermission(addon.getPlugin().getIWM().getPermissionPrefix(world) + ".intopten")) {
                topTenList.get(world).remove(ownerUUID);
                return;
            }
        }
        topTenList.get(world).addLevel(ownerUUID, l);
    }

    /**
     * Creates the top ten list from scratch. Does not get the level of each island. Just
     * takes the level from the player's file.
     * Runs asynchronously from the main thread.
     */
    public void create(String permPrefix) {
        // Obtain all the levels for each known player
        BSBDatabase<LevelsData> levelHandler = addon.getHandler();
        long index = 0;
        for (LevelsData lv : levelHandler.loadObjects()) {
            if (index++ % 1000 == 0) {
                addon.getLogger().info("Processed " + index + " players for top ten");
            }
            // Convert to UUID
            UUID playerUUID = UUID.fromString(lv.getUniqueId());
            // Get the world
            lv.getLevels().forEach((k,v) -> addEntry(Bukkit.getWorld(k), playerUUID, v));
        }
        saveTopTen();
    }

    /**
     * Displays the Top Ten list
     * @param world
     *
     * @param user
     *            - the requesting player
     * @return - true if successful, false if no Top Ten list exists
     */
    public boolean getGUI(World world, final User user, String permPrefix) {
        // Check world
        topTenList.putIfAbsent(world, new TopTenData());
        topTenList.get(world).setUniqueId(world.getName());
        if (DEBUG)
            addon.getLogger().info("DEBUG: GUI display");

        PanelBuilder panel = new PanelBuilder()
                .name(user.getTranslation("island.top.gui-title"))
                .user(user);

        int i = 1;
        Iterator<Entry<UUID, Long>> it = topTenList.get(world).getTopTen().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> m = it.next();
            UUID topTenUUID = m.getKey();
            if (DEBUG)
                addon.getLogger().info("DEBUG: " + i + ": " + topTenUUID);
            // Remove from TopTen if the player is online and has the permission
            Player entry = addon.getServer().getPlayer(topTenUUID);
            boolean show = true;
            if (entry != null) {
                if (DEBUG)
                    addon.getLogger().info("DEBUG: removing from topten");
                if (!entry.hasPermission(permPrefix + "intopten")) {
                    it.remove();
                    show = false;
                }
            } else {
                if (DEBUG)
                    addon.getLogger().info("DEBUG: player not online, so no per check");

            }
            if (show) {
                panel.item(SLOTS[i-1], getHead(i, m.getValue(), topTenUUID, user, world));
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
    private PanelItem getHead(int rank, Long level, UUID playerUUID, User asker, World world) {
        final String name = addon.getPlayers().getName(playerUUID);
        List<String> description = new ArrayList<>();
        if (name != null) {
            description.add(asker.getTranslation("island.top.gui-heading", "[name]", name, "[rank]", String.valueOf(rank)));
            description.add(asker.getTranslation("island.top.island-level","[level]", String.valueOf(level)));
            if (addon.getIslands().inTeam(world, playerUUID)) {
                List<String> memberList = new ArrayList<>();
                for (UUID members : addon.getIslands().getMembers(world, playerUUID)) {
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
        /*
        addon.getAddonByName("BSkyBlock-WelcomeWarps").ifPresent(warp -> {

            if (((Warp)warp).getWarpSignsManager().hasWarp(world, playerUUID)) {
                builder.clickHandler((panel, user, click, slot) -> {
                    if (click.equals(ClickType.LEFT)) {
                        user.sendMessage("island.top.warp-to", "[name]", name);
                        ((Warp)warp).getWarpSignsManager().warpPlayer(world, user, playerUUID);
                    }
                    return true;
                });
            }
        });*/
        return builder.build();
    }

    public TopTenData getTopTenList(World world) {
        return topTenList.get(world);
    }

    /**
     * Loads all the top tens from the database
     */
    public void loadTopTen() {
        topTenList = new HashMap<>();
        handler.loadObjects().forEach(tt -> topTenList.put(Bukkit.getWorld(tt.getUniqueId()), tt));
    }

    /**
     * Removes ownerUUID from the top ten list
     *
     * @param ownerUUID
     */
    public void removeEntry(World world, UUID ownerUUID) {
        topTenList.putIfAbsent(world, new TopTenData());
        topTenList.get(world).setUniqueId(world.getName());
        topTenList.get(world).remove(ownerUUID);
    }

    public void saveTopTen() {
        topTenList.values().forEach(handler::saveObject);
    }

}
