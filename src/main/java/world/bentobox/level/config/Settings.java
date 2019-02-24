package world.bentobox.level.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import world.bentobox.level.Level;

public class Settings {

    private boolean sumTeamDeaths;
    private Map<Material, Integer> blockLimits = new HashMap<>();
    private Map<Material, Integer> blockValues = new HashMap<>();
    private final Map<World, Map<Material, Integer>> worldBlockValues = new HashMap<>();
    private double underWaterMultiplier;
    private int deathpenalty;
    private long levelCost;
    private int levelWait;
    private int maxDeaths;
    private boolean islandResetDeathReset;
    private boolean teamJoinDeathReset;
    private List<String> gameModes = new ArrayList<>();

    public Settings(Level level) {

        level.saveDefaultConfig();

        // GameModes
        gameModes = level.getConfig().getStringList("game-modes");

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
            HashMap<Material, Integer> bl = new HashMap<>();
            for (String material : level.getConfig().getConfigurationSection("limits").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(material);
                    bl.put(mat, level.getConfig().getInt("limits." + material, 0));
                } catch (Exception e) {
                    level.getLogger().warning(() -> "Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
                }
            }
            setBlockLimits(bl);
        }
        if (level.getConfig().isSet("blocks")) {
            Map<Material, Integer> bv = new HashMap<>();
            for (String material : level.getConfig().getConfigurationSection("blocks").getKeys(false)) {

                try {
                    Material mat = Material.valueOf(material);
                    bv.put(mat, level.getConfig().getInt("blocks." + material, 0));
                } catch (Exception e) {
                    level.getLogger().warning(()-> "Unknown material (" + material + ") in config.yml blocks section. Skipping...");
                }
            }
            setBlockValues(bv);
        } else {
            level.getLogger().severe("No block values in config.yml! All island levels will be zero!");
        }
        // Worlds
        if (level.getConfig().isSet("worlds")) {
            ConfigurationSection worlds = level.getConfig().getConfigurationSection("worlds");
            for (String world : worlds.getKeys(false)) {
                World bWorld = Bukkit.getWorld(world);
                if (bWorld != null) {
                    ConfigurationSection worldValues = worlds.getConfigurationSection(world);
                    for (String material : worldValues.getKeys(false)) {
                        Material mat = Material.valueOf(material);
                        Map<Material, Integer> values = worldBlockValues.getOrDefault(bWorld, new HashMap<>());
                        values.put(mat, worldValues.getInt("blocks." + material, 0));
                        worldBlockValues.put(bWorld, values);
                    }
                } else {
                    level.getLogger().severe(() -> "Level Addon: No such world : " + world);
                }
            }
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
    private void setSumTeamDeaths(boolean sumTeamDeaths) {
        this.sumTeamDeaths = sumTeamDeaths;
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
     * @return the underWaterMultiplier
     */
    public final double getUnderWaterMultiplier() {
        return underWaterMultiplier;
    }
    /**
     * @param underWaterMultiplier the underWaterMultiplier to set
     */
    private void setUnderWaterMultiplier(double underWaterMultiplier) {
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
    private void setDeathpenalty(int deathpenalty) {
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
    private void setLevelCost(long levelCost) {
        this.levelCost = levelCost;
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
    private void setLevelWait(int levelWait) {
        this.levelWait = levelWait;
    }
    /**
     * TODO: Use max deaths
     * @return the maxDeaths
     */
    public final int getMaxDeaths() {
        return maxDeaths;
    }
    /**
     * @param maxDeaths the maxDeaths to set
     */
    private void setMaxDeaths(int maxDeaths) {
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
    private void setIslandResetDeathReset(boolean islandResetDeathReset) {
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
    private void setTeamJoinDeathReset(boolean teamJoinDeathReset) {
        this.teamJoinDeathReset = teamJoinDeathReset;
    }

    /**
     * @return the worldBlockValues
     */
    public Map<World, Map<Material, Integer>> getWorldBlockValues() {
        return worldBlockValues;
    }

    /**
     * @return the gameModes
     */
    public List<String> getGameModes() {
        return gameModes;
    }

}
