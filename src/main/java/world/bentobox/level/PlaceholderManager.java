package world.bentobox.level;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.objects.TopTenData;

/**
 * Handles Level placeholders
 * @author tastybento
 *
 */
public class PlaceholderManager {

    private final Level addon;
    private final BentoBox plugin;

    public PlaceholderManager(Level addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();
    }
    
    protected void registerPlaceholders(GameModeAddon gm) {
        if (plugin.getPlaceholdersManager() == null) return;
        PlaceholdersManager bpm = plugin.getPlaceholdersManager();
        // Island Level
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_island_level",
                user -> addon.getManager().getIslandLevelString(gm.getOverWorld(), user.getUniqueId()));
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_island_level_raw",
                user -> String.valueOf(addon.getManager().getIslandLevel(gm.getOverWorld(), user.getUniqueId())));
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_island_total_points",
                user -> {
                IslandLevels data = addon.getManager().getLevelsData(addon.getIslands().getIsland(gm.getOverWorld(), user));
                return data.getTotalPoints()+"";
            });

        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_points_to_next_level",
                user -> addon.getManager().getPointsToNextString(gm.getOverWorld(), user.getUniqueId()));
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_island_level_max",
                user -> String.valueOf(addon.getManager().getIslandMaxLevel(gm.getOverWorld(), user.getUniqueId())));

        // Visited Island Level
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_visited_island_level", user -> getVisitedIslandLevel(gm, user));

        // Register Top Ten Placeholders
        for (int i = 1; i < 11; i++) {
            final int rank = i;
            // Name
            bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_top_name_" + i, u -> getRankName(gm.getOverWorld(), rank));
            // Island Name
            bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_top_island_name_" + i, u -> getRankIslandName(gm.getOverWorld(), rank));
            // Members
            bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_top_members_" + i, u -> getRankMembers(gm.getOverWorld(), rank));
            // Level
            bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_top_value_" + i, u -> getRankLevel(gm.getOverWorld(), rank));
        }

        // Personal rank
        bpm.registerPlaceholder(addon,
                gm.getDescription().getName().toLowerCase() + "_rank_value", u -> getRankValue(gm.getOverWorld(), u));
    }

    /**
     * Get the name of the player who holds the rank in this world
     * @param world world
     * @param rank rank 1 to 10
     * @return rank name
     */
    String getRankName(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > Level.TEN) rank = Level.TEN;
        return addon.getPlayers().getName(addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst().orElse(null));
    }

    /**
     * Get the island name for this rank
     * @param world world
     * @param rank rank 1 to 10
     * @return name of island or nothing if there isn't one
     */
    String getRankIslandName(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > Level.TEN) rank = Level.TEN;
        UUID owner = addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst().orElse(null);
        if (owner != null) {
            Island island = addon.getIslands().getIsland(world, owner);
            if (island != null) {
                return island.getName() == null ? "" : island.getName();
            }
        }
        return "";
    }

    /**
     * Gets a comma separated string of island member names
     * @param world world
     * @param rank rank to request
     * @return comma separated string of island member names
     */
    String getRankMembers(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > Level.TEN) rank = Level.TEN;
        UUID owner = addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst().orElse(null);
        if (owner != null) {
            Island island = addon.getIslands().getIsland(world, owner);
            if (island != null) {
                // Sort members by rank
                return island.getMembers().entrySet().stream()
                        .filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .map(Map.Entry::getKey)
                        .map(addon.getPlayers()::getName)
                        .collect(Collectors.joining(","));
            }
        }
        return "";
    }

    String getRankLevel(World world, int rank) {
        if (rank < 1) rank = 1;
        if (rank > Level.TEN) rank = Level.TEN;
        return addon.getManager()
                .formatLevel(addon.getManager()
                        .getTopTen(world, Level.TEN)
                        .values()
                        .stream()
                        .skip(rank - 1L)
                        .limit(1L)
                        .findFirst()
                        .orElse(null));
    }
    
    /**
     * Return the rank of the player in a world
     * @param world world
     * @param user player
     * @return rank where 1 is the top rank.
     */
    private String getRankValue(World world, User user) {
        if (user == null) {
            return "";
        }
        // Get the island level for this user
        long level = addon.getManager().getIslandLevel(world, user.getUniqueId());
        return String.valueOf(addon.getManager().getTopTenLists().getOrDefault(world, new TopTenData(world)).getTopTen().values().stream().filter(l -> l > level).count() + 1);
    }

    String getVisitedIslandLevel(GameModeAddon gm, User user) {
        if (user == null || !gm.inWorld(user.getWorld())) return "";
        return addon.getIslands().getIslandAt(user.getLocation())
                .map(island -> addon.getManager().getIslandLevelString(gm.getOverWorld(), island.getOwner()))
                .orElse("0");
    }

}
