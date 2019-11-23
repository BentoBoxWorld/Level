package world.bentobox.level.placeholders;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * @author tastybento
 *
 * @deprecated As of 1.9.0, for removal.
 */
@Deprecated
public class LevelPlaceholder implements PlaceholderReplacer {

    private final Level addon;
    private final GameModeAddon gm;

    /**
     * Provides placeholder support
     * @param addon - Level addon
     * @param gm - Game mode
     */
    public LevelPlaceholder(Level addon, GameModeAddon gm) {
        this.addon = addon;
        this.gm = gm;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
     */
    @Override
    public String onReplace(User user) {
        addon.logWarning("You are using a deprecated placeholder.");
        addon.log("Please replace any occurrence of 'Level_" + gm.getDescription().getName().toLowerCase() + "-island-level'");
        addon.log("by 'Level_" + gm.getDescription().getName().toLowerCase() + "_island_level'");
        return addon.getLevelPresenter().getLevelString(addon.getIslandLevel(gm.getOverWorld(), user.getUniqueId()));
    }

}
