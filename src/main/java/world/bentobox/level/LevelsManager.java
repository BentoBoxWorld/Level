package world.bentobox.level;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.Maps;

import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.calculators.EquationEvaluator;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.events.IslandLevelCalculatedEvent;
import world.bentobox.level.events.IslandPreLevelEvent;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.objects.TopTenData;
import world.bentobox.level.util.CachedData;

public class LevelsManager {
    private static final String INTOPTEN = "intopten";
    private static final TreeMap<BigInteger, String> LEVELS = new TreeMap<>();
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    private final Level addon;

    // Database handler for level data
    private final Database<IslandLevels> handler;
    // A cache of island levels.
    private final Map<String, IslandLevels> levelsCache;
    // Top ten lists
    private final Map<World, TopTenData> topTenLists;
    // Cache for top tens
    private Map<World, CachedData> cache = new HashMap<>();
    /**
     * Per-island in-flight zero-scan counter. Incremented when
     * {@link NewChunkListener} schedules a delayed snapshot for a freshly
     * generated chunk, decremented when that snapshot has been processed and
     * its value folded into {@link #addToInitialCount}. The level scan calls
     * {@link #awaitPendingZeros} so it never returns a result while there is
     * unaccounted-for handicap value still queued.
     */
    private final Map<String, AtomicInteger> pendingZeros = new ConcurrentHashMap<>();
    /**
     * Per-island record of chunk positions visited by the active zero-island
     * scan. Populated as the scan reads each chunk's snapshot; used by the
     * post-scan drain to skip chunks that the scan already credited in
     * {@code totalPoints} (preventing double-counting when the chunk listener
     * also fires for the same chunk during the scan window).
     */
    private final Map<String, Set<Long>> zeroScanVisitedChunks = new ConcurrentHashMap<>();
    /**
     * Per-island deferred listener credits captured while a zero-island scan
     * is in progress. Without this, listener {@code addToInitialCount} calls
     * for chunks the scan SKIPPED (ungenerated at poll time, generated
     * mid-scan) would be wiped by the post-scan
     * {@link #setInitialIslandCount setInitialIslandCount(totalPoints)}, and
     * those chunks' values would appear in future scan totals with no
     * matching handicap — producing a stable positive level on a fresh
     * island.
     */
    private final Map<String, Map<Long, Long>> zeroScanDeferredCredits = new ConcurrentHashMap<>();

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
        // Units
        LEVELS.put(THOUSAND, addon.getSettings().getKilo());
        LEVELS.put(THOUSAND.pow(2), addon.getSettings().getMega());
        LEVELS.put(THOUSAND.pow(3), addon.getSettings().getGiga());
        LEVELS.put(THOUSAND.pow(4), addon.getSettings().getTera());

    }

    /**
     * Add an island to a top ten
     * 
     * @param island - island to add
     * @param lv     - level
     * @return true if successful, false if not added
     */
    private boolean addToTopTen(Island island, long lv) {
        if (island != null && island.getOwner() != null && island.getWorld() != null
                && hasTopTenPerm(island.getWorld(), island.getOwner())) {
            topTenLists.computeIfAbsent(island.getWorld(), k -> new TopTenData(island.getWorld())).getTopTen()
                    .put(island.getUniqueId(), lv);
            // Invalidate the cache for this world because of the update
            cache.remove(island.getWorld());
            return true;
        }
        return false;
    }

    /**
     * Calculate the island level, set all island member's levels to the result and
     * try to add the owner to the top ten
     * 
     * @param targetPlayer - uuid of targeted player - owner or team member
     * @param island       - island to calculate
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
            // Results are irrelevant because the island is unowned or deleted, or
            // IslandLevelCalcEvent is cancelled
            if (r == null || fireIslandLevelCalcEvent(targetPlayer, island, r)) {
                result.complete(null);
            }
            // Save result
            setIslandResults(island, r);
            // Save the island scan details
            result.complete(r);
        });
        return result;
    }

    /**
     * Fires the IslandLevelCalculatedEvent and returns true if it is canceled
     * 
     * @param targetPlayer - target player
     * @param island       - island
     * @param results      - results set
     * @return true if canceled
     */
    private boolean fireIslandLevelCalcEvent(UUID targetPlayer, Island island, Results results) {
        // Fire post calculation event
        IslandLevelCalculatedEvent ilce = new IslandLevelCalculatedEvent(targetPlayer, island, results);
        Bukkit.getPluginManager().callEvent(ilce);
        if (ilce.isCancelled())
            return true;
        // Set the values if they were altered
        results.setLevel((Long) ilce.getKeyValues().getOrDefault("level", results.getLevel()));
        results.setInitialCount((Long) ilce.getKeyValues().getOrDefault("initialCount", results.getInitialCount()));
        results.setDeathHandicap((int) ilce.getKeyValues().getOrDefault("deathHandicap", results.getDeathHandicap()));
        results.setPointsToNextLevel(
                (Long) ilce.getKeyValues().getOrDefault("pointsToNextLevel", results.getPointsToNextLevel()));
        results.setTotalPoints((Long) ilce.getKeyValues().getOrDefault("totalPoints", results.getTotalPoints()));
        return ((Boolean) ilce.getKeyValues().getOrDefault("isCancelled", false));
    }

    /**
     * Get the string representation of the level. May be converted to shorthand
     * notation, e.g., 104556 = 10.5k
     * 
     * @param lvl - long value to represent
     * @return string of the level.
     */
    public String formatLevel(@Nullable Long lvl) {
        if (lvl == null)
            return "";
        String level = String.valueOf(lvl);
        // Asking for the level of another player
        if (addon.getSettings().isShorthand()) {
            BigInteger levelValue = BigInteger.valueOf(lvl);

            Map.Entry<BigInteger, String> stage = LEVELS.floorEntry(levelValue);

            if (stage != null) { // level > 1000
                // 1 052 -> 1.0k
                // 1 527 314 -> 1.5M
                // 3 874 130 021 -> 3.8G
                // 4 002 317 889 -> 4.0T
                level = new DecimalFormat("#.#").format(
                        levelValue.divide(stage.getKey().divide(THOUSAND)).doubleValue() / 1000.0) + stage.getValue();
            }
        }
        return level;
    }

    /**
     * Get the initial count of the island. Used to zero island levels
     * 
     * @param island - island
     * @return initial count of island
     */
    @SuppressWarnings("deprecation")
    public long getInitialCount(Island island) {
        Long initialLevel = getLevelsData(island).getInitialLevel(); // Backward compatibility check. For all new islands, this should be null.
        Long initialCount = getLevelsData(island).getInitialCount();
        if (initialLevel != null) {
            // Initial level exists so convert it
            if (initialCount == null) { // If initialCount is not null, then this is an edge case and initialCount will be used and initialLevel discarded
                // Convert from level to count
                initialCount = 0L;
                try {
                    initialCount = getNumBlocks(initialLevel);
                } catch (Exception e) {
                    addon.logError("Could not convert legacy initial level to count, so it will be set to 0. Error is: "
                            + e.getLocalizedMessage());
                    initialCount = 0L;
                }
            }
            // Null out the old initial level and save
            getLevelsData(island).setInitialLevel(null);
            // Save
            this.setInitialIslandCount(island, initialCount);
        }
        // If initialCount doesn't exist, set it to 0L
        if (initialCount == null) {
            initialCount = 0L;
            getLevelsData(island).setInitialCount(0L);
        }
        return initialCount;
    }

    /**
     * Runs the level calculation using the current formula until the level matches the initial value, or fails.
     * @param initialLevel - the old initial level
     * @return block count to obtain this level now
     * @throws ParseException if the formula for level calc is bugged
     * @throws IOException if the number of blocks cannot be found for this level
     */
    private long getNumBlocks(final long initialLevel) throws ParseException, IOException {
        String calcString = addon.getSettings().getLevelCalc();
        int result = -1;
        long calculatedLevel = 0;
        String withCost = calcString.replace("level_cost", String.valueOf(this.addon.getSettings().getLevelCost()));
        long time = System.currentTimeMillis() + 10 * 1000; // 10 seconds
        do {
            result++;
            if (System.currentTimeMillis() > time) {
                throw new IOException("Timeout: Blocks cannot be found to create this initial level");
            }
            // Paste in the values to the formula
            String withValues = withCost.replace("blocks", String.valueOf(result));
            // Try and evaluate it
            calculatedLevel = (long) EquationEvaluator.eval(withValues);
        } while (calculatedLevel != initialLevel);
        return result;
    }

    /**
     * Get level of island from cache for a player.
     * 
     * @param world        - world where the island is
     * @param targetPlayer - target player UUID
     * @return Level of the player's island or zero if player is unknown or UUID is
     *         null
     */
    public long getIslandLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        return getIslandLevel(world, targetPlayer, false);
    }

    /**
     * Get level of island from cache for a player.
     * 
     * @param world        - world where the island is
     * @param targetPlayer - target player UUID
     * @param ownerOnly    - return level only if the target player is the owner
     * @return Level of the player's island or zero if player is unknown or UUID is
     *         null
     */
    public long getIslandLevel(@NonNull World world, @Nullable UUID targetPlayer, boolean ownerOnly) {
        if (targetPlayer == null)
            return 0L;
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        if (island == null || island.getOwner() == null || (ownerOnly && !island.getOwner().equals(targetPlayer))) {
            return 0L;
        }
        return getLevelsData(island).getLevel();
    }

    /**
     * Get the maximum level ever given to this island
     * 
     * @param world        - world where the island is
     * @param targetPlayer - target player UUID
     * @return Max level of the player's island or zero if player is unknown or UUID
     *         is null
     */
    public long getIslandMaxLevel(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null)
            return 0L;
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        return island == null ? 0L : getLevelsData(island).getMaxLevel();
    }

    /**
     * Returns a formatted string of the target player's island level
     * 
     * @param world        - world where the island is
     * @param targetPlayer - target player's UUID
     * @return Formatted level of player or zero if player is unknown or UUID is
     *         null
     */
    public String getIslandLevelString(@NonNull World world, @Nullable UUID targetPlayer) {
        return formatLevel(getIslandLevel(world, targetPlayer));
    }

    /**
     * Load a level data for the island from the cache or database.
     * 
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
                // Clean up just in case 
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
     * Get the number of points required until the next level since the last level
     * calc
     * 
     * @param world        - world where the island is
     * @param targetPlayer - target player UUID
     * @return string with the number required or blank if the player is unknown
     */
    public String getPointsToNextString(@NonNull World world, @Nullable UUID targetPlayer) {
        if (targetPlayer == null)
            return "";
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        return island == null ? "" : String.valueOf(getLevelsData(island).getPointsToNextLevel());
    }

    /**
     * Get the weighted top ten for this world. Weighting is based on number of
     * players per team.
     * 
     * @param world - world requested
     * @param size  - size of the top ten
     * @return sorted top ten map. The key is the island unique ID
     */
    @NonNull
    public Map<Island, Long> getWeightedTopTen(@NonNull World world, int size) {
        createAndCleanRankings(world);
        Map<Island, Long> weightedTopTen = topTenLists.get(world).getTopTen().entrySet().stream()
                .map(en -> addon.getIslands().getIslandById(en.getKey()).map(island -> {

                    long value = (long) (en.getValue() / (double) Math.max(1, island.getMemberSet().size())); // Calculate
                    // weighted
                    // value
                    return new AbstractMap.SimpleEntry<>(island, value);
                }).orElse(null)) // Handle islands that do not exist according to this ID - old deleted ones
                .filter(Objects::nonNull) // Filter out null entries
                .filter(en -> en.getValue() > 0) // Filter out entries with non-positive values
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())) // Sort in descending order of values
                .limit(size) // Limit to the top 'size' entries
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, // In case of key
                        // collision, choose
                        // the first one
                        LinkedHashMap::new // Preserves the order of entries
                ));

        // Return the unmodifiable map
        return Collections.unmodifiableMap(weightedTopTen);
    }

    /**
     * Get the top ten for this world. Returns offline players or players with the
     * intopten permission.
     * 
     * @param world - world requested
     * @param size  - size of the top ten
     * @return sorted top ten map. The key is the island unique ID
     */
    @NonNull
    public Map<String, Long> getTopTen(@NonNull World world, int size) {
        createAndCleanRankings(world);
        CachedData cachedData = cache.get(world);
        Instant now = Instant.now();

        if (cachedData != null && cachedData.getLastUpdated().plusSeconds(1).isAfter(now)) {
            return cachedData.getCachedMap();
        } else {
            Map<String, Long> newTopTen = calculateTopTen(world, size);
            cache.put(world, new CachedData(newTopTen, now));
            return newTopTen;
        }
    }

    private Map<String, Long> calculateTopTen(@NonNull World world, int size) {
        return Collections.unmodifiableMap(topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(l -> l.getValue() > 0).sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(size)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
    }

    void createAndCleanRankings(@NonNull World world) {
        topTenLists.computeIfAbsent(world, TopTenData::new);
        // Remove player from top ten if they are online and do not have the perm
        topTenLists.get(world).getTopTen().keySet().removeIf(u -> addon.getIslands().getIslandById(u)
                .filter(i -> i.getOwner() == null || !hasTopTenPerm(world, i.getOwner())).isPresent());
    }

    /**
     * @return the topTenLists
     */
    public Map<World, TopTenData> getTopTenLists() {
        return topTenLists;
    }

    /**
     * Get the rank of the player in the rankings
     * 
     * @param world - world
     * @param uuid  - player UUID
     * @return rank placing - note - placing of 1 means top ranked
     */
    public int getRank(@NonNull World world, UUID uuid) {
        createAndCleanRankings(world);
        Stream<Entry<String, Long>> stream = topTenLists.get(world).getTopTen().entrySet().stream()
                .filter(l -> l.getValue() > 0).sorted(Collections.reverseOrder(Map.Entry.comparingByValue()));
        // Get player's current island
        Island island = addon.getIslands().getIsland(world, uuid);
        String id = island == null ? null : island.getUniqueId();
        return (int) (stream.takeWhile(x -> !x.getKey().equals(id)).map(Map.Entry::getKey).count() + 1);
    }

    /**
     * Checks if player has the correct top ten perm to have their level saved
     * 
     * @param world
     * @param targetPlayer
     * @return true if player has the perm or the player is offline
     */
    boolean hasTopTenPerm(@NonNull World world, @NonNull UUID targetPlayer) {
        String permPrefix = addon.getPlugin().getIWM().getPermissionPrefix(world);
        return Bukkit.getPlayer(targetPlayer) == null
                || Bukkit.getPlayer(targetPlayer).hasPermission(permPrefix + INTOPTEN);
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
                    // Load islands, but don't cache them
                    addon.getIslands().getIslandById(il.getUniqueId(), false)
                            .ifPresent(i -> this.addToTopTen(i, il.getLevel()));
                }
            });
            topTenLists.keySet().forEach(w -> addon.log("Generated rankings for " + w.getName()));
        });
    }

    /**
     * Removes an island from a world's top ten
     * 
     * @param world - world
     * @param uuid  - the island's uuid
     */
    public void removeEntry(World world, String uuid) {
        if (topTenLists.containsKey(world)) {
            topTenLists.get(world).getTopTen().remove(uuid);
            // Invalidate the cache because of this deletion
            cache.remove(world);
        }
    }

    /**
     * Set an initial island count
     *
     * @param island - the island to set.
     * @param lv     - initial island count
     */
    public void setInitialIslandCount(@NonNull Island island, long lv) {
        levelsCache.computeIfAbsent(island.getUniqueId(), IslandLevels::new).setInitialCount(lv);
        handler.saveObjectAsync(levelsCache.get(island.getUniqueId()));
    }

    /**
     * Add a delta to the island's initial-count handicap. Used by the new-chunk
     * listener to accumulate generator block points (sea floor, nether ceiling,
     * etc.) into the initial count as chunks are generated during normal play.
     * The initial count is subtracted from the live block total in the level
     * calc, so generator blocks do not inflate the level.
     *
     * @param island the island
     * @param delta  the points to add (no-op when zero)
     */
    public void addToInitialCount(@NonNull Island island, long delta) {
        if (delta == 0) {
            return;
        }
        // Use getInitialCount so any legacy initialLevel is migrated first.
        long current = getInitialCount(island);
        IslandLevels data = getLevelsData(island);
        data.setInitialCount(current + delta);
        handler.saveObjectAsync(data);
    }

    // ---- Pending zero-scan tracking ----

    /**
     * Mark that one more lazy-zero snapshot is queued for {@code island}.
     * Paired with {@link #completePendingZero(Island)} when the snapshot has
     * been processed.
     */
    public void addPendingZero(@NonNull Island island) {
        pendingZeros.computeIfAbsent(island.getUniqueId(), k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * Mark that a previously {@link #addPendingZero queued} snapshot has
     * finished. Safe to call from any thread.
     */
    public void completePendingZero(@NonNull Island island) {
        AtomicInteger c = pendingZeros.get(island.getUniqueId());
        if (c != null) {
            c.decrementAndGet();
        }
    }

    /**
     * @return the number of zero-scan snapshots still queued for this island
     */
    public int getPendingZeroCount(@NonNull Island island) {
        AtomicInteger c = pendingZeros.get(island.getUniqueId());
        return c == null ? 0 : Math.max(0, c.get());
    }

    /**
     * Return a future that completes once every queued zero-scan snapshot for
     * {@code island} has been processed (counter reached zero), or after
     * {@code timeoutMs} milliseconds — whichever happens first. The level
     * scan awaits this before computing the final report so the handicap is
     * never out of date with the chunks that have actually generated.
     */
    public CompletableFuture<Void> awaitPendingZeros(@NonNull Island island, long timeoutMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long deadline = System.currentTimeMillis() + timeoutMs;
        pollPendingZeros(island, future, deadline);
        return future;
    }

    private void pollPendingZeros(Island island, CompletableFuture<Void> future, long deadline) {
        if (getPendingZeroCount(island) == 0) {
            future.complete(null);
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            addon.logWarning("Pending zero-scan snapshots did not complete within timeout for island "
                    + island.getUniqueId() + "; level result may be slightly stale.");
            future.complete(null);
            return;
        }
        // Re-check at every 5 ticks (250 ms). Cheap, and the scan's outer
        // timeout (calculation-timeout) provides the upper bound.
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(),
                () -> pollPendingZeros(island, future, deadline), 5L);
    }

    // ---- Zero-scan visited/deferred tracking ----

    /**
     * Pack chunk (x, z) into a single 64-bit key. Negative coordinates are
     * preserved by masking to 32 bits before shifting. Kept in sync with
     * NewChunkListener's identical helper.
     */
    public static long chunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    /**
     * Mark the start of a zero-island scan for {@code island}. Creates the
     * visited-chunks set and the deferred-credits map so concurrent listener
     * processing during the scan can be tracked and folded in after the scan
     * sets the initial-count baseline.
     */
    public void beginZeroScan(@NonNull Island island) {
        String id = island.getUniqueId();
        zeroScanVisitedChunks.put(id, ConcurrentHashMap.newKeySet());
        zeroScanDeferredCredits.put(id, new ConcurrentHashMap<>());
    }

    /**
     * Record that the zero-island scan visited (counted blocks for) a chunk.
     * Called from the scanner on the worker thread.
     */
    public void recordScanVisitedChunk(@NonNull Island island, int chunkX, int chunkZ) {
        Set<Long> set = zeroScanVisitedChunks.get(island.getUniqueId());
        if (set != null) {
            set.add(chunkKey(chunkX, chunkZ));
        }
    }

    /**
     * Try to record a listener credit during an active zero scan. If no
     * scan is active for this island, returns false and the caller should
     * fall back to {@link #addToInitialCount}. If a scan is active, the
     * credit is stored against the chunk key for later processing by
     * {@link #drainZeroScanDeferred}.
     */
    public boolean tryDeferZeroScanCredit(@NonNull Island island, int chunkX, int chunkZ, long value) {
        Map<Long, Long> deferred = zeroScanDeferredCredits.get(island.getUniqueId());
        if (deferred == null) {
            return false;
        }
        deferred.put(chunkKey(chunkX, chunkZ), value);
        return true;
    }

    /**
     * End the active zero scan for {@code island} and return the sum of
     * deferred listener credits for chunks the scan did NOT visit. The
     * caller should add this sum to the initial count immediately after
     * {@link #setInitialIslandCount}, so chunks that the scan skipped
     * (ungenerated at poll time, generated mid-scan) are preserved instead
     * of being wiped by the baseline reset.
     */
    public long drainZeroScanDeferred(@NonNull Island island) {
        String id = island.getUniqueId();
        Set<Long> visited = zeroScanVisitedChunks.remove(id);
        Map<Long, Long> deferred = zeroScanDeferredCredits.remove(id);
        if (deferred == null || deferred.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (Map.Entry<Long, Long> e : deferred.entrySet()) {
            if (visited == null || !visited.contains(e.getKey())) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    /**
     * Set the island level for the owner of the island that targetPlayer is a
     * member
     * 
     * @param world  - world
     * @param lv     - level
     */
    public void setIslandLevel(@NonNull World world, @NonNull UUID targetPlayer, long lv) {
        // Get the island
        Island island = addon.getIslands().getIsland(world, targetPlayer);
        if (island != null) {
            String id = island.getUniqueId();
            IslandLevels il = levelsCache.computeIfAbsent(id, IslandLevels::new);
            il.setLevel(lv);
            handler.saveObjectAsync(levelsCache.get(id));
            // Update TopTen
            addToTopTen(island, levelsCache.get(id).getLevel());
        }
    }

    /**
     * Set the island level for the owner of the island that targetPlayer is a
     * member
     * 
     * @param world - world
     * @param owner - owner of the island
     * @param r     - results of the calculation
     */
    private void setIslandResults(Island island, Results r) {
        if (island == null)
            return;
        IslandLevels ld = levelsCache.computeIfAbsent(island.getUniqueId(), IslandLevels::new);
        ld.setLevel(r.getLevel());
        ld.setUwCount(Maps.asMap(r.getUwCount().elementSet(), elem -> r.getUwCount().count(elem)));
        ld.setMdCount(Maps.asMap(r.getMdCount().elementSet(), elem -> r.getMdCount().count(elem)));
        ld.setPointsToNextLevel(r.getPointsToNextLevel());
        ld.setTotalPoints(r.getTotalPoints());
        levelsCache.put(island.getUniqueId(), ld);
        handler.saveObjectAsync(ld);
        // Update TopTen
        addToTopTen(island, ld.getLevel());
    }

    /**
     * Removes island from cache when it is deleted
     * 
     * @param uniqueId - id of island
     */
    public void deleteIsland(String uniqueId) {
        levelsCache.remove(uniqueId);
        handler.deleteID(uniqueId);
    }

    // ---- Block Donation Methods ----

    /**
     * Record a block donation for an island. Items should already be removed from the player's inventory.
     *
     * @param island   the island receiving the donation
     * @param donorUUID UUID of the donating player
     * @param material the material name being donated
     * @param count    how many blocks
     * @param points   the point value of this donation
     */
    public void donateBlocks(@NonNull Island island, @NonNull UUID donorUUID, @NonNull String material, int count, long points) {
        IslandLevels ld = getLevelsData(island);
        ld.addDonation(donorUUID.toString(), material, count, points);
        handler.saveObjectAsync(ld);
    }

    /**
     * Queue a full level recalculation for the island. Call this after donations
     * so that the level/top-ten update immediately.
     *
     * @param island the island to recalculate
     */
    public void recalculateAfterDonation(@NonNull Island island) {
        UUID owner = island.getOwner();
        if (owner != null) {
            calculateLevel(owner, island);
        }
    }

    /**
     * Get the total donated points for an island.
     *
     * @param island the island
     * @return total donated points
     */
    public long getDonatedPoints(@NonNull Island island) {
        return getLevelsData(island).getDonatedPoints();
    }

    /**
     * Get the donated blocks map for an island.
     *
     * @param island the island
     * @return map of material name to count
     */
    public Map<String, Integer> getDonatedBlocks(@NonNull Island island) {
        return getLevelsData(island).getDonatedBlocks();
    }

}
