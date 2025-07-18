package world.bentobox.level.calculators;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.gui.page.ChestPage;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.Results.Result;
import world.bentobox.level.config.BlockConfig;

public class IslandLevelCalculator {
    private final UUID calcId = UUID.randomUUID();  // ID for hashing
    private static final String LINE_BREAK = "==================================";
    public static final long MAX_AMOUNT = 10000000;
    private static final int CHUNKS_TO_SCAN = 100;
    private final Level addon;
    private final Queue<Pair<Integer, Integer>> chunksToCheck;
    private final Island island;
    private final Map<Object, Integer> limitCount;
    private final CompletableFuture<Results> r;

    private final Results results;
    private long duration;
    private final boolean zeroIsland;
    private final Map<Environment, World> worlds = new EnumMap<>(Environment.class);
    private final int seaHeight;
    private final List<Location> stackedBlocks = new ArrayList<>();
    private final Set<Chunk> chestBlocks = new HashSet<>();
    private final Map<Location, Boolean> spawners = new HashMap<>();

    /**
     * Constructor to get the level for an island
     * 
     * @param addon      - Level addon
     * @param island     - the island to scan
     * @param r          - completable result that will be completed when the
     *                   calculation is complete
     * @param zeroIsland - true if the calculation is due to an island zeroing
     */
    public IslandLevelCalculator(Level addon, Island island, CompletableFuture<Results> r, boolean zeroIsland) {
        this.addon = addon;
        this.island = island;
        this.r = r;
        this.zeroIsland = zeroIsland;
        results = new Results();
        duration = System.currentTimeMillis();
        chunksToCheck = getChunksToScan(island);
        this.limitCount = new HashMap<>();
        // Get the initial island level
        // TODO: results.initialLevel.set(addon.getInitialIslandLevel(island));
        results.setInitialCount(addon.getInitialIslandCount(island));
        // Set up the worlds
        worlds.put(Environment.NORMAL, Util.getWorld(island.getWorld()));
        // Nether
        if (addon.getSettings().isNether()) {
            World nether = addon.getPlugin().getIWM().getNetherWorld(island.getWorld());
            if (nether != null) {
                worlds.put(Environment.NETHER, nether);
            }
        }
        // End
        if (addon.getSettings().isEnd()) {
            World end = addon.getPlugin().getIWM().getEndWorld(island.getWorld());
            if (end != null) {
                worlds.put(Environment.THE_END, end);
            }
        }
        // Sea Height
        seaHeight = addon.getPlugin().getIWM().getSeaHeight(island.getWorld());
    }

    /**
     * Calculate the level based on the raw points
     * 
     * @param rawPoints - raw points counted on island
     * @return level of island
     */
    private long calculateLevel(final long rawPoints) {
        String calcString = addon.getSettings().getLevelCalc();
        // Reduce count by initial count, if zeroing is done
        long modifiedPoints = rawPoints
                - (addon.getSettings().isZeroNewIslandLevels() ? results.initialCount.get() : 0);
        // Paste in the values to the formula
        String withValues = calcString.replace("blocks", String.valueOf(modifiedPoints)).replace("level_cost",
                String.valueOf(this.addon.getSettings().getLevelCost()));
        // Try and evaluate it
        try {
            return (long) EquationEvaluator.eval(withValues);
        } catch (ParseException e) {
            // Hmm, error.
            addon.getPlugin().logStacktrace(e);
            return 0L;
        }
    }

    /**
     * Adds value to the results based on the namespacedId and whether the block is
     * below sea level or not
     * 
     * @param namespacedId   - namespacedId of the block
     * @param belowSeaLevel - true if below sea level
     */
    private void checkBlock(String namespacedId, boolean belowSeaLevel) {
        int count = limitCountAndValue(namespacedId);
        if (belowSeaLevel) {
            results.underWaterBlockCount.addAndGet(count);
            results.uwCount.add(namespacedId);
        } else {
            results.rawBlockCount.addAndGet(count);
            results.mdCount.add(namespacedId);
        }
    }

    /**
     * Adds value to the results based on the material and whether the block is
     * below sea level or not
     * 
     * @param mat           - material of the block
     * @param belowSeaLevel - true if below sea level
     */
    private void checkBlock(Material mat, boolean belowSeaLevel) {
        int count = limitCountAndValue(mat);
        if (belowSeaLevel) {
            results.underWaterBlockCount.addAndGet(count);
            results.uwCount.add(mat);
        } else {
            results.rawBlockCount.addAndGet(count);
            results.mdCount.add(mat);
        }
    }

