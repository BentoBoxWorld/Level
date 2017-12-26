package bskyblock.addin.level;

import java.util.logging.Logger;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Makes code look nicer
 * @author ben
 *
 */
public abstract class LevelPlugin {
    protected final Level plugin;
    protected final BSkyBlock bSkyBlock;

    public LevelPlugin(Level plugin) {
        this.plugin = plugin;
        this.bSkyBlock = BSkyBlock.getPlugin();
    }
    
    public final Logger getLogger() {
        return plugin.getLogger();
    }
  
}
