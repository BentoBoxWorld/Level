package world.bentobox.level;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.Maps;

import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.events.IslandLevelCalculatedEvent;
import world.bentobox.level.events.IslandPreLevelEvent;
import world.bentobox.level.objects.IslandLevels;
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
    private final Level addon;

    // Database handler for level data
    private final Database<IslandLevels> handler;
    // A cache of island levels.
    private final Map<String, IslandLevels> levelsCache;
    // Top ten lists
    private final Map<World,TopTenData> topTenLists;


    public LevelsManager(Level addon) {
        this.addon = addon;
        // Get the BentoBox database
        // Set up the database handler to store and retrieve data
        // Note that these are saved by the BentoBox database
        handler = new Database<>(addon, IslandLevels.class);
        // Initialize the cache
        levelsCache = new HashMap<>();
        // Initialize top ten lists
        topTenLists = new ConcurrentHashMap<>();
    }

    public void migrate() {
        Database<LevelsData> oldDb = new Database<>(addon, LevelsData.class);
        oldDb.loadObjects().forEach(ld -> {
            try {
                UUID owner = UUID.fromString(ld.getUniqueId());
                // Step through each world
                ld.getLevels().keySet().stream()
                // World
                .map(Bukkit::getWorld).filter(Objects::nonNull)
                // Island
                .map(w -> addon.getIslands().getIsland(w, owner)).filter(Objects::nonNull)
                .forEach(i -> {
                    // Make new database entry
                    World w = i.getWorld();
                    IslandLevels il = new IslandLevels(i.getUniqueId());
                    il.setInitialLevel(ld.getInitialLevel(w));
                    il.setLevel(ld.getLevel(w));
                    il.setMdCount(ld.getMdCount(w));
                    il.setPointsToNextLevel(ld.getPointsToNextLevel(w));
                    il.setUwCount(ld.getUwCount(w));
                    // Save it
                    handler.saveObjectAsync(il);
                });
                // Now delete the old database entry
                oldDb.deleteID(ld.getUniqueId());
            } catch (Exception e) {
                addon.logError("Could not migrate level data database! " + e.getMessage());
                e.printStackTrace();
                return;
            }
        });
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
     * Add an island to a top ten
     * @param island - island to add
     * @param lv - level
     * @return true if successful, false if not added
     */
    private boolean addToTopTen(Island island, long lv) {
        if (island != null && island.getOwner() != null && hasTopTenPerm(island.getWorld(), island.getOwner())) {
            topTenLists.computeIfAbsent(island.getWorld(), k -> new TopTenData(island.getWorld()))
            .getTopTen().put(island.getOwner(), lv);
            return true;
        }
        return false;
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
            setIslandResults(island.getWorld(), island.getOwner(), r);
            // Save the island scan details
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
        // Set the values if they were altered
        results.setLevel((Long)ilce.getKeyValues().getOrDefault("level", results.getLevel()));
        results.setInitialLevel((Long)ilce.getKeyValues().getOrDefault("initialLevel", results.getInitialLevel()));
        results.setDeathHandicap((int)ilce.getKeyValues().getOrDefault("deathHandicap", results.getDeathHandicap()));
        results.setPointsToNextLevel((Long)ilce.getKeyValues().getOrDefault("pointsToNextLevel", results.getPointsToNextLevel()));
        results.setTotalPoints((Long)ilce.getKeyValues().getOrDefault("totalPoints", results.getTotalPoints()));
        return ((Boolean)ilce.getKeyValues().getOrDefault("isCancelled", false));
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
     * Get the initial level of the island. Used to zero island levels
     * @param island - island
     * @return initial level of island
     */
    public long getInitialLevel(Island island) {
        return getLevelsData(island).getInitialLevel();
    }

    /**
     * Get level of island from cache for a player.
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return Level of the player's island or zero if player is unknown or UUID is null
     */
    public long getIslandLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return 0L;
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        return island == null ? 0L : getLevelsData(island).getLevel();
    }

    /**
     * Get the maximum level ever given to this island
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return Max level of the player's island or zero if player is unknown or UUID is null
     */
    public long getIslandMaxLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return 0L;
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        return island == null ? 0L : getLevelsData(island).getMaxLevel();
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
     * Load a level data for the island from the cache or database.
     * @param island - UUID of island
     * @return IslandLevels object
     */
    @NonNull
    public IslandLevels getLevelsData(@NonNull Island island) {
        String id = island.getUniqueId();
        if (levelsCache.containsKey(id)) {
            return levelsCache.get(id);
        }
        // Get from database if not in cache
        if (handler.objectExists(id)) {
            IslandLevels ld = handler.loadObject(id);
            if (ld != null) {
                levelsCache.put(id, ld);
            } else {
                handler.deleteID(id);
                levelsCache.put(id, new IslandLevels(id));
            }
        } else {
            levelsCache.put(id, new IslandLevels(id));
        }
        // Return cached value
        return levelsCache.get(id);
    }

    /**
     * Get the number of points required until the next level since the last level calc
     * @param world - world where the island is
     * @param targetPlayer - target player UUID
     * @return string with the number required or blank if the player is unknown
     */
    public String getPointsToNextString(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null) return "";
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        return island == null ? "" : String.valueOf(getLevelsData(island).getPointsToNextLevel());
    }

    /**
     * Get the top ten for this world. Returns offline players or players with the intopten permission.
     * @param world - world requested
     * @param size - size of the top ten
     * @return sorted top ten map
     */
    @NonNull
    public Map<UUID, Long> getTopTen(@NonNull World world, int size) {
        createAndCleanRankings(world);
        // Return the sorted map
        return Collections.unmodifiableMap(topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(e -> addon.getIslands().isOwner(world, e.getKey()))
                .filter(l -> l.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(size)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
    }

    void createAndCleanRankings(@NonNull World world) {
        topTenLists.computeIfAbsent(world, TopTenData::new);
        // Remove player from top ten if they are online and do not have the perm
        topTenLists.get(world).getTopTen().keySet().removeIf(u -> !hasTopTenPerm(world, u));
    }

    /**
     * @return the topTenLists
     */
    protected Map<World, TopTenData> getTopTenLists() {
        return topTenLists;
    }

    /**
     * Get the rank of the player in the rankings
     * @param world - world
     * @param uuid - player UUID
     * @return rank placing - note - placing of 1 means top ranked
     */
    public int getRank(@NonNull World world, UUID uuid) {
        createAndCleanRankings(world);
        Stream<Entry<UUID, Long>> stream = topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(e -> addon.getIslands().isOwner(world, e.getKey()))
                .filter(l -> l.getValue() > 0)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()));
        return (int) (stream.takeWhile(x -> !x.getKey().equals(uuid)).map(Map.Entry::getKey).count() + 1);
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
    public void loadTopTens() {
        topTenLists.clear();
        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            addon.log("Generating rankings");
            handler.loadObjects().forEach(il -> {
                if (il.getLevel() > 0) {
                    addon.getIslands().getIslandById(il.getUniqueId()).ifPresent(i -> this.addToTopTen(i, il.getLevel()));
                }
            });
            topTenLists.keySet().forEach(w -> addon.log("Generated rankings for " + w.getName()));
        });
    }

    /**
     * Removes a player from a world's top ten and removes world from player's level data
     * @param world - world
     * @param uuid - the player's uuid
     */
    public void removeEntry(World world, UUID uuid) {
        if (topTenLists.containsKey(world)) {
            topTenLists.get(world).getTopTen().remove(uuid);
        }

    }

    /**
     * Set an initial island level
     * @param island - the island to set. Must have a non-null world
     * @param lv - initial island level
     */
    public void setInitialIslandLevel(@NonNull Island island, long lv) {
        if (island.getWorld() == null) return;
        levelsCache.computeIfAbsent(island.getUniqueId(), IslandLevels::new).setInitialLevel(lv);
        handler.saveObjectAsync(levelsCache.get(island.getUniqueId()));
    }

    /**
     * Set the island level for the owner of the island that targetPlayer is a member
     * @param world - world
     * @param targetPlayer - player, may be a team member
     * @param lv - level
     */
    public void setIslandLevel(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        if (island != null) {
            String id = island.getUniqueId();
            IslandLevels il = levelsCache.computeIfAbsent(id, IslandLevels::new);
            // Remove the initial level
            if (addon.getSettings().isZeroNewIslandLevels()) {
                il.setLevel(lv - il.getInitialLevel());
            } else {
                il.setLevel(lv);
            }
            handler.saveObjectAsync(levelsCache.get(id));
            // Update TopTen
            addToTopTen(world, targetPlayer, levelsCache.get(id).getLevel());
        }

    }

    /**
     * Set the island level for the owner of the island that targetPlayer is a member
     * @param world - world
     * @param owner - owner of the island
     * @param r - results of the calculation
     */
    private void setIslandResults(World world, @NonNull UUID owner, Results r) {
        // Get the island
        Island island = addon.getIslands().getIsland(world, owner);
        if (island == null) return;
        IslandLevels ld = levelsCache.computeIfAbsent(island.getUniqueId(), IslandLevels::new);
        ld.setLevel(r.getLevel());
        ld.setUwCount(Maps.asMap(r.getUwCount().elementSet(), elem -> r.getUwCount().count(elem)));
        ld.setMdCount(Maps.asMap(r.getMdCount().elementSet(), elem -> r.getMdCount().count(elem)));
        ld.setPointsToNextLevel(r.getPointsToNextLevel());
        ld.setTotalPoints(r.getTotalPoints());
        levelsCache.put(island.getUniqueId(), ld);
        handler.saveObjectAsync(ld);
        // Update TopTen
        addToTopTen(world, owner, ld.getLevel());
    }

    /**
     * Removes island from cache when it is deleted
     * @param uniqueId - id of island
     */
    public void deleteIsland(String uniqueId) {
        levelsCache.remove(uniqueId);
        handler.deleteID(uniqueId);
    }

}
