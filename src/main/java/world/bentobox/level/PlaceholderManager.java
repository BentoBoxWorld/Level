package world.bentobox.level;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.eclipse.jdt.annotation.Nullable;

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
 * Handles registration and resolution of Level placeholders for the Level addon.
 *
 * The class implements:
 * - registering placeholders via the BentoBox PlaceholdersManager
 * - resolving top-ten and per-island level values
 * - mapping blocks/items/spawners to the identifier used by IslandLevels
 *
 */
public class PlaceholderManager {

    private final Level addon;
    private final BentoBox plugin;

    public PlaceholderManager(Level addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();
    }

    /**
     * Register placeholders for a given GameModeAddon.
     *
     * This method registers a number of placeholders with BentoBox's PlaceholdersManager:
     * - island level placeholders (formatted, raw, owner-only)
     * - points / points-to-next-level placeholders
     * - top-ten placeholders (name, island name, members, level) for ranks 1..10
     * - visited island placeholder
     * - mainhand & looking placeholders (value and count)
     * - dynamic placeholders for each configured block key from the BlockConfig
     *
     * The registered placeholders call into the Level manager and IslandLevels to fetch
     * values. Safety checks are performed so that missing players, islands or data return "0"
     * or empty strings rather than throwing exceptions.
     *
     * @param gm the GameModeAddon for which placeholders are being registered
     */
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

                // Register value placeholders
                String placeholder = gm.getDescription().getName().toLowerCase() + "_island_value_" + placeholderSuffix;
                bpm.registerPlaceholder(addon, placeholder,
                    user -> String.valueOf(Objects.requireNonNullElse(
                        // Use the configKey directly, getValue handles String keys
                        addon.getBlockConfig().getValue(gm.getOverWorld(), configKey), 0)) 
                );

                // Register count placeholders
                placeholder = gm.getDescription().getName().toLowerCase() + "_island_count_" + placeholderSuffix;
                 bpm.registerPlaceholder(addon, placeholder,
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
        // Register limit placeholders
        addon.getBlockConfig().getBlockLimits().forEach((configKey, configValue) -> {
         // Format the key for the placeholder name (e.g., minecraft_stone, pig_spawner)
            String placeholderSuffix = configKey.replace(':', '_').replace('.', '_').toLowerCase();
            String placeholder = gm.getDescription().getName().toLowerCase() + "_island_limit_" + placeholderSuffix;
            bpm.registerPlaceholder(addon, placeholder, user -> String.valueOf(configValue));
        });
    }

    /**
     * Get the name of the owner of the island who holds the rank in this world.
     *
     * Behavior / notes:
     * - rank is clamped between 1 and Level.TEN
     * - when weighted == true, the weighted top-ten is used; otherwise the plain top-ten is used
     * - returns an empty string if a rank is not available or owner is null
     *
     * @param world    world to look up the ranking in
     * @param rank     1-based rank (will be clamped)
     * @param weighted whether to use the weighted top-ten
     * @return owner name or empty string
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
     * Get the island name for this rank.
     *
     * Similar behavior to getRankName, but returns the island's name (or empty string).
     *
     * @param world    world to look up the island in
     * @param rank     1-based rank (clamped)
     * @param weighted whether to use the weighted list
     * @return name of island or empty string
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
     * Gets a comma separated string of island member names for a given ranked island.
     *
     * - Members are filtered to those at or above RanksManager.MEMBER_RANK.
     * - Members are sorted by rank descending for consistent ordering.
     * - If the island is missing or has no members, returns an empty string.
     *
     * @param world    world to look up
     * @param rank     rank in the top-ten (1..10)
     * @param weighted whether to use weighted top-ten
     * @return comma-separated member names, or empty string
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
     * Get the level for the rank requested.
     *
     * - Returns a formatted level string using the manager's formatLevel helper.
     * - If a value is missing, manager.formatLevel receives null which should handle the fallback.
     *
     * @param world    world to query
     * @param rank     rank 1..10 (clamped)
     * @param weighted whether to fetch weighted level
     * @return string representation of the level for the rank
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
     * Return the rank of the player in a world.
     *
     * @param world world
     * @param user  player
     * @return rank where 1 is the top rank as a String; returns empty string for null user
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

    /**
     * Return the level for the island the user is currently visiting (if any).
     *
     * @param gm   the GameModeAddon (used to map to the overworld)
     * @param user the user to check
     * @return island level string for the visited island, or empty/ "0" when not applicable
     */
    String getVisitedIslandLevel(GameModeAddon gm, User user) {
        if (user == null || !gm.inWorld(user.getWorld()))
            return "";
        return addon.getIslands().getIslandAt(user.getLocation())
                .map(island -> addon.getManager().getIslandLevelString(gm.getOverWorld(), island.getOwner()))
                .orElse("0");
    }

    /**
     * Gets the most specific identifier object for a block.
     *
     * The identifier is one of:
     * - EntityType for mob spawners (when the spawner block contains a specific spawned type)
     * - Material for regular blocks
     * - null for air or unknown/invalid blocks
     *
     * This is used to map the block to the same identifier the BlockConfig and IslandLevels use.
     *
     * @param block The block to inspect, null-safe
     * @return an EntityType or Material, or null for air/unknown
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
     *
     * This method attempts to:
     * 1) Resolve a specific EntityType for spawner items via BlockStateMeta or a PersistentDataContainer key.
     *    If the exact spawned mob cannot be determined, it returns null for spawner items so counts
     *    are not incorrectly attributed.
     * 2) If ItemsAdder is present, check for custom item Namespaced ID and return it (String).
     * 3) Fallback to returning the Material for block-like items, otherwise null for non-blocks.
     *
     * The return type is one of:
     * - EntityType (specific spawner type)
     * - Material (normal block-type items)
     * - String (custom items IDs like ItemsAdder)
     * - null (air, invalid item, or unidentified spawner item)
     *
     * @param itemStack the item to inspect (may be null)
     * @return EntityType, Material, String, or null
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
     * Convert a configuration key string (from the block config) into the identifier object
     * used by IslandLevels.
     *
     * - Handles "pig_spawner" style keys and resolves them to EntityType where possible.
     * - Resolves namespaced Material keys using Bukkit's Registry.
     * - Returns the original string for custom items (ItemsAdder) when present in registry.
     * - Returns Material.SPAWNER for generic "spawner" key, otherwise null if unresolvable.
     *
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
     * This is a thin wrapper that validates inputs and returns "0" when missing.
     *
     * @param gm GameModeAddon
     * @param user User requesting the count
     * @param identifier The identifier object (EntityType, Material, String)
     * @return String representation of the count (zero when not available)
     */
    private String getBlockCount(GameModeAddon gm, User user, @Nullable Object identifier) {
        if (user == null || identifier == null) {
            return "0";
        }
        return getBlockCountForUser(gm, user, identifier);
    }

    /**
     * Gets the block count for a specific identifier object from IslandLevels.
     *
     * - Fetches the Island for the user and then the IslandLevels data.
     * - IslandLevels stores counts in two maps (mdCount and uwCount) depending on how values
     *   are classified; we add both to provide the complete count.
     * - Returns "0" if island or data is unavailable.
     *
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
