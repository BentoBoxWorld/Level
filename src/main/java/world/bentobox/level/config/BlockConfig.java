package world.bentobox.level.config;

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

    public BlockConfig(Level level, YamlConfiguration blockValues) {

        if (blockValues.isSet("limits")) {
            HashMap<Material, Integer> bl = new HashMap<>();
            for (String material : Objects.requireNonNull(blockValues.getConfigurationSection("limits")).getKeys(false)) {
                try {
                    Material mat = Material.valueOf(material);
                    bl.put(mat, blockValues.getInt("limits." + material, 0));
                } catch (Exception e) {
                    level.logWarning("Unknown material (" + material + ") in blockconfig.yml Limits section. Skipping...");
                }
            }
            setBlockLimits(bl);
        }
        if (blockValues.isSet("blocks")) {
            Map<Material, Integer> bv = new HashMap<>();
            for (String material : Objects.requireNonNull(blockValues.getConfigurationSection("blocks")).getKeys(false)) {

                try {
                    Material mat = Material.valueOf(material);
                    bv.put(mat, blockValues.getInt("blocks." + material, 0));
                } catch (Exception e) {
                    level.logWarning("Unknown material (" + material + ") in blockconfig.yml blocks section. Skipping...");
                }
            }
            setBlockValues(bv);
        } else {
            level.logWarning("No block values in blockconfig.yml! All island levels will be zero!");
        }
        // Worlds
        if (blockValues.isSet("worlds")) {
            ConfigurationSection worlds = blockValues.getConfigurationSection("worlds");
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
