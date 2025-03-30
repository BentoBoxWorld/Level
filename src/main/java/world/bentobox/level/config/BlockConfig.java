package world.bentobox.level.config;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.level.Level;

/**
 * Contains all the block values, limits and world differences
 * @author tastybento
 *
 */
public class BlockConfig {

    public static final String SPAWNER = "_spawner";
    private Map<String, Integer> blockLimits = new HashMap<>();
    private Map<String, Integer> blockValues = new HashMap<>();
    private final Map<World, Map<String, Integer>> worldBlockValues = new HashMap<>();
    private final Map<World, Map<EntityType, Integer>> worldSpawnerValues = new HashMap<>();
    private final List<String> hiddenBlocks;
    private Map<EntityType, Integer> spawnerValues = new EnumMap<>(EntityType.class);
    private Level addon;

    /**
     * Loads block limits, values and world settings and then saves them again
     * @param addon - addon
     * @param blockValuesConfig - yaml configuration file containing the block values
     * @param file - the file representing the yaml config. Will be saved after readong.
     * @throws IOException - if there is an error
     */
    public BlockConfig(Level addon, YamlConfiguration blockValuesConfig, File file) throws IOException {
        this.addon = addon;
        if (blockValuesConfig.isConfigurationSection("limits")) {
            ConfigurationSection limits = blockValuesConfig.getConfigurationSection("limits");
            for (String key : limits.getKeys(false)) {
                // Convert old materials to namespaced keys
                key = convertKey(limits, key);
                blockLimits.put(key, limits.getInt(key));
            }
        }
        // The blocks section can include blocks, spawners, and namespacedIDs
        if (blockValuesConfig.isConfigurationSection("blocks")) {
            ConfigurationSection blocks = blockValuesConfig.getConfigurationSection("blocks");
            for (String key : blocks.getKeys(false)) {
                // Convert old materials to namespaced keys
                key = convertKey(blocks, key);
                // Validate
                if (isMaterial(key) || isSpawner(key) || isOther(key)) {
                    // Store for lookup
                    this.blockValues.put(key, blocks.getInt(key));
                } else {
                    addon.logError("Unknown listing in blocks section: " + key);
                }
            }
            // Add missing items to the list
            addMissing(blocks);
        }

        // Worlds
        if (blockValuesConfig.isConfigurationSection("worlds")) {
            loadWorlds(blockValuesConfig);
        }
        // Hidden
        hiddenBlocks = blockValuesConfig.getStringList("hidden-blocks").stream().map(this::convert).toList();
        blockValuesConfig.set("hidden-blocks", hiddenBlocks); // Update

        // All done
        blockValuesConfig.save(file);
    }

    private void addMissing(ConfigurationSection blocks) {
        // Add missing materials
        Registry.MATERIAL.stream()
                .filter(m -> m.isBlock()).filter(m -> !m.isAir())
                .filter(m -> !blockValues.containsKey(m.getKey().getKey()))
                .forEach(m -> blocks.set(m.getKey().getKey(), 1)); // Add a default value of 1
        // Add missing spawners
        Registry.MATERIAL.stream().filter(Material::isItem).filter(m -> m.name().endsWith("_SPAWN_EGG")) // Get potential spawners by looking up spawn eggs, which are how a spawner can be set
                .map(m -> m.getKey().getKey().substring(0, m.name().length() - 10) + SPAWNER) // Change the name of the egg to "entity-type_spawner"
                .filter(s -> !this.blockValues.containsKey(s)) // Check if the blockValues map contains this spawner
                .forEach(m -> blocks.set(m, 1)); // Add a default value of 1
    }

    private boolean isOther(String key) {
        // Maybe a custom name space
        return addon.isItemsAdder() && ItemsAdderHook.isInRegistry(key);
    }

    private boolean isSpawner(String key) {
        if (key.endsWith(SPAWNER)) {
            // Spawner
            String name = key.substring(0, key.length() - 8);
            return Registry.ENTITY_TYPE.get(NamespacedKey.fromString(name)) != null;
        }
        return false;
    }

    private boolean isMaterial(String key) {
        return Registry.MATERIAL.get(NamespacedKey.fromString(key)) != null;
    }

    /**
     * Converts old to new
     * @param blocks 
     * @param key key
     * @return new key
     */
    private String convertKey(ConfigurationSection blocks, String key) {
        int value = blocks.getInt(key);
        blocks.set(key, null); // Delete the old entry
        key = convert(key);
        blocks.set(key, value); // set the value
        return key;
    }

