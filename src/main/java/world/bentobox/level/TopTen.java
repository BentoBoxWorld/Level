package world.bentobox.level;

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

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.level.objects.TopTenData;

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
    private Database<TopTenData> handler;

    public TopTen(Level addon) {
        this.addon = addon;
        // Set up the database handler to store and retrieve the TopTenList class
        // Note that these are saved in the BSkyBlock database
        handler = new Database<>(addon, TopTenData.class);
        loadTopTen();
    }

    /**
     * Loads all the top tens from the database
     */
    private void loadTopTen() {
        topTenList = new HashMap<>();
        handler.loadObjects().forEach(tt -> {
            World world = Bukkit.getWorld(tt.getUniqueId());
            if (world != null) {
                topTenList.put(world, tt);
            } else {
                addon.logError("TopTen world " + tt.getUniqueId() + " is not known on server. Skipping...");
            }
        });
    }

    /**
     * Adds a player to the top ten, if the level is good enough
     *
     * @param ownerUUID - owner UUID
     * @param l - level
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
        Player player = Bukkit.getServer().getPlayer(ownerUUID);
        if (player != null && !player.hasPermission(addon.getPlugin().getIWM().getPermissionPrefix(world) + "intopten")) {
            topTenList.get(world).remove(ownerUUID);
            return;
        }
        topTenList.get(world).addLevel(ownerUUID, l);
    }

    /**
     * Displays the Top Ten list
     * @param world - world
     * @param user - the requesting player
     */
    public void getGUI(World world, final User user, String permPrefix) {
        // Check world
        topTenList.putIfAbsent(world, new TopTenData());
        topTenList.get(world).setUniqueId(world.getName());

        PanelBuilder panel = new PanelBuilder()
                .name(user.getTranslation("island.top.gui-title"))
                .user(user);

        int i = 1;
        Iterator<Entry<UUID, Long>> it = topTenList.get(world).getTopTen().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> m = it.next();
            UUID topTenUUID = m.getKey();
            // Remove from TopTen if the player is online and has the permission
            Player entry = Bukkit.getServer().getPlayer(topTenUUID);
            boolean show = true;
            if (entry != null) {
                if (!entry.hasPermission(permPrefix + "intopten")) {
                    it.remove();
                    show = false;
                }
            }
            if (show) {
                panel.item(SLOTS[i-1], getHead(i, m.getValue(), topTenUUID, user, world));
                if (i++ == 10) break;
            }
        }
        panel.build();
    }

    /**
     * Get the head panel item
     * @param rank - the top ten rank of this player/team. Can be used in the name of the island for vanity.
     * @param level - the level of the island
     * @param playerUUID - the UUID of the top ten player
     * @param asker - the asker of the top ten
     * @return PanelItem
     */
    private PanelItem getHead(int rank, long level, UUID playerUUID, User asker, World world) {
        String playerName = addon.getPlayers().getName(playerUUID);
        String name = "";
        if (addon.getIslands().hasIsland(world, playerUUID)) {
            name = addon.getIslands().getIsland(world, playerUUID).getName();
        }         
        if (name == null) {
            name = playerName;
        }    
        name = asker.getTranslation("island.top.gui-heading", "[name]", name, "[rank]", String.valueOf(rank));        
        List<String> description = new ArrayList<>();
       
        description.add(asker.getTranslation("island.top.island-level","[level]", addon.getLevelPresenter().getLevelString(level)));
        if (addon.getIslands().inTeam(world, playerUUID)) {
            List<String> memberList = new ArrayList<>();
            for (UUID members : addon.getIslands().getMembers(world, playerUUID)) {
                memberList.add(ChatColor.AQUA + addon.getPlayers().getName(members));
            }
            description.addAll(memberList);
        }
        
        PanelItemBuilder builder = new PanelItemBuilder()
                .icon(playerName)
                .name(name)
                .description(description);
        return builder.build();
    }

    /**
     * Get the top ten list for this world
     * @param world - world
     * @return top ten data object
     */
    public TopTenData getTopTenList(World world) {
        topTenList.putIfAbsent(world, new TopTenData());
        return topTenList.get(world);
    }

    /**
     * Removes ownerUUID from the top ten list
     *
     * @param ownerUUID - uuid to remove
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
