package world.bentobox.level;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.addon.AddonEvent;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.events.IslandLevelCalculatedEvent;
import world.bentobox.level.events.IslandPreLevelEvent;
import world.bentobox.level.objects.LevelsData;
import world.bentobox.level.objects.TopTenData;

public class LevelsManager {
    private static final String INTOPTEN = "intopten";
    private static final TreeMap<BigInteger, String> LEVELS;
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    static {
        LEVELS = new TreeMap<>();

        LEVELS.put(THOUSAND, "k");
        LEVELS.put(THOUSAND.pow(2), "M");
        LEVELS.put(THOUSAND.pow(3), "G");
        LEVELS.put(THOUSAND.pow(4), "T");
    }
    private Level addon;


    // Database handler for level data
    private final Database<LevelsData> handler;
    // A cache of island levels.
    private final Map<UUID, LevelsData> levelsCache;

    private final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
    private final Database<TopTenData> topTenHandler;
    // Top ten lists
    private Map<World,TopTenData> topTenLists;


    public LevelsManager(Level addon) {
        this.addon = addon;
        // Get the BentoBox database
        // Set up the database handler to store and retrieve data
        // Note that these are saved by the BentoBox database
        handler = new Database<>(addon, LevelsData.class);
        // Top Ten handler
        topTenHandler = new Database<>(addon, TopTenData.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Initialize top ten lists
        topTenLists = new HashMap<>();
    }

    private void addToTopTen(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        topTenLists.computeIfAbsent(world, k -> new TopTenData(world));
        if (hasTopTenPerm(world, targetPlayer)) {
            topTenLists.get(world).getTopTen().put(targetPlayer, lv);
        } else {
            topTenLists.get(world).getTopTen().remove(targetPlayer);
        }

    }

    /**
     * Calculate the island level, set all island member's levels to the result and try to add the owner to the top ten
     * @param targetPlayer - uuid of targeted player - owner or team member
     * @param island - island to calculate
     * @return completable future with the results of the calculation
     */
    public CompletableFuture<Results> calculateLevel(UUID targetPlayer, Island island) {
        CompletableFuture<Results> result = new CompletableFuture<>();
        // Fire pre-level calc event
        IslandPreLevelEvent e = new IslandPreLevelEvent(targetPlayer, island);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return CompletableFuture.completedFuture(null);
        }
        // Add island to the pipeline
        addon.getPipeliner().addIsland(island).thenAccept(r -> {
            // Results are irrelevant because the island is unowned or deleted, or IslandLevelCalcEvent is cancelled
            if (r == null || fireIslandLevelCalcEvent(targetPlayer, island, r)) {
                result.complete(null);
            }
            // Save result
            island.getMemberSet().forEach(uuid -> setIslandLevel(island.getWorld(), uuid, r.getLevel()));
            addToTopTen(island.getWorld(), island.getOwner(), r.getLevel());
            result.complete(r);
        });
        return result;
    }

    private boolean fireIslandLevelCalcEvent(UUID targetPlayer, Island island, Results results) {
        // Fire post calculation event
        IslandLevelCalculatedEvent ilce = new IslandLevelCalculatedEvent(targetPlayer, island, results);
        Bukkit.getPluginManager().callEvent(ilce);
        // This exposes these values to plugins via the event
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("eventName", "IslandLevelCalculatedEvent");
        keyValues.put("targetPlayer", targetPlayer);
        keyValues.put("islandUUID", island.getUniqueId());
        keyValues.put("level", results.getLevel());
        keyValues.put("pointsToNextLevel", results.getPointsToNextLevel());
        keyValues.put("deathHandicap", results.getDeathHandicap());
        keyValues.put("initialLevel", results.getInitialLevel());
        new AddonEvent().builder().addon(addon).keyValues(keyValues).build();
        results = ilce.getResults();
        return ilce.isCancelled();
    }

