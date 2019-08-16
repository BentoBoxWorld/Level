package world.bentobox.level.placeholders;

import java.util.Collection;
import java.util.UUID;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.Level;

/**
 * @author tastybento
 *
 */
public class TopTenNamePlaceholder implements PlaceholderReplacer {

    private final Level level;
    private final GameModeAddon gm;
    private final int i;

    public TopTenNamePlaceholder(Level level, GameModeAddon gm, int i) {
        this.level = level;
        this.gm = gm;
        this.i = i - 1;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.placeholders.PlaceholderReplacer#onReplace(world.bentobox.bentobox.api.user.User)
     */
    @Override
    public String onReplace(User user) {
        Collection<UUID> values = level.getTopTen().getTopTenList(gm.getOverWorld()).getTopTen().keySet();
        return values.size() < i ? "" : level.getPlayers().getName(values.stream().skip(i).findFirst().orElse(null));
    }

}
