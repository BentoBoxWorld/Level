package world.bentobox.level;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;

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
 * 
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
	if (plugin.getPlaceholdersManager() == null)
	    return;
	PlaceholdersManager bpm = plugin.getPlaceholdersManager();
	// Island Level
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_level",
		user -> addon.getManager().getIslandLevelString(gm.getOverWorld(), user.getUniqueId()));
	// Unformatted island level
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_level_raw",
		user -> String.valueOf(addon.getManager().getIslandLevel(gm.getOverWorld(), user.getUniqueId())));
	// Total number of points counted before applying level formula
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_total_points", user -> {
	    IslandLevels data = addon.getManager().getLevelsData(addon.getIslands().getIsland(gm.getOverWorld(), user));
	    return data.getTotalPoints() + "";
	});
	// Points to the next level for player
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_points_to_next_level",
		user -> addon.getManager().getPointsToNextString(gm.getOverWorld(), user.getUniqueId()));
	// Maximum level this island has ever been. Current level maybe lower.
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_level_max",
		user -> String.valueOf(addon.getManager().getIslandMaxLevel(gm.getOverWorld(), user.getUniqueId())));

	// Visited Island Level
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_visited_island_level",
		user -> getVisitedIslandLevel(gm, user));

	// Register Top Ten Placeholders
	for (int i = 1; i < 11; i++) {
	    final int rank = i;
	    // Name
	    bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_name_" + i,
		    u -> getRankName(gm.getOverWorld(), rank));
	    // Island Name
	    bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_island_name_" + i,
		    u -> getRankIslandName(gm.getOverWorld(), rank));
	    // Members
	    bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_members_" + i,
		    u -> getRankMembers(gm.getOverWorld(), rank));
	    // Level
	    bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_value_" + i,
		    u -> getRankLevel(gm.getOverWorld(), rank));
	    // Weighted Level (Level / number of members)
	    bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_weighted_value_" + i,
		    u -> getWeightedRankLevel(gm.getOverWorld(), rank));
	}

	// Personal rank
	bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_rank_value",
		u -> getRankValue(gm.getOverWorld(), u));
    }

    /**
     * Get the name of the owner of the island who holds the rank in this world.
     * 
     * @param world world
     * @param rank  rank 1 to 10
     * @return rank name
     */
    String getRankName(World world, int rank) {
	// Ensure rank is within bounds
	rank = Math.max(1, Math.min(rank, Level.TEN));
	@Nullable
	UUID owner = addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L)
		.findFirst().flatMap(addon.getIslands()::getIslandById).map(Island::getOwner).orElse(null);

	return addon.getPlayers().getName(owner);
    }

    /**
     * Get the island name for this rank
     * 
     * @param world world
     * @param rank  rank 1 to 10
     * @return name of island or nothing if there isn't one
     */
    String getRankIslandName(World world, int rank) {
	// Ensure rank is within bounds
	rank = Math.max(1, Math.min(rank, Level.TEN));
	return addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst()
		.flatMap(addon.getIslands()::getIslandById).map(Island::getName).orElse("");
    }

    /**
     * Gets a comma separated string of island member names
     * 
     * @param world world
     * @param rank  rank to request
     * @return comma separated string of island member names
     */
    String getRankMembers(World world, int rank) {
	// Ensure rank is within bounds
	rank = Math.max(1, Math.min(rank, Level.TEN));
	Optional<Island> island = addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L)
		.limit(1L).findFirst().flatMap(addon.getIslands()::getIslandById);

	if (island.isPresent()) {
	    // Sort members by rank
	    return island.get().getMembers().entrySet().stream().filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
		    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey)
		    .map(addon.getPlayers()::getName).collect(Collectors.joining(","));
	}
	return "";
    }

    /**
     * Gets the weighted level, which is the level / number of players
     * 
     * @param world world
     * @param rank  level
     * @return weighted level
     */
    String getWeightedRankLevel(World world, int rank) {
	// Ensure rank is within bounds
	rank = Math.max(1, Math.min(rank, Level.TEN));

	// Retrieve the top ten entries
	Map<String, Long> topTen = addon.getManager().getTopTen(world, Level.TEN);
	if (topTen.isEmpty()) {
	    return "";
	}

	// Find the entry corresponding to the rank
	Entry<String, Long> entry = topTen.entrySet().stream().skip(rank - 1).findFirst().orElse(null);
	if (entry == null) {
	    return "";
	}

	// Calculate the score
	Island island = addon.getIslands().getIslandById(entry.getKey()).orElse(null);
	if (island == null || island.getMemberSet().isEmpty()) {
	    return "";
	}

	double score = (double) entry.getValue() / island.getMemberSet().size();

	// Format and return the level
	return addon.getManager().formatLevel((long) score);

    }

    String getRankLevel(World world, int rank) {
	// Ensure rank is within bounds
	rank = Math.max(1, Math.min(rank, Level.TEN));
	return addon.getManager().formatLevel(addon.getManager().getTopTen(world, Level.TEN).values().stream()
		.skip(rank - 1L).limit(1L).findFirst().orElse(null));
    }

    /**
     * Return the rank of the player in a world
     * 
     * @param world world
     * @param user  player
     * @return rank where 1 is the top rank.
     */
    private String getRankValue(World world, User user) {
	if (user == null) {
	    return "";
	}
	// Get the island level for this user
	long level = addon.getManager().getIslandLevel(world, user.getUniqueId());
	return String.valueOf(addon.getManager().getTopTenLists().getOrDefault(world, new TopTenData(world)).getTopTen()
		.values().stream().filter(l -> l > level).count() + 1);
    }

    String getVisitedIslandLevel(GameModeAddon gm, User user) {
	if (user == null || !gm.inWorld(user.getWorld()))
	    return "";
	return addon.getIslands().getIslandAt(user.getLocation())
		.map(island -> addon.getManager().getIslandLevelString(gm.getOverWorld(), island.getOwner()))
		.orElse("0");
    }

}
