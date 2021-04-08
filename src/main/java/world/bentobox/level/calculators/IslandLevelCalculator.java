package world.bentobox.level.calculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.gui.page.ChestPage;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class IslandLevelCalculator {
    private static final String LINE_BREAK = "==================================";
    public static final long MAX_AMOUNT = 10000;

    /**
     * Method to evaluate a mathematical equation
     * @param str - equation to evaluate
     * @return value of equation
     */
    private static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    switch (func) {
                    case "sqrt":
                        x = Math.sqrt(x);
                        break;
                    case "sin":
                        x = Math.sin(Math.toRadians(x));
                        break;
                    case "cos":
                        x = Math.cos(Math.toRadians(x));
                        break;
                    case "tan":
                        x = Math.tan(Math.toRadians(x));
                        break;
                    default:
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }
        }.parse();
    }
    private final Level addon;
    private final Queue<Pair<Integer, Integer>> chunksToCheck;
    private final Island island;
    private final HashMap<Material, Integer> limitCount;
    private final CompletableFuture<Results> r;


    private final Results results;
    private long duration;
    private final boolean zeroIsland;

    /**
     * Constructor to get the level for an island
     * @param addon - Level addon
     * @param island - the island to scan
     * @param r - completable result that will be completed when the calculation is complete
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
        this.limitCount = new HashMap<>(addon.getBlockConfig().getBlockLimits());
        // Get the initial island level
        results.initialLevel.set(addon.getInitialIslandLevel(island));
    }

    /**
     * Calculate the level based on the raw points
     * @param blockAndDeathPoints - raw points counted on island
     * @return level of island
     */
    private long calculateLevel(long blockAndDeathPoints) {
        String calcString = addon.getSettings().getLevelCalc();
        String withValues = calcString.replace("blocks", String.valueOf(blockAndDeathPoints)).replace("level_cost", String.valueOf(this.addon.getSettings().getLevelCost()));
        return (long)eval(withValues) - this.island.getLevelHandicap() - (addon.getSettings().isZeroNewIslandLevels() ? results.initialLevel.get() : 0);
    }

    /**
     * Adds value to the results based on the material and whether the block is below sea level or not
     * @param mat - material of the block
     * @param belowSeaLevel - true if below sea level
     */
    private void checkBlock(Material mat, boolean belowSeaLevel) {
        int count = limitCount(mat);
        if (belowSeaLevel) {
            results.underWaterBlockCount.addAndGet(count);
            results.uwCount.add(mat);
        } else {
            results.rawBlockCount.addAndGet(count);
            results.mdCount.add(mat);
        }
    }

    /**
     * Get a set of all the chunks in island
     * @param island - island
     * @return - set of pairs of x,z coordinates to check
     */
    private Queue<Pair<Integer, Integer>> getChunksToScan(Island island) {
        Queue<Pair<Integer, Integer>> chunkQueue = new ConcurrentLinkedQueue<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionRange() * 2 + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionRange() * 2 + 16); z += 16) {
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
     * @return the r
     */
    public CompletableFuture<Results> getR() {
        return r;
    }

    /**
     * Get the full analysis report
     * @return a list of lines
     */
    private List<String> getReport() {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island in " + addon.getPlugin().getIWM().getFriendlyName(island.getWorld()) + " at " + Util.xyz(island.getCenter().toVector()));
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d",results.rawBlockCount.get()));
        reportLines.add("Formula to calculate island level: " + addon.getSettings().getLevelCalc());
        reportLines.add("Level cost = " + addon.getSettings().getLevelCost());
        reportLines.add("Deaths handicap = " + results.deathHandicap.get());
        if (addon.getSettings().isZeroNewIslandLevels()) {
            reportLines.add("Initial island level = " + (0L - addon.getManager().getInitialLevel(island)));
        }
        reportLines.add("Previous level = " + addon.getManager().getIslandLevel(island.getWorld(), island.getOwner()));
        reportLines.add("New level = " + results.getLevel());
        reportLines.add(LINE_BREAK);
        int total = 0;
        if (!results.uwCount.isEmpty()) {
            reportLines.add("Underwater block count (Multiplier = x" + addon.getSettings().getUnderWaterMultiplier() + ") value");
            reportLines.add("Total number of underwater blocks = " + String.format("%,d",results.uwCount.size()));
            reportLines.addAll(sortedReport(total, results.uwCount));
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d",results.mdCount.size()));
        reportLines.addAll(sortedReport(total, results.mdCount));

        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",results.ofCount.size()));
        Iterable<Multiset.Entry<Material>> entriesSortedByCount = results.ofCount.entrySet();
        Iterator<Entry<Material>> it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<Material> type = it.next();
            Integer limit = addon.getBlockConfig().getBlockLimits().get(type.getElement());
            String explain = ")";
            if (limit == null) {
                Material generic = type.getElement();
                limit = addon.getBlockConfig().getBlockLimits().get(generic);
                explain = " - All types)";
            }
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks (max " + limit + explain);
        }
        reportLines.add(LINE_BREAK);
        reportLines.add("Blocks on island that are not in config.yml");
        reportLines.add("Total number = " + String.format("%,d",results.ncCount.size()));
        entriesSortedByCount = results.ncCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<Material> type = it.next();
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
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
     * Get value of a material
     * World blocks trump regular block values
     * @param md - Material to check
     * @return value of a material
     */
    private int getValue(Material md) {
        Integer value = addon.getBlockConfig().getValue(island.getWorld(), md);
        if (value == null) {
            // Not in config
            results.ncCount.add(md);
            return 0;
        }
        return value;
    }

    /**
     * Get a chunk async
     * @param world - the world where the chunk is
     * @param env - the environment
     * @param x - chunk x coordinate
     * @param z - chunk z coordinate
     * @return a future chunk or future null if there is no chunk to load, e.g., there is no island nether
     */
    private CompletableFuture<Chunk> getWorldChunk(@NonNull World world, Environment env, int x, int z) {
        switch (env) {
        case NETHER:
            if (addon.getSettings().isNether()) {
                World nether = addon.getPlugin().getIWM().getNetherWorld(island.getWorld());
                if (nether != null) {
                    return Util.getChunkAtAsync(nether, x, z, true);
                }
            }
            // There is no chunk to scan, so return a null chunk
            return CompletableFuture.completedFuture(null);
        case THE_END:
            if (addon.getSettings().isEnd()) {
                World end = addon.getPlugin().getIWM().getEndWorld(island.getWorld());
                if (end != null) {
                    return Util.getChunkAtAsync(end, x, z, true);
                }
            }
            // There is no chunk to scan, so return a null chunk
            return CompletableFuture.completedFuture(null);
        default:
            return Util.getChunkAtAsync(world, x, z, true);

        }
    }

    /**
     * Checks if a block has been limited or not and whether a block has any value or not
     * @param md Material
     * @return value of the block if can be counted
     */
    private int limitCount(Material md) {
        if (limitCount.containsKey(md)) {
            int count = limitCount.get(md);
            if (count > 0) {
                limitCount.put(md, --count);
                return getValue(md);
            } else {
                results.ofCount.add(md);
                return 0;
            }
        }
        return getValue(md);
    }


    /**
     * Count the blocks on the island
     * @param result - the CompletableFuture that should be completed when this scan is done
     * @param chunkSnapshot - the chunk to scan
     */
    private void scanAsync(CompletableFuture<Boolean> result, ChunkSnapshot chunkSnapshot, Chunk chunk) {
        int seaHeight = addon.getPlugin().getIWM().getSeaHeight(island.getWorld());
        List<Vector> stackedBlocks = new ArrayList<>();
        for (int x = 0; x< 16; x++) {
            // Check if the block coordinate is inside the protection zone and if not, don't count it
            if (chunkSnapshot.getX() * 16 + x < island.getMinProtectedX() || chunkSnapshot.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionRange() * 2) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coordinate is inside the protection zone and if not, don't count it
                if (chunkSnapshot.getZ() * 16 + z < island.getMinProtectedZ() || chunkSnapshot.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionRange() * 2) {
                    continue;
                }
                // Only count to the highest block in the world for some optimization
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    boolean belowSeaLevel = seaHeight > 0 && y <= seaHeight;
                    // Slabs can be doubled, so check them twice
                    if (Tag.SLABS.isTagged(blockData.getMaterial())) {
                        Slab slab = (Slab)blockData;
                        if (slab.getType().equals(Slab.Type.DOUBLE)) {
                            checkBlock(blockData.getMaterial(), belowSeaLevel);
                        }
                    }
                    // Hook for Wild Stackers (Blocks Only) - this has to use the real chunk
                    if (addon.isStackersEnabled() && blockData.getMaterial() == Material.CAULDRON) {
                        stackedBlocks.add(new Vector(x,y,z));
                    }
                    // Add the value of the block's material
                    checkBlock(blockData.getMaterial(), belowSeaLevel);
                }
            }
        }
        // Complete the future - this must go back onto the primary thread to exit async otherwise subsequent actions will be async
        Bukkit.getScheduler().runTask(addon.getPlugin(),() -> {
            // Deal with any stacked blocks
            stackedBlocks.forEach(v -> {
                Block cauldronBlock = chunk.getBlock(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                boolean belowSeaLevel = seaHeight > 0 && v.getBlockY() <= seaHeight;
                if (WildStackerAPI.getWildStacker().getSystemManager().isStackedBarrel(cauldronBlock)) {
                    StackedBarrel barrel = WildStackerAPI.getStackedBarrel(cauldronBlock);
                    int barrelAmt = WildStackerAPI.getBarrelAmount(cauldronBlock);
                    for (int _x = 0; _x < barrelAmt; _x++) {
                        checkBlock(barrel.getType(), belowSeaLevel);
                    }
                }
            });
            result.complete(true);
        });
    }

    /**
     * Scan all containers in a chunk and count their blocks
     * @param chunk - the chunk to scan
     */
    private void scanChests(Chunk chunk) {
        // Count blocks in chests
        for (BlockState bs : chunk.getTileEntities()) {
            if (bs instanceof Container) {
                if (addon.isAdvChestEnabled()) {
                    AdvancedChest aChest = AdvancedChestsAPI.getChestManager().getAdvancedChest(bs.getLocation());
                    if (aChest != null) {
                        aChest.getPages().stream().map(ChestPage::getItems).forEach(c -> {
                            for (ItemStack i : c) {
                                countItemStack(i);
                            }
                        });
                        continue;
                    }
                }
                // Regular chest
                ((Container)bs).getSnapshotInventory().forEach(this::countItemStack);
            }
        }
    }

    private void countItemStack(ItemStack i) {
        if (i != null && i.getType().isBlock()) {
            for (int c = 0; c < i.getAmount(); c++) {
                checkBlock(i.getType(), false);
            }
        }
    }

    /**
     * Scan the chunk chests and count the blocks
     * @param chunk - the chunk to scan
     * @return future that completes when the scan is done and supplies a boolean that will be true if the scan was successful, false if not
     */
    private CompletableFuture<Boolean> scanChunk(@Nullable Chunk chunk) {
        // If the chunk hasn't been generated, return
        if (chunk == null) return CompletableFuture.completedFuture(false);
        // Scan chests
        if (addon.getSettings().isIncludeChests()) {
            scanChests(chunk);
        }
        // Count blocks in chunk
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(), () -> scanAsync(result, snapshot, chunk));
        return result;
    }

    /**
     * Scan the next chunk on the island
     * @return completable boolean future that will be true if more chunks are left to be scanned, and false if not
     */
    public CompletableFuture<Boolean> scanNextChunk() {
        if (chunksToCheck.isEmpty()) {
            addon.logError("Unexpected: no chunks to scan!");
            // This should not be needed, but just in case
            return CompletableFuture.completedFuture(false);
        }
        // Retrieve and remove from the queue
        Pair<Integer, Integer> p = chunksToCheck.poll();
        // Set up the result
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // Get chunks and scan
        getWorldChunk(island.getWorld(), Environment.THE_END, p.x, p.z).thenAccept(endChunk ->
        scanChunk(endChunk).thenAccept(b ->
        getWorldChunk(island.getWorld(), Environment.NETHER, p.x, p.z).thenAccept(netherChunk ->
        scanChunk(netherChunk).thenAccept(b2 ->
        getWorldChunk(island.getWorld(), Environment.NORMAL, p.x, p.z).thenAccept(normalChunk ->
        scanChunk(normalChunk).thenAccept(b3 ->
        // Complete the result now that all chunks have been scanned
        result.complete(!chunksToCheck.isEmpty()))))
                )
                )
                );

        return result;
    }

    private Collection<String> sortedReport(int total, Multiset<Material> materialCount) {
        Collection<String> r = new ArrayList<>();
        Iterable<Multiset.Entry<Material>> entriesSortedByCount = Multisets.copyHighestCountFirst(materialCount).entrySet();
        for (Entry<Material> en : entriesSortedByCount) {
            Material type = en.getElement();

            int value = getValue(type);

            r.add(type.toString() + ":"
                    + String.format("%,d", en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
            total += (value * en.getCount());

        }
        r.add("Subtotal = " + total);
        r.add(LINE_BREAK);
        return r;
    }


    /**
     * Finalizes the calculations and makes the report
     */
    public void tidyUp() {
        // Finalize calculations
        results.rawBlockCount.addAndGet((long)(results.underWaterBlockCount.get() * addon.getSettings().getUnderWaterMultiplier()));

        // Set the death penalty
        if (this.addon.getSettings().isSumTeamDeaths())
        {
            for (UUID uuid : this.island.getMemberSet())
            {
                this.results.deathHandicap.addAndGet(this.addon.getPlayers().getDeaths(island.getWorld(), uuid));
            }
        }
        else
        {
            // At this point, it may be that the island has become unowned.
            this.results.deathHandicap.set(this.island.getOwner() == null ? 0 :
                this.addon.getPlayers().getDeaths(island.getWorld(), this.island.getOwner()));
        }

        long blockAndDeathPoints = this.results.rawBlockCount.get();

        if (this.addon.getSettings().getDeathPenalty() > 0)
        {
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
}
