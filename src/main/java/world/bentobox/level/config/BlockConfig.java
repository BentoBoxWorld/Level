package world.bentobox.level.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.level.Level;

public class BlockConfig {

    private Map<Material, Integer> blockLimits = new HashMap<>();
    private Map<Material, Integer> blockValues = new HashMap<>();
    private final Map<World, Map<Material, Integer>> worldBlockValues = new HashMap<>();

    public BlockConfig(Level level, YamlConfiguration blockValues, File file) throws IOException {

        if (blockValues.isConfigurationSection("limits")) {
            HashMap<Material, Integer> bl = new HashMap<>();
            ConfigurationSection limits = Objects.requireNonNull(blockValues.getConfigurationSection("limits"));
            for (String material : limits.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(material);
                    bl.put(mat, limits.getInt(material, 0));
                } catch (Exception e) {
                    level.logWarning("Unknown material (" + material + ") in blockconfig.yml Limits section. Skipping...");
                }
            }
            setBlockLimits(bl);
        }
        if (blockValues.isConfigurationSection("blocks")) {
            ConfigurationSection blocks = Objects.requireNonNull(blockValues.getConfigurationSection("blocks"));
            Map<Material, Integer> bv = new HashMap<>();
            // Update blockvalues to latest settings
            Arrays.stream(Material.values()).filter(Material::isBlock)
            .filter(m -> !m.name().startsWith("LEGACY_"))
            .forEach(m -> {
                if (!blocks.contains(m.name(), true)) {
                    blocks.set(m.name(), 1);
                }
                bv.put(m, blocks.getInt(m.name(), 1));
            });
            setBlockValues(bv);
        } else {
            level.logWarning("No block values in blockconfig.yml! All island levels will be zero!");
        }
        // Worlds
        if (blockValues.isConfigurationSection("worlds")) {
            ConfigurationSection worlds = Objects.requireNonNull(blockValues.getConfigurationSection("worlds"));
            for (String world : Objects.requireNonNull(worlds).getKeys(false)) {
                World bWorld = Bukkit.getWorld(world);
                if (bWorld != null) {
                    ConfigurationSection worldValues = worlds.getConfigurationSection(world);
                    for (String material : Objects.requireNonNull(worldValues).getKeys(false)) {
                        Material mat = Material.valueOf(material);
                        Map<Material, Integer> values = worldBlockValues.getOrDefault(bWorld, new HashMap<>());
                        values.put(mat, worldValues.getInt(material));
                        worldBlockValues.put(bWorld, values);
                    }
                } else {
                    level.logWarning("Level Addon: No such world in blockconfig.yml : " + world);
                }
            }
        }
        // All done
        blockValues.save(file);
    }

    /**
     * @return the blockLimits
     */
    public final Map<Material, Integer> getBlockLimits() {
        return blockLimits;
    }
    /**
     * @param blockLimits2 the blockLimits to set
     */
    private void setBlockLimits(HashMap<Material, Integer> blockLimits2) {
        this.blockLimits = blockLimits2;
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
}