    private String convert(String key) {
        Material m = Material.getMaterial(key);
        if (m != null) {
            // Old material
            key = m.getKey().getKey();
        }
        // Convert old spawners
        if (key.endsWith("_SPAWNER")) {
            // Old spawner, convert to entity type name space
            key = key.toLowerCase(Locale.ENGLISH);
        }
        return key;
    }

    private void loadWorlds(YamlConfiguration blockValues2) {
        ConfigurationSection worlds = Objects.requireNonNull(blockValues2.getConfigurationSection("worlds"));
        for (String world : Objects.requireNonNull(worlds).getKeys(false)) {
            World bWorld = Bukkit.getWorld(world);
            if (bWorld != null) {
                ConfigurationSection blocks = worlds.getConfigurationSection(world);
                Map<String, Integer> values = worldBlockValues.getOrDefault(bWorld, new HashMap<>());
                for (String key : blocks.getKeys(false)) {
                    // Convert old materials to namespaced keys
                    key = convertKey(blocks, key);
                    // Validate
                    if (isMaterial(key) || isSpawner(key) || isOther(key)) {
                        // Store for lookup
                        values.put(key, blocks.getInt(key));
                    } else {
                        addon.logError("Unknown listing in blocks section: " + key);
                    }
                }
                worldBlockValues.put(bWorld, values);
            } else {
                addon.log("Level Addon: No such world in blockconfig.yml : " + world);
            }
        }

    }

    /**
     * Return the limits for any particular material or entity type
     * @param obj material, entity type, or namespacedId
     * @return the limit or null if there isn't one
     */
    public Integer getLimit(Object obj) {
        if (obj instanceof Material m) {
            return blockLimits.get(m.name());
        }
        if (obj instanceof EntityType et) {
            return blockLimits.get(et.name().concat(SPAWNER));
        }
        if (obj instanceof String s) {
            return blockLimits.get(s);
        }
        return null;
    }

    /**
     * @return the worldSpawnerValues
     */
    public Map<World, Map<EntityType, Integer>> getWorldSpawnerValues() {
        return worldSpawnerValues;
    }

    /**
     * Retrieves the value associated with a spawner in the specified world,
     * using world-specific settings if available, or falling back to baseline values.
     *
     * @param world the world context
     * @param obj   the object representing the entity type or material
     * @return the corresponding value, or null if no value is configured
     */
    public Integer getValue(World world, Object obj) {
        // Extract the key based on the type of obj
        String key = switch (obj) {
        case Keyed keyed -> keyed.getKey().getKey();
        case String str -> str;
        default -> "";
        };

        if (key.isEmpty()) {
            return null;
        }

        if (obj instanceof EntityType) {
            key = key.concat(SPAWNER);
        }

        // Try to get the world-specific value first
        Map<String, Integer> worldValues = getWorldBlockValues().get(world);
        if (worldValues != null) {
            Integer value = worldValues.get(key);
            if (value != null) {
                return value;
            }
        }

        // Fall back to the baseline value
        return getBlockValues().get(key);
    }

    /**
     * Return true if the block should be hidden
     * @param m block material or entity type of spanwer
     * @return true if hidden
     */
    public boolean isHiddenBlock(Object obj) {
        if (obj instanceof String s) {
            return hiddenBlocks.contains(s);
        } else if (obj instanceof Material m) {
            return hiddenBlocks.contains(m.name());
        }
        return hiddenBlocks.contains(Material.SPAWNER.name());
    }

    /**
     * Return true if the block should not be hidden
     * @param m block material
     * @return false if hidden
     */
    public boolean isNotHiddenBlock(Object obj) {
        return !isHiddenBlock(obj);
    }

    /**
     * @return the spawnerValues
     */
    public Map<EntityType, Integer> getSpawnerValues() {
        return spawnerValues;
    }

    /**
     * @param spawnerValues the spawnerValues to set
     */
    public void setSpawnerValues(Map<EntityType, Integer> spawnerValues) {
        this.spawnerValues = spawnerValues;
    }

    /**
     * @return the blockValues
     */
    public Map<String, Integer> getBlockValues() {
        return blockValues;
    }

    /**
     * @return the worldBlockValues
     */
    public Map<World, Map<String, Integer>> getWorldBlockValues() {
        return worldBlockValues;
    }

}
