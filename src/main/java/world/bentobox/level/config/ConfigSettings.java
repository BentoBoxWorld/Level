package world.bentobox.level.config;

import java.util.Collections;
import java.util.List;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

@StoreAt(filename="config.yml", path="addons/Level")
@ConfigComment("Level Configuration [version]")
@ConfigComment("")
public class ConfigSettings implements ConfigObject {
    @ConfigComment("")
    @ConfigComment("Disabled Game Mode Addons")
    @ConfigComment("Level will NOT hook into these game mode addons.")
    @ConfigEntry(path = "disabled-game-modes")
    private List<String> gameModes = Collections.emptyList();

    @ConfigComment("")
    @ConfigComment("Number of concurrent island calculations")
    @ConfigComment("If your CPU can handle it, you can run parallel island calcs if there are more than one in the queue")
    @ConfigEntry(path = "concurrent-island-calcs")
    private int concurrentIslandCalcs = 1;

    @ConfigComment("")
    @ConfigComment("Island level calculation timeout in minutes.")
    @ConfigComment("If an island takes longer that this time to calculate, then the calculation will abort.")
    @ConfigComment("Generally, calculation should only take a few seconds, so if this ever triggers then something is not right.")
    @ConfigEntry(path = "calculation-timeout")
    private int calculationTimeout = 5;


    @ConfigComment("")
    @ConfigComment("Zero island levels on new island or island reset")
    @ConfigComment("If true, Level will calculate the starter island's level and remove it from any future level calculations.")
    @ConfigComment("If false, the player's starter island and will count towards their level.")
    @ConfigComment("This will reduce CPU if it isn't used.")
    @ConfigEntry(path = "zero-new-island-levels")
    private boolean zeroNewIslandLevels = true;


    @ConfigComment("")
    @ConfigComment("Calculate island level on login")
    @ConfigComment("This silently calculates the player's island level when they login")
    @ConfigComment("This applies to all islands the player has on the server, e.g., BSkyBlock, AcidIsland")
    @ConfigEntry(path = "login")
    private boolean calcOnLogin = false;

    @ConfigComment("")
    @ConfigComment("Include nether island in level calculations.")
    @ConfigComment("Warning: Enabling this mid-game will give players with an island a jump in")
    @ConfigComment("island level. New islands will be correctly zeroed.")
    @ConfigEntry(path = "nether")
    private boolean nether = false;

    @ConfigComment("")
    @ConfigComment("Include end island in level calculations.")
    @ConfigComment("Warning: Enabling this mid-game will give players with an island a jump in")
    @ConfigComment("island level. New islands will be correctly zeroed.")
    @ConfigEntry(path = "end")
    private boolean end = false;

    @ConfigComment("")
    @ConfigComment("Include chest contents in level calculations.")
    @ConfigComment("Will count blocks in chests or containers.")
    @ConfigEntry(path = "include-chests")
    private boolean includeChests = false;

    @ConfigComment("")
    @ConfigComment("Underwater block multiplier")
    @ConfigComment("If blocks are below sea-level, they can have a higher value. e.g. 2x")
    @ConfigComment("Promotes under-water development if there is a sea. Value can be fractional.")
    @ConfigEntry(path = "underwater")
    private double underWaterMultiplier = 1.0;

    @ConfigComment("")
    @ConfigComment("Value of one island level. Default 100. Minimum value is 1.")
    @ConfigEntry(path = "levelcost")
    private long levelCost = 100;

    @ConfigComment("")
    @ConfigComment("Island level calculation formula")
    @ConfigComment("blocks - the sum total of all block values, less any death penalty")
    @ConfigComment("level_cost - in a linear equation, the value of one level")
    @ConfigComment("This formula can include +,=,*,/,sqrt,^,sin,cos,tan. Result will always be rounded to a long integer")
    @ConfigComment("for example, an alternative non-linear option could be: 3 * sqrt(blocks / level_cost)")
    @ConfigEntry(path = "level-calc")
    private String levelCalc = "blocks / level_cost";

    @ConfigComment("")
    @ConfigComment("Cooldown between level requests in seconds")
    @ConfigEntry(path = "levelwait")
    private int levelWait = 60;

    @ConfigComment("")
    @ConfigComment("Death penalty")
    @ConfigComment("How many block values a player will lose per death.")
    @ConfigComment("Default value of 100 means that for every death, the player will lose 1 level (if levelcost is 100)")
    @ConfigComment("Set to zero to not use this feature")
    @ConfigEntry(path = "deathpenalty")
    private int deathPenalty = 100;

    @ConfigComment("Sum team deaths - if true, all the teams deaths are summed")
    @ConfigComment("If false, only the leader's deaths counts")
    @ConfigComment("For other death related settings, see the GameModeAddon's config.yml settings.")
    @ConfigEntry(path = "sumteamdeaths")
    private boolean sumTeamDeaths = false;


    @ConfigComment("Shorthand island level")
    @ConfigComment("Shows large level values rounded down, e.g., 10,345 -> 10k")
    @ConfigEntry(path = "shorthand")
    private boolean shorthand = false;


