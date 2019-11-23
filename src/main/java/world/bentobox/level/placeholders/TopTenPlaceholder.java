package world.bentobox.level.placeholders;

import java.util.Collection;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * Provides the level values to placeholders
 * @author tastybento
 * @deprecated As of 1.9.0, for removal.
 */
@Deprecated
public class TopTenPlaceholder implements PlaceholderReplacer {

    private final Level level;
    private final GameModeAddon gm;
    private final int i;

    public TopTenPlaceholder(Level level, GameModeAddon gm, int i) {
        this.level = level;
        this.gm = gm;
        this.i = i - 1;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
     */
    @Override
    public String onReplace(User user) {
        level.logWarning("You are using a deprecated placeholder.");
        level.log("Please replace any occurrence of 'Level_" + gm.getDescription().getName().toLowerCase() + "-island-top-value-#'");
        level.log("by 'Level_" + gm.getDescription().getName().toLowerCase() + "_top_value_#'");
        Collection<Long> values = level.getTopTen().getTopTenList(gm.getOverWorld()).getTopTen().values();
        return values.size() < i ? "" : values.stream().skip(i).findFirst().map(String::valueOf).orElse("");
    }

}
