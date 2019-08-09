package world.bentobox.level.placeholders;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * @author tastybento
 *
 */
public class LevelPlaceholder implements PlaceholderReplacer {

    private Level addon;
    private GameModeAddon gm;

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
        return addon.getLevelPresenter().getLevelString(addon.getIslandLevel(gm.getOverWorld(), user.getUniqueId()));
    }

}
