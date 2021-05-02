package world.bentobox.level.requests;

import java.util.Collections;
import java.util.Map;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.addons.request.AddonRequestHandler;
import world.bentobox.level.Level;


/**
 * This Request Handler allows other plugins to get access to top 10 player list per particular world.
 * Handler returns linked hashmap from TopTenData for particular world.
 */
public class TopTenRequestHandler extends AddonRequestHandler {

    private static final String WORLD_NAME = "world-name";
    /**
     * The level addon field.
     */
    private final Level addon;

    /**
     * This constructor creates a new TopTenRequestHandler instance.
     *
     * @param addon of type Level
     */
    public TopTenRequestHandler(Level addon) {
        super("top-ten-level");
        this.addon = addon;
    }

    /**
     * See {@link AddonRequestHandler#handle(Map)}
     */
    @Override
    public Object handle(Map<String, Object> map) {
        /*
            What we need in the map:

            "world-name" -> String

            What we will return:

            - Empty map if invalid input
            - the map of top ten player UUIDs and their island levels. Can be less then 10.
         */

        if (map == null || map.isEmpty()
                || map.get(WORLD_NAME) == null || !(map.get(WORLD_NAME) instanceof String)
                || Bukkit.getWorld((String) map.get(WORLD_NAME)) == null) {
            return Collections.emptyMap();
        }

        // No null check required
        return addon.getManager().getTopTen(Bukkit.getWorld((String) map.get(WORLD_NAME)), Level.TEN);
    }
}