    /**
     * Get the string representation of the level. May be converted to shorthand notation, e.g., 104556 = 10.5k
     * @param lvl - long value to represent
     * @return string of the level.
     */
    public String formatLevel(long lvl) {
        String level = String.valueOf(lvl);
        // Asking for the level of another player
        if(addon.getSettings().isShorthand()) {
            BigInteger levelValue = BigInteger.valueOf(lvl);

            Map.Entry<BigInteger, String> stage = LEVELS.floorEntry(levelValue);

            if (stage != null) { // level > 1000
                // 1 052 -> 1.0k
                // 1 527 314 -> 1.5M
                // 3 874 130 021 -> 3.8G
                // 4 002 317 889 -> 4.0T
                level = new DecimalFormat("#.#").format(levelValue.divide(stage.getKey().divide(THOUSAND)).doubleValue()/1000.0) + stage.getValue();
            }
        }
        return level;
    }

    /**
     * Displays the Top Ten list
     * @param world - world
     * @param user - the requesting player
     */
    public void getGUI(World world, final User user) {
        // Check world
        Map<UUID, Long> topTen = getTopTen(world, 10);

        PanelBuilder panel = new PanelBuilder()
                .name(user.getTranslation("island.top.gui-title"))
                .user(user);
        int i = 0;
        for (Entry<UUID, Long> m : topTen.entrySet()) {
            panel.item(SLOTS[i], getHead(i + 1, m.getValue(), m.getKey(), user, world));
        }
        panel.build();
    }

    /**
     * Get the head panel item
     * @param rank - the top ten rank of this player/team. Can be used in the name of the island for vanity.
     * @param level - the level of the island
     * @param playerUUID - the UUID of the top ten player
     * @param asker - the asker of the top ten
     * @return PanelItem
     */
    private PanelItem getHead(int rank, long level, UUID playerUUID, User asker, World world) {
        final String name = addon.getPlayers().getName(playerUUID);
        List<String> description = new ArrayList<>();
        description.add(asker.getTranslation("island.top.gui-heading", "[name]", name, "[rank]", String.valueOf(rank)));
        description.add(asker.getTranslation("island.top.island-level","[level]", formatLevel(level)));
        if (addon.getIslands().inTeam(world, playerUUID)) {
            List<String> memberList = new ArrayList<>();
            for (UUID members : addon.getIslands().getMembers(world, playerUUID)) {
                memberList.add(ChatColor.AQUA + addon.getPlayers().getName(members));
            }
            description.addAll(memberList);
        }

        PanelItemBuilder builder = new PanelItemBuilder()
                .icon(name)
                .name(name)
                .description(description);
        return builder.build();
    }

    /**
     * Get the initial level of the island. Used to zero island levels
     * @param island - island
     * @return initial level of island
     */
    public long getInitialLevel(Island island) {
        @Nullable
        LevelsData ld = getLevelsData(island.getOwner());
        return ld == null ? 0 : ld.getInitialLevel(island.getWorld());
    }