    /**
     * @return the gameModes
     */
    public List<String> getGameModes() {
        return gameModes;
    }


    /**
     * @param gameModes the gameModes to set
     */
    public void setGameModes(List<String> gameModes) {
        this.gameModes = gameModes;
    }

    /**
     * @return the calcOnLogin
     */
    public boolean isCalcOnLogin() {
        return calcOnLogin;
    }


    /**
     * @param calcOnLogin the calcOnLogin to set
     */
    public void setCalcOnLogin(boolean calcOnLogin) {
        this.calcOnLogin = calcOnLogin;
    }


    /**
     * @return the nether
     */
    public boolean isNether() {
        return nether;
    }


    /**
     * @param nether the nether to set
     */
    public void setNether(boolean nether) {
        this.nether = nether;
    }


    /**
     * @return the end
     */
    public boolean isEnd() {
        return end;
    }


    /**
     * @param end the end to set
     */
    public void setEnd(boolean end) {
        this.end = end;
    }


    /**
     * @return the underWaterMultiplier
     */
    public double getUnderWaterMultiplier() {
        return underWaterMultiplier;
    }


    /**
     * @param underWaterMultiplier the underWaterMultiplier to set
     */
    public void setUnderWaterMultiplier(double underWaterMultiplier) {
        this.underWaterMultiplier = underWaterMultiplier;
    }


    /**
     * @return the levelCost
     */
    public long getLevelCost() {
        if (levelCost < 1) {
            levelCost = 1;
            BentoBox.getInstance().logError("levelcost in config.yml cannot be less than 1. Setting to 1.");
        }
        return levelCost;
    }


    /**
     * @param levelCost the levelCost to set
     */
    public void setLevelCost(long levelCost) {
        this.levelCost = levelCost;
    }


    /**
     * @return the levelCalc
     */
    public String getLevelCalc() {
        return levelCalc;
    }


    /**
     * @param levelCalc the levelCalc to set
     */
    public void setLevelCalc(String levelCalc) {
        this.levelCalc = levelCalc;
    }


    /**
     * @return the levelWait
     */
    public int getLevelWait() {
        if (levelWait < 0) {
            BentoBox.getInstance().logError("levelwait must be at least 0");
            levelWait = 0;
        }
        return levelWait;
    }


    /**
     * @param levelWait the levelWait to set
     */
    public void setLevelWait(int levelWait) {
        this.levelWait = levelWait;
    }


    /**
     * @return the deathPenalty
     */
    public int getDeathPenalty() {
        return deathPenalty;
    }


    /**
     * @param deathPenalty the deathPenalty to set
     */
    public void setDeathPenalty(int deathPenalty) {
        this.deathPenalty = deathPenalty;
    }


    /**
     * @return the sumTeamDeaths
     */
    public boolean isSumTeamDeaths() {
        return sumTeamDeaths;
    }


    /**
     * @param sumTeamDeaths the sumTeamDeaths to set
     */
    public void setSumTeamDeaths(boolean sumTeamDeaths) {
        this.sumTeamDeaths = sumTeamDeaths;
    }


    /**
     * @return the shorthand
     */
    public boolean isShorthand() {
        return shorthand;
    }


    /**
     * @param shorthand the shorthand to set
     */
    public void setShorthand(boolean shorthand) {
        this.shorthand = shorthand;
    }


    /**
     * @return the includeChests
     */
    public boolean isIncludeChests() {
        return includeChests;
    }


    /**
     * @param includeChests the includeChests to set
     */
    public void setIncludeChests(boolean includeChests) {
        this.includeChests = includeChests;
    }


    /**
     * @return the concurrentIslandCalcs
     */
    public int getConcurrentIslandCalcs() {
        if (concurrentIslandCalcs < 1) concurrentIslandCalcs = 1;
        return concurrentIslandCalcs;
    }


    /**
     * @param concurrentIslandCalcs the concurrentIslandCalcs to set
     */
    public void setConcurrentIslandCalcs(int concurrentIslandCalcs) {
        if (concurrentIslandCalcs < 1) concurrentIslandCalcs = 1;
        this.concurrentIslandCalcs = concurrentIslandCalcs;
    }


    /**
     * @return the zeroNewIslandLevels
     */
    public boolean isZeroNewIslandLevels() {
        return zeroNewIslandLevels;
    }


    /**
     * @param zeroNewIslandLevels the zeroNewIslandLevels to set
     */
    public void setZeroNewIslandLevels(boolean zeroNewIslandLevels) {
        this.zeroNewIslandLevels = zeroNewIslandLevels;
    }


    /**
     * @return the calculationTimeout
     */
    public int getCalculationTimeout() {
        return calculationTimeout;
    }


    /**
     * @param calculationTimeout the calculationTimeout to set
     */
    public void setCalculationTimeout(int calculationTimeout) {
        this.calculationTimeout = calculationTimeout;
    }

}
