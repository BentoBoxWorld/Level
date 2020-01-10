package world.bentobox.level.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import world.bentobox.level.Level;

public class Settings {

    private final Level level;
    private boolean sumTeamDeaths;
    private Map<Material, Integer> blockLimits = new HashMap<>();
    private Map<Material, Integer> blockValues = new HashMap<>();
    private final Map<World, Map<Material, Integer>> worldBlockValues = new HashMap<>();
    private double underWaterMultiplier;
    private int deathpenalty;
    private long levelCost;
    private int levelWait;

    private List<String> gameModes;


    public Settings(Level level) {
        this.level = level;
        level.saveDefaultConfig();

        // GameModes
        gameModes = level.getConfig().getStringList("game-modes");

        setLevelWait(level.getConfig().getInt("levelwait", 60));
        if (getLevelWait() < 0) {
            setLevelWait(0);
        }
        setDeathpenalty(level.getConfig().getInt("deathpenalty", 0));
        setSumTeamDeaths(level.getConfig().getBoolean("sumteamdeaths"));
        setUnderWaterMultiplier(level.getConfig().getDouble("underwater", 1D));
        setLevelCost(level.getConfig().getInt("levelcost", 100));
        if (getLevelCost() < 1) {
            setLevelCost(1);
            level.logWarning("levelcost in blockvalues.yml cannot be less than 1. Setting to 1.");
        }

        if (level.getConfig().isSet("limits")) {
            HashMap<Material, Integer> bl = new HashMap<>();
            for (String material : Objects.requireNonNull(level.getConfig().getConfigurationSection("limits")).getKeys(false)) {
                try {
                    Material mat = Material.valueOf(material);
                    bl.put(mat, level.getConfig().getInt("limits." + material, 0));
                } catch (Exception e) {
                    level.logWarning("Unknown material (" + material + ") in blockvalues.yml Limits section. Skipping...");
                }
            }
            setBlockLimits(bl);
        }
        if (level.getConfig().isSet("blocks")) {
            Map<Material, Integer> bv = new HashMap<>();
            for (String material : Objects.requireNonNull(level.getConfig().getConfigurationSection("blocks")).getKeys(false)) {

                try {
                    Material mat = Material.valueOf(material);
                    bv.put(mat, level.getConfig().getInt("blocks." + material, 0));
                } catch (Exception e) {
                    level.logWarning("Unknown material (" + material + ") in config.yml blocks section. Skipping...");
                }
            }
            setBlockValues(bv);
        } else {
            level.logWarning("No block values in config.yml! All island levels will be zero!");
        }
        // Worlds
        if (level.getConfig().isSet("worlds")) {
            ConfigurationSection worlds = level.getConfig().getConfigurationSection("worlds");
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
                    level.logWarning("Level Addon: No such world in config.yml : " + world);
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

    /**
     * @return if the nether island should be included in the level calc or not
     */
    public boolean isNether() {
        return level.getConfig().getBoolean("nether");
    }

    /**
     * @return if the end island should be included in the level calc or not
     */
    public boolean isEnd() {
        return level.getConfig().getBoolean("end");
    }

    /**
     * @return true if level should be calculated on login
     */
    public boolean isLogin() {
        return level.getConfig().getBoolean("login");
    }

    /**
     * @return true if levels should be shown in shorthand notation, e.g., 10,234 = 10k
     */
    public boolean isShortHand() {
        return level.getConfig().getBoolean("shorthand");
    }

    /**
     * @return the formula to calculate island levels
     */
    public String getLevelCalc() {
        return level.getConfig().getString("level-calc", "blocks / level_cost");
    }

}