    /**
     * Get level from cache for a player.
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return Level of player or zero if player is unknown or UUID is null
     */
    public long getIslandLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return 0L;
        LevelsData ld = getLevelsData(targetPlayer);
        return ld == null ? 0L : ld.getLevel(world);
    }

    /**
     * Returns a formatted string of the target player's island level
     * @param world - world where the island is
     * @param targetPlayer - target player's UUID
     * @return Formatted level of player or zero if player is unknown or UUID is null
     */
    public String getIslandLevelString(@NonNull World world, @Nullable UUID targetPlayer) {
        return formatLevel(getIslandLevel(world, targetPlayer));
    }

    /**
     * Load a player from the cache or database
     * @param targetPlayer - UUID of target player
     * @return LevelsData object or null if not found
     */
    @Nullable
    public LevelsData getLevelsData(@NonNull UUID targetPlayer) {
        // Get from database if not in cache
        if (!levelsCache.containsKey(targetPlayer) && handler.objectExists(targetPlayer.toString())) {
            LevelsData ld = handler.loadObject(targetPlayer.toString());
            if (ld != null) {
                levelsCache.put(targetPlayer, ld);
            } else {
                handler.deleteID(targetPlayer.toString());
            }
        }
        // Return cached value or null
        return levelsCache.get(targetPlayer);
    }

    /**
     * Get the number of points required until the next level since the last level calc
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return string with the number required or blank if the player is unknown
     */
    public String getPointsToNextString(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return "";
        LevelsData ld = getLevelsData(targetPlayer);
        return ld == null ? "" : String.valueOf(ld.getPointsToNextLevel(world));
    }

    /**
     * Get the top ten for this world. Returns offline players or players with the intopten permission.
     * @param world - world requested
     * @param size - size of the top ten
     * @return sorted top ten map
     */
    public Map<UUID, Long> getTopTen(World world, int size) {
        topTenLists.computeIfAbsent(world, k -> new TopTenData(k));
        // Remove player from top ten if they are online and do not have the perm
        topTenLists.get(world).getTopTen().keySet().removeIf(u -> !hasTopTenPerm(world, u));
        // Return the sorted map
        return Collections.unmodifiableMap(topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(l -> l.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(size)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
    }

    /**
     * Checks if player has the correct top ten perm to have their level saved
     * @param world
     * @param targetPlayer
     * @return
     */
    boolean hasTopTenPerm(@NonNull World world, @NonNull UUID targetPlayer) {
        String permPrefix = addon.getPlugin().getIWM().getPermissionPrefix(world);
        boolean hasPerm = Bukkit.getPlayer(targetPlayer) != null && Bukkit.getPlayer(targetPlayer).hasPermission(permPrefix + INTOPTEN);
        return hasPerm;
    }

    /**
     * Loads all the top tens from the database
     */
    void loadTopTens() {
        topTenLists = new HashMap<>();
        topTenHandler.loadObjects().forEach(tt -> {
            World world = Bukkit.getWorld(tt.getUniqueId());
            if (world != null) {
                topTenLists.put(world, tt);
                addon.log("Loaded TopTen for " + world.getName());
                // Update based on user data
                for (UUID uuid : tt.getTopTen().keySet()) {
                    tt.getTopTen().compute(uuid, (k,v) -> v = updateLevel(k, world));
                }
            } else {
                addon.logError("TopTen world '" + tt.getUniqueId() + "' is not known on server. You might want to delete this table. Skipping...");
            }
        });
    }

    /**
     * Removes a player from a world's top ten and removes world from player's level data
     * @param world - world
     * @param uuid - the player's uuid
     */
    public void removeEntry(World world, UUID uuid) {
        if (levelsCache.containsKey(uuid)) {
            levelsCache.get(uuid).remove(world);
            // Save
            handler.saveObjectAsync(levelsCache.get(uuid));
        }
        if (topTenLists.containsKey(world)) {
            topTenLists.get(world).getTopTen().remove(uuid);
            topTenHandler.saveObjectAsync(topTenLists.get(world));
        }

    }

    /**
     * Saves all player data and the top ten
     */
    public void save() {
        levelsCache.values().forEach(handler::saveObjectAsync);
        topTenLists.values().forEach(topTenHandler::saveObjectAsync);
    }

    /**
     * Set an initial island level for player
     * @param island - the island to set. Must have a non-null world and owner
     * @param lv - initial island level
     */
    public void setInitialIslandLevel(@NonNull Island island, long lv) {
        if (island.getOwner() == null || island.getWorld() == null) return;
        levelsCache.computeIfAbsent(island.getOwner(), k -> new LevelsData(k)).setInitialLevel(island.getWorld(), lv);
        handler.saveObjectAsync(levelsCache.get(island.getOwner()));
    }

    public void setIslandLevel(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        levelsCache.computeIfAbsent(targetPlayer, k -> new LevelsData(targetPlayer)).setLevel(world, lv);
        handler.saveObjectAsync(levelsCache.get(targetPlayer));
        // Add to Top Ten
        addToTopTen(world, targetPlayer, lv);
    }

    private Long updateLevel(UUID uuid, World world) {
        if (handler.objectExists(uuid.toString())) {
            @Nullable
            LevelsData ld = handler.loadObject(uuid.toString());
            if (ld != null) {
                return ld.getLevel(world);
            }
        }
        return 0L;
    }



}
