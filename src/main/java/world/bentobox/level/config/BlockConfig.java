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
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import world.bentobox.level.Level;

/**
 * Contains all the block values, limits and world differences
 * @author tastybento
 *
 */
public class BlockConfig {

    private static final String SPAWNER = "_SPAWNER";
    private Map<Material, Integer> blockLimits = new EnumMap<>(Material.class);
    private Map<Material, Integer> blockValues = new EnumMap<>(Material.class);
    private final Map<World, Map<Material, Integer>> worldBlockValues = new HashMap<>();
    private final Map<World, Map<EntityType, Integer>> worldSpawnerValues = new HashMap<>();
    private final List<Material> hiddenBlocks;
    private Map<EntityType, Integer> spawnerValues = new EnumMap<>(EntityType.class);
    private Level addon;

    /**
     * Loads block limits, values and world settings and then saves them again
     * @param addon - addon
     * @param blockValues - yaml configuration file containing the block values
     * @param file - the file representing the yaml config. Will be saved after readong.
     * @throws IOException - if there is an error
     */
    public BlockConfig(Level addon, YamlConfiguration blockValues, File file) throws IOException {
        this.addon = addon;
        if (blockValues.isConfigurationSection("limits")) {
            setBlockLimits(loadBlockLimits(blockValues));
        }
        if (blockValues.isConfigurationSection("blocks")) {
            setBlockValues(loadBlockValues(blockValues));
            setSpawnerValues(loadSpawnerValues(blockValues));
        } else {
            addon.logWarning("No block values in blockconfig.yml! All island levels will be zero!");
        }
        // Worlds
        if (blockValues.isConfigurationSection("worlds")) {
            loadWorlds(blockValues);
        }
        // Hidden
        hiddenBlocks = blockValues.getStringList("hidden-blocks").stream().map(name -> {
            try {
                return Material.valueOf(name.toUpperCase(Locale.ENGLISH));

            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();

        // All done
        blockValues.save(file);
    }

    private void loadWorlds(YamlConfiguration blockValues2) {
        ConfigurationSection worlds = Objects.requireNonNull(blockValues2.getConfigurationSection("worlds"));
        for (String world : Objects.requireNonNull(worlds).getKeys(false)) {
            World bWorld = Bukkit.getWorld(world);
            if (bWorld != null) {
                ConfigurationSection worldValues = worlds.getConfigurationSection(world);
                for (String material : Objects.requireNonNull(worldValues).getKeys(false)) {
                    try {
                        Material mat = Material.valueOf(material);
                        Map<Material, Integer> values = worldBlockValues.getOrDefault(bWorld,
                                new EnumMap<>(Material.class));
                        values.put(mat, worldValues.getInt(material));
                        worldBlockValues.put(bWorld, values);
                    } catch (Exception e) {
                        addon.logError(
                                "Unknown material (" + material + ") in blockconfig.yml worlds section. Skipping...");
                    }
                }
            } else {
                addon.logWarning("Level Addon: No such world in blockconfig.yml : " + world);
            }
        }

    }

    private Map<Material, Integer> loadBlockValues(YamlConfiguration blockValues2) {
        ConfigurationSection blocks = Objects.requireNonNull(blockValues2.getConfigurationSection("blocks"));
        Map<Material, Integer> bv = new EnumMap<>(Material.class);
        // Update blockvalues to latest settings
        Registry.MATERIAL.stream().filter(Material::isBlock)
        .filter(m -> !m.name().startsWith("LEGACY_"))
        .filter(m -> !m.isAir())
        .filter(m -> !m.equals(Material.WATER))
        .forEach(m -> {
            if (!blocks.contains(m.name(), true)) {
                blocks.set(m.name(), 1);
            }
            bv.put(m, blocks.getInt(m.name(), 1));
        });
        return bv;
    }

    /**
     * Loads the spawner values from the blocks in the config
     * Format is entityname + _SPANWER, so for example ALLAY_SPAWNER
     * If they are missing, then they will score 1
     * @param blockValues config section
     * @return map of entity types and their score
     */
    private Map<EntityType, Integer> loadSpawnerValues(YamlConfiguration blockValues) {
        ConfigurationSection blocks = Objects.requireNonNull(blockValues.getConfigurationSection("blocks"));
        Map<EntityType, Integer> bv = new HashMap<>();


        // Update spawners
        Registry.MATERIAL.stream().filter(Material::isItem)
                .filter(m -> m.name().endsWith("_SPAWN_EGG")).map(m -> m.name().substring(0, m.name().length() - 10))
        .forEach(m -> {
            // Populate missing spawners
                    if (!blocks.contains(m + SPAWNER, true)) {
                        blocks.set(m + SPAWNER, 1);
                    }
                    // Load value
                    try {
                        EntityType et = EntityType.valueOf(m);
                        bv.put(et, blocks.getInt(m + SPAWNER));
                    } catch (Exception e) {
                        e.printStackTrace();
            }
        });
        return bv;
    }

    private Map<Material, Integer> loadBlockLimits(YamlConfiguration blockValues2) {
        Map<Material, Integer> bl = new EnumMap<>(Material.class);
        ConfigurationSection limits = Objects.requireNonNull(blockValues2.getConfigurationSection("limits"));
        for (String material : limits.getKeys(false)) {
            try {
                Material mat = Material.valueOf(material);
                bl.put(mat, limits.getInt(material, 0));
            } catch (Exception e) {
                addon.logError("Unknown material (" + material + ") in blockconfig.yml Limits section. Skipping...");
            }
        }
        return bl;
    }

    /**
     * @return the blockLimits
     */
    public final Map<Material, Integer> getBlockLimits() {
        return blockLimits;
    }
    /**
     * @param bl the blockLimits to set
     */
    private void setBlockLimits(Map<Material, Integer> bl) {
        this.blockLimits = bl;
    }
    /**
     * @return the blockValues
     */
    public final Map<Material, Integer> getBlockValues() {
        return blockValues;
    }
    /**
     * @param blockValues2 the blockValues to set
     */
    private void setBlockValues(Map<Material, Integer> blockValues2) {
        this.blockValues = blockValues2;
    }

    /**
     * @return the worldBlockValues
     */
    public Map<World, Map<Material, Integer>> getWorldBlockValues() {
        return worldBlockValues;
    }

    /**
     * @return the worldSpawnerValues
     */
    public Map<World, Map<EntityType, Integer>> getWorldSpawnerValues() {
        return worldSpawnerValues;
    }

    /**
     * Get the value of of a spawner in world
     * @param world - world
     * @param obj - entity type that will spawn from this spawner or material
     * @return value or null if not configured with a value
     */
    public Integer getValue(World world, Object obj) {
        if (obj instanceof EntityType et) {
            // Check world settings
            if (getWorldSpawnerValues().containsKey(world) && getWorldSpawnerValues().get(world).containsKey(et)) {
                return getWorldSpawnerValues().get(world).get(et);
            }
            // Check baseline
            if (getSpawnerValues().containsKey(et)) {
                return getSpawnerValues().get(et);
            }
        } else if (obj instanceof Material md) {
            // Check world settings
            if (getWorldBlockValues().containsKey(world) && getWorldBlockValues().get(world).containsKey(md)) {
                return getWorldBlockValues().get(world).get(md);
            }
            // Check baseline
            if (getBlockValues().containsKey(md)) {
                return getBlockValues().get(md);
            }
        }
        return null;
    }

    /**
     * Return true if the block should be hidden
     * @param m block material or entity type of spanwer
     * @return true if hidden
     */
    public boolean isHiddenBlock(Object obj) {
        if (obj instanceof Material m) {
            return hiddenBlocks.contains(m);
        }
        return hiddenBlocks.contains(Material.SPAWNER);
    }

    /**
     * Return true if the block should not be hidden
     * @param m block material
     * @return false if hidden
     */
    public boolean isNotHiddenBlock(Object obj) {
        if (obj instanceof Material m) {
            return !hiddenBlocks.contains(m);
        } else {
            return !hiddenBlocks.contains(Material.SPAWNER);
        }
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

}
