package bskyblock.addin.level;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Makes code look nicer
 * @author ben
 *
 */
public class LevelPlugin {
    protected Level plugin;
    protected BSkyBlock bSkyBlock;

    public LevelPlugin(Level plugin) {
        super();
        this.plugin = plugin;
        this.bSkyBlock = BSkyBlock.getPlugin();
    }
    
}
