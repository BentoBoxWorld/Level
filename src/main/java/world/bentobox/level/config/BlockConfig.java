package world.bentobox.level.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.level.Level;

/**
 * Contains all the block values, limits and world differences
 * @author tastybento
 *
 */
public class BlockConfig {

    private Map<Material, Integer> blockLimits = new EnumMap<>(Material.class);
    private Map<Material, Integer> blockValues = new EnumMap<>(Material.class);
    private final Map<World, Map<Material, Integer>> worldBlockValues = new HashMap<>();
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
        } else {
            addon.logWarning("No block values in blockconfig.yml! All island levels will be zero!");
        }
        // Worlds
        if (blockValues.isConfigurationSection("worlds")) {
            loadWorlds(blockValues);
        }
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
                    Material mat = Material.valueOf(material);
                    Map<Material, Integer> values = worldBlockValues.getOrDefault(bWorld, new EnumMap<>(Material.class));
                    values.put(mat, worldValues.getInt(material));
                    worldBlockValues.put(bWorld, values);
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
        Arrays.stream(Material.values()).filter(Material::isBlock)
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

    private Map<Material, Integer> loadBlockLimits(YamlConfiguration blockValues2) {
        Map<Material, Integer> bl = new EnumMap<>(Material.class);
        ConfigurationSection limits = Objects.requireNonNull(blockValues2.getConfigurationSection("limits"));
        for (String material : limits.getKeys(false)) {
            try {
                Material mat = Material.valueOf(material);
                bl.put(mat, limits.getInt(material, 0));
            } catch (Exception e) {
                addon.logWarning("Unknown material (" + material + ") in blockconfig.yml Limits section. Skipping...");
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
     * Get the value of material in world
     * @param world - world
     * @param md - material
     * @return value or null if not configured with a value
     */
    public Integer getValue(World world, Material md) {
        // Check world settings
        if (getWorldBlockValues().containsKey(world) && getWorldBlockValues().get(world).containsKey(md)) {
            return getWorldBlockValues().get(world).get(md);
        }
        // Check baseline
        if (getBlockValues().containsKey(md)) {
            return getBlockValues().get(md);
        }
        return null;
    }


}
