package world.bentobox.level;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;

/**
 * @author tastybento
 *
 */
public class LevelPladdon extends Pladdon {

    @Override
    public Addon getAddon() {
        return new Level();
    }

}
