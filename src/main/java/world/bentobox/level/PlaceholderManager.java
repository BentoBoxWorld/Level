package world.bentobox.level;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.eclipse.jdt.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
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
        bpm.registerPlaceholder(addon,gm.getDescription().getName().toLowerCase() + "_island_value_mainhand",
            user -> {
                if (user.getPlayer() == null) return "0";
                ItemStack itemInHand = user.getPlayer().getInventory().getItemInMainHand();
                Object identifier = getItemIdentifier(itemInHand); // Get EntityType, Material, String, or null
                if (identifier == null) return "0";
                // BlockConfig.getValue handles EntityType, Material, String correctly
                return String.valueOf(Objects.requireNonNullElse(
                    addon.getBlockConfig().getValue(gm.getOverWorld(), identifier), 0));
            }
        );
        
        bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_count_mainhand",
            user -> {
                 if (user.getPlayer() == null) return "0";
                 ItemStack itemInHand = user.getPlayer().getInventory().getItemInMainHand();
                 Object identifier = getItemIdentifier(itemInHand); // Get EntityType, Material, String, or null
                 if (identifier == null) return "0";
                 // Pass the actual object identifier to getBlockCount
                 return getBlockCount(gm, user, identifier);
            }
        );

        // Register looking at block placeholders
        bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_value_looking",
            user -> {
                if (user.getPlayer() == null) return "0";
                Block targetBlock = user.getPlayer().getTargetBlockExact(5);
                Object identifier = getBlockIdentifier(targetBlock); // Get EntityType, Material, String, or null
                if (identifier == null) return "0";
                 // BlockConfig.getValue handles EntityType, Material, String correctly
                    return String.valueOf(Objects.requireNonNullElse(
                    addon.getBlockConfig().getValue(gm.getOverWorld(), identifier), 0));
            }
        );

        bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_count_looking",
            user -> {
                if (user.getPlayer() == null) return "0";
                Block targetBlock = user.getPlayer().getTargetBlockExact(5);
                Object identifier = getBlockIdentifier(targetBlock); // Get EntityType, Material, String, or null
                if (identifier == null) return "0";
                 // Pass the actual object identifier to getBlockCount
                 return getBlockCount(gm, user, identifier);
            }
        );

        // Register placeholders for all block materials/types from config
        if (Bukkit.getServer() != null) {
            // Iterate over the String keys defined in the block config's baseline values
            addon.getBlockConfig().getBlockValues().keySet().forEach(configKey -> {
                // configKey is a String like "minecraft:stone", "pig_spawner", "itemsadder:my_custom_block"
                
                // Format the key for the placeholder name (e.g., minecraft_stone, pig_spawner)
                String placeholderSuffix = configKey.replace(':', '_').replace('.', '_').toLowerCase();

                // Register value placeholder
                bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_value_" + placeholderSuffix,
                    user -> String.valueOf(Objects.requireNonNullElse(
                        // Use the configKey directly, getValue handles String keys
                        addon.getBlockConfig().getValue(gm.getOverWorld(), configKey), 0)) 
                );

                // Register count placeholder
                bpm.registerPlaceholder(addon, gm.getDescription().getName().toLowerCase() + "_island_count_" + placeholderSuffix,
                    user -> {
                        // Convert the String configKey back to the expected Object type (EntityType, Material, String)
                        // for IslandLevels lookup.
                        Object identifier = getObjectFromConfigKey(configKey);
                        if (identifier == null) return "0";
                        return getBlockCount(gm, user, identifier);
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
     * Gets the most specific identifier object for a block.
     * NOTE: Does not currently support getting custom block IDs (e.g., ItemsAdder)
     * directly from the Block object due to hook limitations.
     * @param block The block
     * @return EntityType, Material, or null if air/invalid.
     */
    @Nullable
    private Object getBlockIdentifier(@Nullable Block block) {
        if (block == null || block.getType().isAir()) return null; 

        Material type = block.getType();

        // Handle Spawners
        if (type == Material.SPAWNER) {
            BlockState state = block.getState();
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                EntityType spawnedType = spawner.getSpawnedType();
                if (spawnedType != null) {
                    return spawnedType; // Return EntityType
                }
                return Material.SPAWNER; // Return generic spawner material if type unknown
            }
             return Material.SPAWNER; // Return generic spawner material if state invalid
        }

        // Fallback to the Material for regular blocks
        return type;
    }

    /**
     * Gets the most specific identifier object for an ItemStack.
     * Prioritizes standard Bukkit methods for spawners.
     * Adds support for reading "spawnermeta:type" NBT tag via PDC.
     * Returns null for spawners if the specific type cannot be determined.
     * Supports ItemsAdder items.
     * @param itemStack The ItemStack
     * @return EntityType, Material (for standard blocks), String (for custom items),
     *         or null (if air, invalid, or unidentified spawner).
     */
    @Nullable
    private Object getItemIdentifier(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null; // Invalid item
        }

        Material type = itemStack.getType();

        // 1. Handle Spawners
        if (type == Material.SPAWNER) {
            if (itemStack.hasItemMeta()) {
                ItemMeta meta = itemStack.getItemMeta();
                EntityType specificType = null; // Variable to store the result

                // 1a. Try standard BlockStateMeta method FIRST
                if (meta instanceof BlockStateMeta) {
                    BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
                    if (blockStateMeta.hasBlockState()) {
                        BlockState blockState = blockStateMeta.getBlockState();
                        if (blockState instanceof CreatureSpawner) {
                            CreatureSpawner spawner = (CreatureSpawner) blockState;
                            // Get type if standard method works
                            specificType = spawner.getSpawnedType(); 
                        }
                    }
                }

                // 1b. If standard method failed (specificType is still null), try reading PDC tag
                if (specificType == null && meta != null) { // Check meta != null again just in case
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    // Define the key used by SpawnerMeta (and potentially others)
                    NamespacedKey spawnerMetaTypeKey = new NamespacedKey("spawnermeta", "type"); 

                    if (pdc.has(spawnerMetaTypeKey, PersistentDataType.STRING)) {
                        String entityName = pdc.get(spawnerMetaTypeKey, PersistentDataType.STRING);
                         if (entityName != null && !entityName.isEmpty()) {
                            try {
                                // Convert the name (e.g., "ZOMBIE") to EntityType
                                // Use uppercase as EntityType names are uppercase enums
                                specificType = EntityType.valueOf(entityName.toUpperCase()); 
                            } catch (IllegalArgumentException e) {
                                 // Keep specificType as null
                            }
                        }
                    }
                }

                // Return the identified type (from standard or PDC), or null if neither worked
                return specificType; 

            } else {
                 // No ItemMeta at all for the spawner item
                return null; 
            }
        } // End of Spawner handling

        // 2. Handle potential custom items (e.g., ItemsAdder)
         if (addon.isItemsAdder()) {
             Optional<String> customId = ItemsAdderHook.getNamespacedId(itemStack); 
             if (customId.isPresent()) {
                 return customId.get(); // Return the String ID from ItemsAdder
             }
         }

        // 3. Fallback to Material for regular items that represent blocks
        return type.isBlock() ? type : null; 
    }

    /**
     * Helper method to convert a String key from the config (e.g., "pig_spawner", "minecraft:stone")
     * back into the corresponding Object (EntityType, Material, String) used by IslandLevels.
     * @param configKey The key string from block config.
     * @return EntityType, Material, String identifier, or null if not resolvable.
     */
    @Nullable
    private Object getObjectFromConfigKey(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return null;
        }
        
        String lowerCaseKey = configKey.toLowerCase(); // Normalize for checks

        // Check if it's a spawner key (e.g., "pig_spawner")
        // Ensure it's not the generic "minecraft:spawner" or just "spawner"
        if (lowerCaseKey.endsWith("_spawner") && !lowerCaseKey.equals(Material.SPAWNER.getKey().toString()) && !lowerCaseKey.equals("spawner")) {
            String entityTypeName = lowerCaseKey.substring(0, lowerCaseKey.length() - "_spawner".length());
            // Entity types require namespace in modern MC. Assume minecraft if none provided.
            // This might need adjustment if config uses non-namespaced keys for entities.
            NamespacedKey entityKey = NamespacedKey.fromString(entityTypeName); // Allow full key like "minecraft:pig"
            if (entityKey == null && !entityTypeName.contains(":")) { // If no namespace, assume minecraft
                entityKey = NamespacedKey.minecraft(entityTypeName);
            }
             
            if (entityKey != null) {
                EntityType entityType = Registry.ENTITY_TYPE.get(entityKey);
                if (entityType != null) {
                    return entityType;
                }
            }
            return null; // Cannot resolve
        }

        // Check if it's a standard Material key (namespaced)
        NamespacedKey matKey = NamespacedKey.fromString(lowerCaseKey); 
        Material material = null;
        if (matKey != null) {
            material = Registry.MATERIAL.get(matKey);
        }
        // Add check for non-namespaced legacy material names? Might conflict with custom keys. Risky.
        // Example: Material legacyMat = Material.matchMaterial(configKey);
        
        if (material != null) {
            return material;
        }

        // Assume it's a custom String key (e.g., ItemsAdder) if not resolved yet
        if (addon.isItemsAdder() && ItemsAdderHook.isInRegistry(configKey)) { // Use original case key for lookup?
            return configKey; 
        }

        // Final check: maybe it's the generic "spawner" key from config?
        if(lowerCaseKey.equals("spawner")) {
            return Material.SPAWNER;
        }
        return null;
    }

    /**
     * Gets the block count for a specific identifier object in a user's island.
     * @param gm GameModeAddon
     * @param user User requesting the count
     * @param identifier The identifier object (EntityType, Material, String)
     * @return String representation of the count.
     */
    private String getBlockCount(GameModeAddon gm, User user, @Nullable Object identifier) {
        if (user == null || identifier == null) {
            return "0";
        }
        return getBlockCountForUser(gm, user, identifier);
    }

    /**
     * Gets the block count for a specific identifier object from IslandLevels.
     * This now correctly uses EntityType or Material as keys based on `DetailsPanel`'s logic.
     * @param gm GameModeAddon
     * @param user User to get count for
     * @param identifier The identifier object (EntityType, Material, String)
     * @return String representation of the count.
     */
    private String getBlockCountForUser(GameModeAddon gm, User user, Object identifier) {
        Island island = addon.getIslands().getIsland(gm.getOverWorld(), user);
        if (island == null) {
            return "0";
        }

        IslandLevels data = addon.getManager().getLevelsData(island);
        if (data == null) {
            return "0";
        }

        // Get the count based on the type of the identifier
        // Assumes IslandLevels uses EntityType for specific spawners, Material for blocks,
        // and potentially String for custom items, based on DetailsPanel and BlockConfig analysis.
        int count = data.getMdCount().getOrDefault(identifier, 0) + data.getUwCount().getOrDefault(identifier, 0);

        return String.valueOf(count);
    }

}
