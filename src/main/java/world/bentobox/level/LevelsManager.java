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
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.addon.AddonBaseEvent;
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
    private static final int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
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

    private final Database<TopTenData> topTenHandler;
    // Top ten lists
    private Map<World,TopTenData> topTenLists;
    // Background
    private final PanelItem background;



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
        // Background
        background = new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }

    /**
     * Add a score to the top players list
     * @param world - world
     * @param targetPlayer - target player
     * @param lv - island level
     */
    private void addToTopTen(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        // Get top ten
        Map<UUID, Long> topTen = topTenLists.computeIfAbsent(world, k -> new TopTenData(world)).getTopTen();
        // Remove this player from the top list no matter what (we'll put them back later if required)
        topTen.remove(targetPlayer);

        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        if (island != null && island.getOwner() != null && hasTopTenPerm(world, island.getOwner())) {
            // Insert the owner into the top ten
            topTen.put(island.getOwner(), lv);
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
            setIslandLevel(island.getWorld(), island.getOwner(), r.getLevel());
            addon.getManager().saveTopTen(island.getWorld());
            result.complete(r);
        });
        return result;
    }

    /**
     * Fires the IslandLevelCalculatedEvent and returns true if it is canceled
     * @param targetPlayer - target player
     * @param island - island
     * @param results - results set
     * @return true if canceled
     */
    private boolean fireIslandLevelCalcEvent(UUID targetPlayer, Island island, Results results) {
        // Fire post calculation event
        IslandLevelCalculatedEvent ilce = new IslandLevelCalculatedEvent(targetPlayer, island, results);
        Bukkit.getPluginManager().callEvent(ilce);
        if (ilce.isCancelled()) return true;
        // This exposes these values to plugins via the event
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("eventName", "IslandLevelCalculatedEvent");
        keyValues.put("targetPlayer", targetPlayer);
        keyValues.put("islandUUID", island.getUniqueId());
        keyValues.put("level", results.getLevel());
        keyValues.put("pointsToNextLevel", results.getPointsToNextLevel());
        keyValues.put("deathHandicap", results.getDeathHandicap());
        keyValues.put("initialLevel", results.getInitialLevel());
        keyValues.put("isCancelled", false);
        AddonBaseEvent e = new AddonEvent().builder().addon(addon).keyValues(keyValues).build();
        // Set the values if they were altered
        results.setLevel((Long)e.getKeyValues().getOrDefault("level", results.getLevel()));
        results.setInitialLevel((Long)e.getKeyValues().getOrDefault("initialLevel", results.getInitialLevel()));
        results.setDeathHandicap((int)e.getKeyValues().getOrDefault("deathHandicap", results.getDeathHandicap()));
        results.setPointsToNextLevel((Long)e.getKeyValues().getOrDefault("pointsToNextLevel", results.getPointsToNextLevel()));
        return ((Boolean)e.getKeyValues().getOrDefault("isCancelled", false));
    }

    /**
     * Get the string representation of the level. May be converted to shorthand notation, e.g., 104556 = 10.5k
     * @param lvl - long value to represent
     * @return string of the level.
     */
    public String formatLevel(@Nullable Long lvl) {
        if (lvl == null) return "";
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
                .size(54)
                .user(user);
        // Background
        for (int j = 0; j < 54; panel.item(j++, background));

        // Top Ten
        int i = 0;
        for (Entry<UUID, Long> m : topTen.entrySet()) {
            panel.item(SLOTS[i], getHead(i++, m.getValue(), m.getKey(), user, world));
        }
        // Show remaining slots
        for (; i < SLOTS.length; i++) {
            panel.item(SLOTS[i], new PanelItemBuilder().icon(Material.GREEN_STAINED_GLASS_PANE).name(String.valueOf(i + 1)).build());
        }

        // Add yourself
        panel.item(49, getHead(0, this.getIslandLevel(world, user.getUniqueId()), user.getUniqueId(), user, world));
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
        if (rank > 0) {
            description.add(asker.getTranslation("island.top.gui-heading", "[name]", name, "[rank]", String.valueOf(rank)));
        }
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
     * Get level of island from cache for a player.
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return Level of the player's island or zero if player is unknown or UUID is null
     */
    public long getIslandLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return 0L;
        // Get the island owner
        UUID owner = addon.getIslands().getOwner(world, targetPlayer);
        if (owner == null) return 0L;
        LevelsData ld = getLevelsData(owner);
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
     * Load a level data for the island owner from the cache or database. Only island onwers are stored.
     * @param islandOwner - UUID of island owner
     * @return LevelsData object or null if not found
     */
    @Nullable
    public LevelsData getLevelsData(@NonNull UUID islandOwner) {
        // Get from database if not in cache
        if (!levelsCache.containsKey(islandOwner) && handler.objectExists(islandOwner.toString())) {
            LevelsData ld = handler.loadObject(islandOwner.toString());
            if (ld != null) {
                levelsCache.put(islandOwner, ld);
            } else {
                handler.deleteID(islandOwner.toString());
            }
        }
        // Return cached value or null
        return levelsCache.get(islandOwner);
    }

    /**
     * Get the number of points required until the next level since the last level calc
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return string with the number required or blank if the player is unknown
     */
    public String getPointsToNextString(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return "";
        UUID owner = addon.getIslands().getOwner(world, targetPlayer);
        if (owner == null) return "";
        LevelsData ld = getLevelsData(owner);
        return ld == null ? "" : String.valueOf(ld.getPointsToNextLevel(world));
    }

    /**
     * Get the top ten for this world. Returns offline players or players with the intopten permission.
     * @param world - world requested
     * @param size - size of the top ten
     * @return sorted top ten map
     */
    @NonNull
    public Map<UUID, Long> getTopTen(@NonNull World world, int size) {
        topTenLists.computeIfAbsent(world, TopTenData::new);
        // Remove player from top ten if they are online and do not have the perm
        topTenLists.get(world).getTopTen().keySet().removeIf(u -> !hasTopTenPerm(world, u));
        // Return the sorted map
        return Collections.unmodifiableMap(topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(e -> addon.getIslands().isOwner(world, e.getKey()))
                .filter(l -> l.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(size)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
    }

    /**
     * Checks if player has the correct top ten perm to have their level saved
     * @param world
     * @param targetPlayer
     * @return true if player has the perm or the player is offline
     */
    boolean hasTopTenPerm(@NonNull World world, @NonNull UUID targetPlayer) {
        String permPrefix = addon.getPlugin().getIWM().getPermissionPrefix(world);
        return Bukkit.getPlayer(targetPlayer) == null || Bukkit.getPlayer(targetPlayer).hasPermission(permPrefix + INTOPTEN);
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
                // Remove any non island owners
                tt.getTopTen().keySet().removeIf(u -> !addon.getIslands().isOwner(world, u));
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
     * Save the top ten for world
     * @param world - world
     */
    public void saveTopTen(World world) {
        topTenHandler.saveObjectAsync(topTenLists.get(world));
    }

    /**
     * Set an initial island level for player
     * @param island - the island to set. Must have a non-null world and owner
     * @param lv - initial island level
     */
    public void setInitialIslandLevel(@NonNull Island island, long lv) {
        if (island.getOwner() == null || island.getWorld() == null) return;
        levelsCache.computeIfAbsent(island.getOwner(), LevelsData::new).setInitialLevel(island.getWorld(), lv);
        handler.saveObjectAsync(levelsCache.get(island.getOwner()));
    }

    /**
     * Set the island level for the owner of the island that targetPlayer is a member
     * @param world - world
     * @param targetPlayer - player, may be a team member
     * @param lv - level
     */
    public void setIslandLevel(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        levelsCache.computeIfAbsent(targetPlayer, LevelsData::new).setLevel(world, lv);
        handler.saveObjectAsync(levelsCache.get(targetPlayer));
        // Update TopTen
        addToTopTen(world, targetPlayer, levelsCache.get(targetPlayer).getLevel(world));
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
