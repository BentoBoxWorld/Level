package world.bentobox.level;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.Nullable;
import org.bukkit.Bukkit;

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
        // Island Level owner only
        bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_level_owner",
                user -> String.valueOf(addon.getManager().getIslandLevel(gm.getOverWorld(), user.getUniqueId(), true)));
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
                    u -> getRankName(gm.getOverWorld(), rank, false));
            // Island Name
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_island_name_" + i,
                    u -> getRankIslandName(gm.getOverWorld(), rank, false));
            // Members
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_members_" + i,
                    u -> getRankMembers(gm.getOverWorld(), rank, false));
            // Level
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_value_" + i,
                    u -> getRankLevel(gm.getOverWorld(), rank, false));
            // Weighted Level Name (Level / number of members)
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_weighted_name_" + i,
                    u -> getRankName(gm.getOverWorld(), rank, true));
            // Weighted Island Name
            bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_top_weighted_island_name_" + i,
                    u -> getRankIslandName(gm.getOverWorld(), rank, true));
            // Weighted Members
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_weighted_members_" + i,
                    u -> getRankMembers(gm.getOverWorld(), rank, true));
            // Weighted Level (Level / number of members)
            bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_top_weighted_value_" + i,
                    u -> getRankLevel(gm.getOverWorld(), rank, true));
        }

        // Personal rank
        bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_rank_value",
                u -> getRankValue(gm.getOverWorld(), u));

        // Register mainhand placeholders
        bpm.registerPlaceholder(addon,
            gm.getDescription().getName().toLowerCase() + "_island_value_mainhand",
            user -> {
                if (user.getPlayer() == null || !user.getPlayer().getInventory().getItemInMainHand().getType().isBlock()) {
                    return "0";
                }
                String blockName = user.getPlayer().getInventory().getItemInMainHand().getType().getKey().getKey();
                return String.valueOf(Objects.requireNonNullElse(
                    addon.getBlockConfig().getValue(gm.getOverWorld(), blockName),
                    0
                ));
            }
        );
        
        bpm.registerPlaceholder(addon,
            gm.getDescription().getName().toLowerCase() + "_island_count_mainhand",
            user -> {
                if (user.getPlayer() == null || !user.getPlayer().getInventory().getItemInMainHand().getType().isBlock()) {
                    return "0";
                }
                Material material = user.getPlayer().getInventory().getItemInMainHand().getType();
                return getBlockCount(gm, user, material);
            }
        );

        // Register looking at block placeholders
        bpm.registerPlaceholder(addon,
            gm.getDescription().getName().toLowerCase() + "_island_value_looking",
            user -> {
                if (user.getPlayer() == null) return "0";
                var targetBlock = user.getPlayer().getTargetBlock(null, 5);
                if (targetBlock != null && !targetBlock.getType().isAir()) {
                    String blockName = targetBlock.getType().getKey().getKey();
                    return String.valueOf(Objects.requireNonNullElse(
                        addon.getBlockConfig().getValue(gm.getOverWorld(), blockName),
                        0
                    ));
                }
                return "0";
            }
        );

        bpm.registerPlaceholder(addon,
            gm.getDescription().getName().toLowerCase() + "_island_count_looking",
            user -> {
                if (user.getPlayer() == null) return "0";
                var targetBlock = user.getPlayer().getTargetBlock(null, 5);
                if (targetBlock != null && !targetBlock.getType().isAir()) {
                    return getBlockCount(gm, user, targetBlock.getType());
                }
                return "0";
            }
        );

        // Register placeholders for all block materials
        if (Bukkit.getServer() != null) {
            // Get all materials from the block config
            addon.getBlockConfig().getBlockValues().keySet().forEach(blockName -> {
                String formattedName = blockName.replace(':', '_').toLowerCase();

                // Register value placeholder
                bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_island_value_" + formattedName,
                    user -> String.valueOf(Objects.requireNonNullElse(
                        addon.getBlockConfig().getValue(gm.getOverWorld(), blockName),
                        0
                    ))
                );

                // Register count placeholder
                bpm.registerPlaceholder(addon,
                    gm.getDescription().getName().toLowerCase() + "_island_count_" + formattedName,
                    user -> {
                        Material material = Material.valueOf(blockName.toUpperCase());
                        return getBlockCount(gm, user, material);
                    }
                );
            });
        }
    }

    /**
     * Get the name of the owner of the island who holds the rank in this world.
     * 
     * @param world    world
     * @param rank     rank 1 to 10
     * @param weighted if true, then the weighted rank name is returned
     * @return rank name
     */
    String getRankName(World world, int rank, boolean weighted) {
        // Ensure rank is within bounds
        rank = Math.max(1, Math.min(rank, Level.TEN));
        if (weighted) {
            return addon.getManager().getWeightedTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L)
                    .findFirst().map(Island::getOwner).filter(Objects::nonNull).map(addon.getPlayers()::getName)
                    .orElse("");
        }
        @Nullable
        UUID owner = addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L)
                .findFirst().flatMap(addon.getIslands()::getIslandById).filter(island -> island.getOwner() != null) // Filter out null owners
                .map(Island::getOwner).orElse(null);

        return addon.getPlayers().getName(owner);
    }

    /**
     * Get the island name for this rank
     * 
     * @param world    world
     * @param rank     rank 1 to 10
     * @param weighted if true, then the weighted rank name is returned
     * @return name of island or nothing if there isn't one
     */
    String getRankIslandName(World world, int rank, boolean weighted) {
        // Ensure rank is within bounds
        rank = Math.max(1, Math.min(rank, Level.TEN));
        if (weighted) {
            return addon.getManager().getWeightedTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L)
                    .findFirst().filter(island -> island.getName() != null) // Filter out null names
                    .map(Island::getName).orElse("");
        }
        return addon.getManager().getTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L).findFirst()
                .flatMap(addon.getIslands()::getIslandById).filter(island -> island.getName() != null) // Filter out null names
                .map(Island::getName).orElse("");
    }

    /**
     * Gets a comma separated string of island member names
     * 
     * @param world    world
     * @param rank     rank to request
     * @param weighted if true, then the weighted rank name is returned
     * @return comma separated string of island member names
     */
    String getRankMembers(World world, int rank, boolean weighted) {
        // Ensure rank is within bounds
        rank = Math.max(1, Math.min(rank, Level.TEN));
        if (weighted) {
            return addon.getManager().getWeightedTopTen(world, Level.TEN).keySet().stream().skip(rank - 1L).limit(1L)
                    .findFirst()
                    .map(is -> is.getMembers().entrySet().stream().filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).map(Map.Entry::getKey)
                            .map(addon.getPlayers()::getName).collect(Collectors.joining(",")))
                    .orElse("");
        }

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
     * Get the level for the rank requested
     * 
     * @param world    world
     * @param rank     rank wanted
     * @param weighted true if weighted (level/number of team members)
     * @return level for the rank requested
     */
    String getRankLevel(World world, int rank, boolean weighted) {
        // Ensure rank is within bounds
        rank = Math.max(1, Math.min(rank, Level.TEN));
        if (weighted) {
            return addon.getManager().formatLevel(addon.getManager().getWeightedTopTen(world, Level.TEN).values()
                    .stream().skip(rank - 1L).limit(1L).findFirst().orElse(null));
        }
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

    /**
     * Gets the block count for a specific material in a user's island
     * @param gm GameModeAddon
     * @param user User requesting the count
     * @param material Material to count
     * @return String representation of the count
     */
    private String getBlockCount(GameModeAddon gm, User user, Object material) {
        if (user == null) {
            return "0";
        }
        return getBlockCountForUser(gm, user, material);
    }

    /**
     * Gets the block count for a specific material in a user's island
     * @param gm GameModeAddon
     * @param user User to get count for
     * @param material Material to count
     * @return String representation of the count
     */
    private String getBlockCountForUser(GameModeAddon gm, User user, Object material) {
        // Get the island for the user
        Island island = addon.getIslands().getIsland(gm.getOverWorld(), user);
        if (island == null) {
            return "0";
        }

        // Get the level data for the island
        IslandLevels data = addon.getManager().getLevelsData(island);
        if (data == null) {
            return "0";
        }

        // Get the total count from both above sea level and underwater
        int count = data.getMdCount().getOrDefault(material, 0) + data.getUwCount().getOrDefault(material, 0);
        return String.valueOf(count);
    }

}