    /**
     * Adds value to the results based on the material and whether the block is
     * below sea level or not
     * 
     * @param mat           - material of the block
     * @param belowSeaLevel - true if below sea level
     */
    private void checkSpawner(EntityType et, boolean belowSeaLevel) {
        Integer count = limitCountAndValue(et);
        if (count != null) {
            if (belowSeaLevel) {
                results.underWaterBlockCount.addAndGet(count);
                results.uwCount.add(et);
            } else {
                results.rawBlockCount.addAndGet(count);
                results.mdCount.add(et);
            }
        }
    }

    /**
     * Get a set of all the chunks in island
     * 
     * @param island - island
     * @return - set of pairs of x,z coordinates to check
     */
    private Queue<Pair<Integer, Integer>> getChunksToScan(Island island) {
        Queue<Pair<Integer, Integer>> chunkQueue = new ConcurrentLinkedQueue<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionRange() * 2
                + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionRange() * 2
                    + 16); z += 16) {
                chunkQueue.add(new Pair<>(x >> 4, z >> 4));
            }
        }
        return chunkQueue;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Get the completable result for this calculation
     * 
     * @return the r
     */
    public CompletableFuture<Results> getR() {
        return r;
    }

    /**
     * Get the full analysis report
     * 
     * @return a list of lines
     */
    private List<String> getReport() {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island in " + addon.getPlugin().getIWM().getFriendlyName(island.getWorld())
                + " at " + Util.xyz(island.getCenter().toVector()));
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d", results.rawBlockCount.get()));
        reportLines.add("Formula to calculate island level: " + addon.getSettings().getLevelCalc());
        reportLines.add("Level cost = " + addon.getSettings().getLevelCost());
        reportLines.add("Deaths handicap = " + results.deathHandicap.get());
        /*
        if (addon.getSettings().isZeroNewIslandLevels()) {
            reportLines.add("Initial island level = " + (0L - addon.getManager().getInitialLevel(island)));
        }*/
        if (addon.getSettings().isZeroNewIslandLevels()) {
            reportLines.add("Initial island count = " + (0L - addon.getManager().getInitialCount(island)));
        }
        reportLines.add("Previous level = " + addon.getManager().getIslandLevel(island.getWorld(), island.getOwner()));
        reportLines.add("New level = " + results.getLevel());
        reportLines.add(LINE_BREAK);
        int total = 0;
        if (!results.uwCount.isEmpty()) {
            reportLines.add("Underwater block count (Multiplier = x" + addon.getSettings().getUnderWaterMultiplier()
                    + ") value");
            reportLines.add("Total number of underwater blocks = " + String.format("%,d", results.uwCount.size()));
            reportLines.addAll(sortedReport(total, results.uwCount));
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d", results.mdCount.size()));
        reportLines.addAll(sortedReport(total, results.mdCount));

        reportLines.add(
                "Blocks not counted because they exceeded limits: " + String.format("%,d", results.ofCount.size()));
        Iterable<Multiset.Entry<Object>> entriesSortedByCount = results.ofCount.entrySet();
        Iterator<Entry<Object>> it = entriesSortedByCount.iterator();
        while (it.hasNext()) {

            Entry<Object> type = it.next();
            Integer limit = addon.getBlockConfig().getLimit(type.getElement());
            String explain = ")";
            reportLines.add(Util.prettifyText(type.toString()) + ": " + String.format("%,d", type.getCount())
                    + " blocks (max " + limit + explain);
        }
        reportLines.add(LINE_BREAK);
        return reportLines;
    }

    /**
     * @return the results
     */
    public Results getResults() {
        return results;
    }

    /**
     * Get value of a material World blocks trump regular block values
     * 
     * @param obj - Material, EntityType, or NamespacedId to check
     * @return value
     */
    private int getValue(Object obj) {
        Integer value = addon.getBlockConfig().getValue(island.getWorld(), obj);
        if (value == null) {
            // Not in config
            results.ncCount.add(obj);
            return 0;
        }
        return value;
    }

    /**
     * Get a chunk async
     * 
     * @param env      - the environment
     * @param pairList - chunk coordinate
     * @return a future chunk or future null if there is no chunk to load, e.g.,
     *         there is no island nether
     */
    private CompletableFuture<List<Chunk>> getWorldChunk(Environment env, Queue<Pair<Integer, Integer>> pairList) {
        if (worlds.containsKey(env)) {
            CompletableFuture<List<Chunk>> r2 = new CompletableFuture<>();
            List<Chunk> chunkList = new ArrayList<>();
            World world = worlds.get(env);
            // Get the chunk, and then coincidentally check the RoseStacker
            loadChunks(r2, world, pairList, chunkList);
            return r2;
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private void loadChunks(CompletableFuture<List<Chunk>> r2, World world, Queue<Pair<Integer, Integer>> pairList,
            List<Chunk> chunkList) {
        if (pairList.isEmpty()) {
            r2.complete(chunkList);
            return;
        }
        Pair<Integer, Integer> p = pairList.poll();
        // We need to generate now all the time because some game modes are not voids
        Util.getChunkAtAsync(world, p.x, p.z, true).thenAccept(chunk -> {
            if (chunk != null) {
                chunkList.add(chunk);
                roseStackerCheck(chunk);
            }
            loadChunks(r2, world, pairList, chunkList); // Iteration
        });
    }

    private void roseStackerCheck(Chunk chunk) {
        if (addon.isRoseStackersEnabled()) {
            RoseStackerAPI.getInstance().getStackedBlocks(Collections.singletonList(chunk)).forEach(e -> {
                // Blocks below sea level can be scored differently
                boolean belowSeaLevel = seaHeight > 0 && e.getLocation().getY() <= seaHeight;
                // Check block once because the base block will be counted in the chunk snapshot
                for (int _x = 0; _x < e.getStackSize() - 1; _x++) {
                    checkBlock(e.getBlock().getType(), belowSeaLevel);
                }
            });
        }
    }

    /**
     * Checks if the given object (Material or EntityType) has reached its limit.
     * If it hasn't, the object's value is returned and its count is incremented.
     * If the object is not a Material or EntityType, or if it has exceeded its limit, 0 is returned.
     *
     * @param obj A Material or EntityType
     * @return The object's value if within limit, otherwise 0.
     */
    private int limitCountAndValue(Object obj) {
        // Only process if obj is a Material or EntityType
        if (!(obj instanceof Material) && !(obj instanceof EntityType) && !(obj instanceof String)) {
            return 0;
        }

        Integer limit = addon.getBlockConfig().getLimit(obj);
        if (limit == null) {
            return getValue(obj);
        }

        int count = limitCount.getOrDefault(obj, 0);

        if (count > limit) {
            // Add block to ofCount
            this.results.ofCount.add(obj);
            return 0;
        }
        limitCount.put(obj, count + 1);
        return getValue(obj);
    }

    /**
     * Scan all containers in a chunk and count their blocks
     * 
     * @param chunk - the chunk to scan
     */
    private void scanChests(Chunk chunk) {
        // Count blocks in chests
        for (BlockState bs : chunk.getTileEntities()) {
            if (bs instanceof Container container) {
                if (addon.isAdvChestEnabled()) {
                    AdvancedChest<?, ?> aChest = AdvancedChestsAPI.getChestManager().getAdvancedChest(bs.getLocation());
                    if (aChest != null && aChest.getChestType().getName().equals("NORMAL")) {
                        aChest.getPages().stream().map(ChestPage::getItems).forEach(c -> {
                            for (Object i : c) {
                                countItemStack((ItemStack) i);
                            }
                        });
                        continue;
                    }
                }
                // Regular chest
                container.getSnapshotInventory().forEach(this::countItemStack);
            }
        }
    }

    private void countItemStack(ItemStack i) {
        if (i == null || !i.getType().isBlock())
            return;

        for (int c = 0; c < i.getAmount(); c++) {
            if (addon.getSettings().isIncludeShulkersInChest()
                    && i.getItemMeta() instanceof BlockStateMeta blockStateMeta
                    && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                shulkerBox.getSnapshotInventory().forEach(this::countItemStack);
            }

            checkBlock(i.getType(), false);
        }
    }

    /**
     * Scan the chunk chests and count the blocks. Note that the chunks are a list
     * of all the island chunks in a particular world, so the memory usage is high,
     * but I think most servers can handle it.
     * 
     * @param chunks - a list of chunks to scan
     * @return future that completes when the scan is done and supplies a boolean
     *         that will be true if the scan was successful, false if not
     */
    private CompletableFuture<Boolean> scanChunk(List<Chunk> chunks) {
        // If the chunk hasn't been generated, return
        if (chunks == null || chunks.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        // Count blocks in chunk
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        /*
         * At this point, we need to grab a snapshot of each chunk and then scan it
         * async. At the end, we make the CompletableFuture true to show it is done. I'm
         * not sure how much lag this will cause, but as all the chunks are loaded,
         * maybe not that much.
         */
        List<ChunkPair> preLoad = chunks.stream().map(c -> new ChunkPair(c.getWorld(), c, c.getChunkSnapshot()))
                .toList();
        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(), () -> {
            preLoad.forEach(this::scanAsync);
            // Once they are all done, return to the main thread.
            Bukkit.getScheduler().runTask(addon.getPlugin(), () -> result.complete(true));
        });
        return result;
    }

    record ChunkPair(World world, Chunk chunk, ChunkSnapshot chunkSnapshot) {
    }

    private void scanAsync(ChunkPair cp) {
        int chunkX = cp.chunkSnapshot.getX() * 16;
        int chunkZ = cp.chunkSnapshot.getZ() * 16;
        int minX = island.getMinProtectedX();
        int maxX = minX + island.getProtectionRange() * 2;
        int minZ = island.getMinProtectedZ();
        int maxZ = minZ + island.getProtectionRange() * 2;

        for (int x = 0; x < 16; x++) {
            int globalX = chunkX + x;
            if (globalX >= minX && globalX < maxX) {
                for (int z = 0; z < 16; z++) {
                    int globalZ = chunkZ + z;
                    if (globalZ >= minZ && globalZ < maxZ) {
                        for (int y = cp.world.getMinHeight(); y < cp.world.getMaxHeight(); y++) {
                            processBlock(cp, x, y, z, globalX, globalZ);
                        }
                    }
                }
            }
        }
    }

    private void processBlock(ChunkPair cp, int x, int y, int z, int globalX, int globalZ) {
        BlockData blockData = cp.chunkSnapshot.getBlockData(x, y, z);
        Material m = blockData.getMaterial();
        if (m.isAir()) {
            return;
        }

        boolean belowSeaLevel = seaHeight > 0 && y <= seaHeight;
        Location loc = new Location(cp.world, globalX, y, globalZ);

        String customRegionId = addon.isItemsAdder() ? ItemsAdderHook.getInCustomRegion(loc) : null;
        if (customRegionId != null) {
            checkBlock(customRegionId, belowSeaLevel);
            return;
        }

        processSlabs(blockData, m, belowSeaLevel);
        processStackers(loc, m);
        processUltimateStacker(m, loc, belowSeaLevel);
        processChests(cp, cp.chunkSnapshot.getBlockType(x, y, z));
        processSpawnerOrBlock(m, loc, belowSeaLevel);
    }

    private void processSlabs(BlockData blockData, Material m, boolean belowSeaLevel) {
        if (Tag.SLABS.isTagged(m)) {
            Slab slab = (Slab) blockData;
            if (slab.getType().equals(Slab.Type.DOUBLE)) {
                checkBlock(m, belowSeaLevel);
            }
        }
    }

    private void processStackers(Location loc, Material m) {
        if (addon.isStackersEnabled() && (m.equals(Material.CAULDRON) || m.equals(Material.SPAWNER))) {
            stackedBlocks.add(loc);
        }
    }

    private void processUltimateStacker(Material m, Location loc, boolean belowSeaLevel) {
        if (addon.isUltimateStackerEnabled() && !m.isAir()) {
            UltimateStackerCalc.addStackers(m, loc, results, belowSeaLevel, limitCountAndValue(m));
        }
    }

    private void processChests(ChunkPair cp, Material material) {
        if (addon.getSettings().isIncludeChests()) {
            switch (material) {
            case CHEST:
            case TRAPPED_CHEST:
            case BARREL:
            case HOPPER:
            case DISPENSER:
            case DROPPER:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case BREWING_STAND:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case BEACON: // has an inventory slot
            case ENCHANTING_TABLE: // technically has an item slot
            case LECTERN: // stores a book
            case JUKEBOX: // stores a record
                // ✅ It's a container
                chestBlocks.add(cp.chunk);
                break;
            default:
                // ❌ Not a container
                break;
            }

        }
    }

    private void processSpawnerOrBlock(Material m, Location loc, boolean belowSeaLevel) {
        if (m == Material.SPAWNER) {
            spawners.put(loc, belowSeaLevel);
        } else {
            checkBlock(m, belowSeaLevel);
        }
    }

    /**
     * Scan the next chunk on the island
     * 
     * @return completable boolean future that will be true if more chunks are left
     *         to be scanned, and false if not
     */
    public CompletableFuture<Boolean> scanNextChunk() {
        if (chunksToCheck.isEmpty()) {
            addon.logError("Unexpected: no chunks to scan!");
            // This should not be needed, but just in case
            return CompletableFuture.completedFuture(false);
        }
        // Retrieve and remove from the queue
        Queue<Pair<Integer, Integer>> pairList = new ConcurrentLinkedQueue<>();
        int i = 0;
        while (!chunksToCheck.isEmpty() && i++ < CHUNKS_TO_SCAN) {
            pairList.add(chunksToCheck.poll());
        }
        Queue<Pair<Integer, Integer>> endPairList = new ConcurrentLinkedQueue<>(pairList);
        Queue<Pair<Integer, Integer>> netherPairList = new ConcurrentLinkedQueue<>(pairList);
        // Set up the result
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // Get chunks and scan
        // Get chunks and scan
        getWorldChunk(Environment.THE_END, endPairList).thenAccept(
                endChunks -> scanChunk(endChunks).thenAccept(b -> getWorldChunk(Environment.NETHER, netherPairList)
                        .thenAccept(netherChunks -> scanChunk(netherChunks)
                                .thenAccept(b2 -> getWorldChunk(Environment.NORMAL, pairList)
                                        .thenAccept(normalChunks -> scanChunk(normalChunks).thenAccept(b3 ->
                                        // Complete the result now that all chunks have been scanned
                                        result.complete(!chunksToCheck.isEmpty())))))));

        return result;
    }

    private Collection<String> sortedReport(int total, Multiset<Object> uwCount) {
        Collection<String> result = new ArrayList<>();
        Iterable<Multiset.Entry<Object>> entriesSortedByCount = Multisets.copyHighestCountFirst(uwCount)
                .entrySet();
        for (Entry<Object> en : entriesSortedByCount) {

            int value = 0;
            String name = "";
            if (en.getElement() instanceof Material md) {
                value = Objects.requireNonNullElse(addon.getBlockConfig().getValue(island.getWorld(), md), 0);
                name = Util.prettifyText(md.name());
            } else if (en.getElement() instanceof EntityType et) {
                name = Util.prettifyText(et.name() + BlockConfig.SPAWNER);
                value = Objects.requireNonNullElse(addon.getBlockConfig().getValue(island.getWorld(), et), 0);
            } else if (en.getElement() instanceof String str) {
                name = str;
                value = Objects.requireNonNullElse(addon.getBlockConfig().getValue(island.getWorld(), str), 0);
            }

            result.add(name + ": " + String.format("%,d", en.getCount()) + " blocks x " + value + " = "
                    + (value * en.getCount()));
            total += (value * en.getCount());

        }
        result.add("Subtotal = " + total);
        result.add(LINE_BREAK);
        return result;
    }

    /**
     * Finalizes the calculations and makes the report
     */
    public void tidyUp() {
        // Finalize calculations
        results.rawBlockCount
                .addAndGet((long) (results.underWaterBlockCount.get() * addon.getSettings().getUnderWaterMultiplier()));

        // Set the death penalty
        if (this.addon.getSettings().isSumTeamDeaths()) {
            for (UUID uuid : this.island.getMemberSet()) {
                this.results.deathHandicap.addAndGet(this.addon.getPlayers().getDeaths(island.getWorld(), uuid));
            }
        } else {
            // At this point, it may be that the island has become unowned.
            this.results.deathHandicap.set(this.island.getOwner() == null ? 0
                    : this.addon.getPlayers().getDeaths(island.getWorld(), this.island.getOwner()));
        }

        long blockAndDeathPoints = this.results.rawBlockCount.get();
        this.results.totalPoints.set(blockAndDeathPoints);

        if (this.addon.getSettings().getDeathPenalty() > 0) {
            // Proper death penalty calculation.
            blockAndDeathPoints -= this.results.deathHandicap.get() * this.addon.getSettings().getDeathPenalty();
        }
        this.results.level.set(calculateLevel(blockAndDeathPoints));

        // Calculate how many points are required to get to the next level
        long nextLevel = this.results.level.get();
        long blocks = blockAndDeathPoints;
        while (nextLevel < this.results.level.get() + 1 && blocks - blockAndDeathPoints < MAX_AMOUNT) {
            nextLevel = calculateLevel(++blocks);
        }
        this.results.pointsToNextLevel.set(blocks - blockAndDeathPoints);

        // Report
        results.report = getReport();
        // Set the duration
        addon.getPipeliner().setTime(System.currentTimeMillis() - duration);
        // All done.
    }

    /**
     * @return the zeroIsland
     */
    boolean isNotZeroIsland() {
        return !zeroIsland;
    }

    public void scanIsland(Pipeliner pipeliner) {
        // Scan the next chunk
        scanNextChunk().thenAccept(result -> {
            if (!Bukkit.isPrimaryThread()) {
                addon.getPlugin().logError("scanChunk not on Primary Thread!");
            }
            // Timeout check
            if (System.currentTimeMillis()
                    - pipeliner.getInProcessQueue().get(this) > addon.getSettings().getCalculationTimeout() * 60000) {
                // Done
                pipeliner.getInProcessQueue().remove(this);
                getR().complete(new Results(Result.TIMEOUT));
                addon.logError("Level calculation timed out after " + addon.getSettings().getCalculationTimeout()
                        + "m for island: " + getIsland());
                if (!isNotZeroIsland()) {
                    addon.logError("Island level was being zeroed.");
                }
                return;
            }
            if (Boolean.TRUE.equals(result) && !pipeliner.getTask().isCancelled()) {
                // scanNextChunk returns true if there are more chunks to scan
                scanIsland(pipeliner);
            } else {
                // Done
                pipeliner.getInProcessQueue().remove(this);
                BentoBox.getInstance().log("Completed Level scan.");
                // Chunk finished
                // This was the last chunk. Handle stacked blocks, spawners, chests and exit
                handleStackedBlocks().thenCompose(v -> handleSpawners()).thenCompose(v -> handleChests())
                        .thenRun(() -> {
                    this.tidyUp();
                    this.getR().complete(getResults());
                });
            }
        });
    }

    private CompletableFuture<Void> handleSpawners() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<Location, Boolean> en : this.spawners.entrySet()) {
            CompletableFuture<Void> future = Util.getChunkAtAsync(en.getKey()).thenAccept(c -> {
                if (en.getKey().getBlock().getType() == Material.SPAWNER) {
                    EntityType et = ((CreatureSpawner) en.getKey().getBlock().getState()).getSpawnedType();
                    if (et != null) {
                        checkSpawner(et, en.getValue());
                    } else {
                        // This spawner has no spawning capability. Just list it as a spawner block
                        checkBlock(Material.SPAWNER, en.getValue());
                    }
                }
            });
            futures.add(future);
        }
        // Return a CompletableFuture that completes when all futures are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

    }

    private CompletableFuture<Void> handleChests() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Chunk v : chestBlocks) {
            CompletableFuture<Void> future = Util.getChunkAtAsync(v.getWorld(), v.getX(), v.getZ()).thenAccept(c -> {
                scanChests(c);
            });
            futures.add(future);
        }
        // Return a CompletableFuture that completes when all futures are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> handleStackedBlocks() {
        // Deal with any stacked blocks
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Location v : stackedBlocks) {
            CompletableFuture<Void> future = Util.getChunkAtAsync(v).thenAccept(c -> {
                Block stackedBlock = v.getBlock();
                boolean belowSeaLevel = seaHeight > 0 && v.getBlockY() <= seaHeight;
                if (WildStackerAPI.getWildStacker().getSystemManager().isStackedBarrel(stackedBlock)) {
                    StackedBarrel barrel = WildStackerAPI.getStackedBarrel(stackedBlock);
                    int barrelAmt = WildStackerAPI.getBarrelAmount(stackedBlock);
                    for (int _x = 0; _x < barrelAmt; _x++) {
                        checkBlock(barrel.getType(), belowSeaLevel);
                    }
                } else if (WildStackerAPI.getWildStacker().getSystemManager().isStackedSpawner(stackedBlock)) {
                    int spawnerAmt = WildStackerAPI.getSpawnersAmount((CreatureSpawner) stackedBlock.getState());
                    for (int _x = 0; _x < spawnerAmt; _x++) {
                        checkBlock(stackedBlock.getType(), belowSeaLevel);
                    }
                }
            });
            futures.add(future);
        }
        // Return a CompletableFuture that completes when all futures are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IslandLevelCalculator)) return false;
        IslandLevelCalculator that = (IslandLevelCalculator) o;
        return calcId.equals(that.calcId);
    }

    @Override
    public int hashCode() {
        return calcId.hashCode();
    }
}
