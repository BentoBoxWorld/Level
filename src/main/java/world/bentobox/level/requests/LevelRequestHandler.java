package world.bentobox.level.requests;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.addons.request.AddonRequestHandler;
import world.bentobox.level.Level;

public class LevelRequestHandler extends AddonRequestHandler {

    private static final Object WORLD_NAME = "world-name";
    private static final Object PLAYER = "player";
    private final Level addon;

    public LevelRequestHandler(Level addon) {
        super("island-level");
        this.addon = addon;
    }

    @Override
    public Object handle(Map<String, Object> map) {
        /*
            What we need in the map:

            0. "world-name" -> String
            1. "player" -> UUID

            What we will return:

            - 0L if invalid input/player has no island
            - the island level otherwise (which may be 0)
         */

        if (map == null || map.isEmpty()
                || map.get(WORLD_NAME) == null || !(map.get(WORLD_NAME) instanceof String)
                || map.get(PLAYER) == null || !(map.get(PLAYER) instanceof UUID)
                || Bukkit.getWorld((String) map.get(WORLD_NAME)) == null) {
            return 0L;
        }

        return addon.getManager().getIslandLevel(Bukkit.getWorld((String) map.get(WORLD_NAME)), (UUID) map.get(PLAYER));
    }
}
