package bskyblock.addin.level.config;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import bskyblock.addin.level.Level;

public class PluginConfig {
    private static final boolean DEBUG = false;

    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public PluginConfig(Level plugin) {
        plugin.saveDefaultConfig();

        // Island level cool down time
        Settings.levelWait = plugin.getConfig().getInt("levelwait", 60);
        if (Settings.levelWait < 0) {
            Settings.levelWait = 0;
        }

        // Get the under water multiplier
        Settings.deathpenalty = plugin.getConfig().getInt("deathpenalty", 0);
        Settings.sumTeamDeaths = plugin.getConfig().getBoolean("sumteamdeaths");
        Settings.maxDeaths = plugin.getConfig().getInt("maxdeaths", 10);
        Settings.islandResetDeathReset = plugin.getConfig().getBoolean("islandresetdeathreset", true);
        Settings.teamJoinDeathReset = plugin.getConfig().getBoolean("teamjoindeathreset", true);
        Settings.underWaterMultiplier = plugin.getConfig().getDouble("underwater", 1D);
        Settings.levelCost = plugin.getConfig().getInt("levelcost", 100);
        if (Settings.levelCost < 1) {
            Settings.levelCost = 1;
            plugin.getLogger().warning("levelcost in blockvalues.yml cannot be less than 1. Setting to 1.");
        }
        Settings.blockLimits = new HashMap<MaterialData, Integer>();
        if (plugin.getConfig().isSet("limits")) {
            for (String material : plugin.getConfig().getConfigurationSection("limits").getKeys(false)) {
                try {
                    String[] split = material.split(":");
                    byte data = 0;
                    if (split.length>1) {
                        data = Byte.valueOf(split[1]);
                    }
                    Material mat;
                    if (StringUtils.isNumeric(split[0])) {
                        mat = Material.getMaterial(Integer.parseInt(split[0]));
                    } else {
                        mat = Material.valueOf(split[0].toUpperCase());
                    }
                    MaterialData materialData = new MaterialData(mat);
                    materialData.setData(data);
                    Settings.blockLimits.put(materialData, plugin.getConfig().getInt("limits." + material, 0));
                    if (DEBUG) {
                        plugin.getLogger().info("Maximum number of " + materialData + " will be " + Settings.blockLimits.get(materialData));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
                }
            }
        }
        Settings.blockValues = new HashMap<MaterialData, Integer>();
        if (plugin.getConfig().isSet("blocks")) {
            for (String material : plugin.getConfig().getConfigurationSection("blocks").getKeys(false)) {
                try {
                    String[] split = material.split(":");
                    byte data = 0;
                    if (split.length>1) {
                        data = Byte.valueOf(split[1]);
                    }
                    MaterialData materialData = null;
                    if (StringUtils.isNumeric(split[0])) {
                        materialData = new MaterialData(Integer.parseInt(split[0]));
                    } else {
                        materialData = new MaterialData(Material.valueOf(split[0].toUpperCase()));
                    }

                    materialData.setData(data);
                    Settings.blockValues.put(materialData, plugin.getConfig().getInt("blocks." + material, 0));
                    if (DEBUG) {
                        plugin.getLogger().info(materialData.toString() + " value = " + Settings.blockValues.get(materialData));
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                    plugin.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml blocks section. Skipping...");
                }
            }
        } else {
            plugin.getLogger().severe("No block values in blockvalues.yml! All island levels will be zero!");
        }
        // All done
    }
}
