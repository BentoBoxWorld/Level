package bskyblock.addon.level.config;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import bskyblock.addon.level.Level;

public class Settings {

    private static final boolean DEBUG = false;
    private boolean sumTeamDeaths;
    private int seaHeight;
    private HashMap<MaterialData, Integer> blockLimits = new HashMap<>();
    private HashMap<MaterialData, Integer> blockValues = new HashMap<>();
    private double underWaterMultiplier;
    private int deathpenalty;
    private long levelCost;
    private Object defaultLanguage;
    private int levelWait;
    private int maxDeaths;
    private boolean islandResetDeathReset;
    private boolean teamJoinDeathReset;

    @SuppressWarnings("deprecation")
    public Settings(Level level) {
        level.saveDefaultConfig();

        setLevelWait(level.getConfig().getInt("levelwait", 60));
        if (getLevelWait() < 0) {
            setLevelWait(0);
        }
        setDeathpenalty(level.getConfig().getInt("deathpenalty", 0));
        setSumTeamDeaths(level.getConfig().getBoolean("sumteamdeaths"));
        setMaxDeaths(level.getConfig().getInt("maxdeaths", 10));
        setIslandResetDeathReset(level.getConfig().getBoolean("islandresetdeathreset", true));
        setTeamJoinDeathReset(level.getConfig().getBoolean("teamjoindeathreset", true));
        setUnderWaterMultiplier(level.getConfig().getDouble("underwater", 1D));
        setLevelCost(level.getConfig().getInt("levelcost", 100));
        if (getLevelCost() < 1) {
            setLevelCost(1);
            level.getLogger().warning("levelcost in blockvalues.yml cannot be less than 1. Setting to 1.");
        }

        if (level.getConfig().isSet("limits")) {
            HashMap<MaterialData, Integer> blockLimits = new HashMap<>();
            for (String material : level.getConfig().getConfigurationSection("limits").getKeys(false)) {
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
                    blockLimits.put(materialData, level.getConfig().getInt("limits." + material, 0));
                    if (DEBUG) {
                        level.getLogger().info("Maximum number of " + materialData + " will be " + blockLimits.get(materialData));
                    }
                } catch (Exception e) {
                    level.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
                }
            }
            setBlockLimits(blockLimits);
        }
        if (level.getConfig().isSet("blocks")) {
            HashMap<MaterialData, Integer> blockValues = new HashMap<>();
            for (String material : level.getConfig().getConfigurationSection("blocks").getKeys(false)) {
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
                    blockValues.put(materialData, level.getConfig().getInt("blocks." + material, 0));
                    if (DEBUG) {
                        level.getLogger().info(materialData.toString() + " value = " + blockValues.get(materialData));
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                    level.getLogger().warning("Unknown material (" + material + ") in blockvalues.yml blocks section. Skipping...");
                }
            }
            setBlockValues(blockValues);
        } else {
            level.getLogger().severe("No block values in blockvalues.yml! All island levels will be zero!");
        }
        // All done
    }

    /**
     * @return the sumTeamDeaths
     */
    public final boolean isSumTeamDeaths() {
        return sumTeamDeaths;
    }
    /**
     * @param sumTeamDeaths the sumTeamDeaths to set
     */
    public final void setSumTeamDeaths(boolean sumTeamDeaths) {
        this.sumTeamDeaths = sumTeamDeaths;
    }
    /**
     * @return the seaHeight
     */
    public final int getSeaHeight() {
        return seaHeight;
    }
    /**
     * @param seaHeight the seaHeight to set
     */
    public final void setSeaHeight(int seaHeight) {
        this.seaHeight = seaHeight;
    }
    /**
     * @return the blockLimits
     */
    public final HashMap<MaterialData, Integer> getBlockLimits() {
        return blockLimits;
    }
    /**
     * @param blockLimits the blockLimits to set
     */
    public final void setBlockLimits(HashMap<MaterialData, Integer> blockLimits) {
        this.blockLimits = blockLimits;
    }
    /**
     * @return the blockValues
     */
    public final HashMap<MaterialData, Integer> getBlockValues() {
        return blockValues;
    }
    /**
     * @param blockValues the blockValues to set
     */
    public final void setBlockValues(HashMap<MaterialData, Integer> blockValues) {
        this.blockValues = blockValues;
    }
    /**
     * @return the underWaterMultiplier
     */
    public final double getUnderWaterMultiplier() {
        return underWaterMultiplier;
    }
    /**
     * @param underWaterMultiplier the underWaterMultiplier to set
     */
    public final void setUnderWaterMultiplier(double underWaterMultiplier) {
        this.underWaterMultiplier = underWaterMultiplier;
    }
    /**
     * @return the deathpenalty
     */
    public final int getDeathPenalty() {
        return deathpenalty;
    }
    /**
     * @param deathpenalty the deathpenalty to set
     */
    public final void setDeathpenalty(int deathpenalty) {
        this.deathpenalty = deathpenalty;
    }
    /**
     * @return the levelCost
     */
    public final long getLevelCost() {
        return levelCost;
    }
    /**
     * @param levelCost the levelCost to set
     */
    public final void setLevelCost(long levelCost) {
        this.levelCost = levelCost;
    }
    /**
     * @return the defaultLanguage
     */
    public final Object getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * @param defaultLanguage the defaultLanguage to set
     */
    public final void setDefaultLanguage(Object defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    /**
     * @return the levelWait
     */
    public final int getLevelWait() {
        return levelWait;
    }
    /**
     * @param levelWait the levelWait to set
     */
    public final void setLevelWait(int levelWait) {
        this.levelWait = levelWait;
    }
    /**
     * @return the maxDeaths
     */
    public final int getMaxDeaths() {
        return maxDeaths;
    }
    /**
     * @param maxDeaths the maxDeaths to set
     */
    public final void setMaxDeaths(int maxDeaths) {
        this.maxDeaths = maxDeaths;
    }
    /**
     * @return the islandResetDeathReset
     */
    public final boolean isIslandResetDeathReset() {
        return islandResetDeathReset;
    }
    /**
     * @param islandResetDeathReset the islandResetDeathReset to set
     */
    public final void setIslandResetDeathReset(boolean islandResetDeathReset) {
        this.islandResetDeathReset = islandResetDeathReset;
    }
    /**
     * @return the teamJoinDeathReset
     */
    public final boolean isTeamJoinDeathReset() {
        return teamJoinDeathReset;
    }
    /**
     * @param teamJoinDeathReset the teamJoinDeathReset to set
     */
    public final void setTeamJoinDeathReset(boolean teamJoinDeathReset) {
        this.teamJoinDeathReset = teamJoinDeathReset;
    }

}
