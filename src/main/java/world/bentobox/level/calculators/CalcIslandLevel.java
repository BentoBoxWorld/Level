package world.bentobox.level.calculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;


public class CalcIslandLevel {

    private static final String LINE_BREAK = "==================================";

    public  static final long MAX_AMOUNT = 10000;

    private final Level addon;

    private final Set<Pair<Integer, Integer>> chunksToScan;
    private final Island island;
    private final Results result;
    private final Runnable onExit;

    // Copy the limits hash map
    private final HashMap<Material, Integer> limitCount;
    private final List<World> worlds;
    private final World world;

    private AtomicInteger count;

    private int total;
    private Queue<Chunk> q;
    private int queueid;

    /**
     * Calculate the island's level
     * Results are available in {@link CalcIslandLevel.Results}
     * @param addon - Level addon
     * @param island - island to be calculated
     * @param onExit - what to run when done
     */
    public CalcIslandLevel(final Level addon, final Island island, final Runnable onExit) {
        this.addon = addon;
        this.island = island;
        this.world = island.getWorld();
        this.limitCount = new HashMap<>(addon.getSettings().getBlockLimits());
        this.onExit = onExit;
        this.worlds = new ArrayList<>();
        this.worlds.add(world);
        q = new LinkedList<>();

        // Results go here
        result = new Results();

        // Set the initial island handicap
        result.setInitialLevel(addon.getInitialIslandLevel(island));

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);
        count = new AtomicInteger();
        // Total number of chunks to scan
        total = chunksToScan.size();
        // Add nether world scanning
        if (addon.getSettings().isNether()) {
            World netherWorld = addon.getPlugin().getIWM().getNetherWorld(world);
            if (netherWorld != null) {
                this.worlds.add(netherWorld);
                total += chunksToScan.size();
            }
        }
        // Add End world scanning
        if (addon.getSettings().isEnd()) {
            World endWorld = addon.getPlugin().getIWM().getEndWorld(world);
            if (endWorld != null) {
                this.worlds.add(endWorld);
                total += chunksToScan.size();
            }
        }
        queueid = Bukkit.getScheduler().scheduleSyncRepeatingTask(addon.getPlugin(), () -> {
            for (int i = 0; i < addon.getSettings().getChunks(); i++) {
                if (q.size() == 0) {
                    return;
                }
                Chunk c = q.remove();
                getChunk(c);
            }
        }, addon.getSettings().getTickDelay(), addon.getSettings().getTickDelay());
        chunksToScan.forEach(c -> worlds.forEach(w -> Util.getChunkAtAsync(w, c.x, c.z).thenAccept(this::addChunkQueue)));

    }

    private void addChunkQueue(Chunk ch) {
        q.add(ch);
    }

    private void getChunk(Chunk ch) {
        ChunkSnapshot snapShot = ch.getChunkSnapshot();

        Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            this.scanChunk(snapShot);
            count.getAndIncrement();
            if (count.get() == total) {
                Bukkit.getScheduler().cancelTask(queueid);
                this.tidyUp();
            }
        });
    }

    private void scanChunk(ChunkSnapshot chunk) {
        World chunkWorld = Bukkit.getWorld(chunk.getWorldName());
        if (chunkWorld == null) return;
        int maxHeight = chunkWorld.getMaxHeight();

        for (int x = 0; x< 16; x++) {
            // Check if the block coordinate is inside the protection zone and if not, don't count it
            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionRange() * 2) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coordinate is inside the protection zone and if not, don't count it
                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionRange() * 2) {
                    continue;
                }

                for (int y = 0; y < maxHeight; y++) {
                    BlockData blockData = chunk.getBlockData(x, y, z);
                    int seaHeight = addon.getPlugin().getIWM().getSeaHeight(chunkWorld);
                    boolean belowSeaLevel = seaHeight > 0 && y <= seaHeight;
                    // Slabs can be doubled, so check them twice
                    if (Tag.SLABS.isTagged(blockData.getMaterial())) {
                        Slab slab = (Slab)blockData;
                        if (slab.getType().equals(Slab.Type.DOUBLE)) {
                            checkBlock(blockData, belowSeaLevel);
                        }
                    }
                    checkBlock(blockData, belowSeaLevel);
                }
            }
        }
    }

    private void checkBlock(BlockData bd, boolean belowSeaLevel) {
        int count = limitCount(bd.getMaterial());
        if (belowSeaLevel) {
            result.underWaterBlockCount.addAndGet(count);
            result.uwCount.add(bd.getMaterial());
        } else {
            result.rawBlockCount.addAndGet(count);
            result.mdCount.add(bd.getMaterial());
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
                result.ofCount.add(md);
                return 0;
            }
        }
        return getValue(md);
    }

    /**
     * Get value of a material
     * World blocks trump regular block values
     * @param md - Material to check
     * @return value of a material
     */
    private int getValue(Material md) {
        // Check world settings
        if (addon.getSettings().getWorldBlockValues().containsKey(world) && addon.getSettings().getWorldBlockValues().get(world).containsKey(md)) {
            return addon.getSettings().getWorldBlockValues().get(world).get(md);
        }
        // Check baseline
        if (addon.getSettings().getBlockValues().containsKey(md)) {
            return addon.getSettings().getBlockValues().get(md);
        }
        // Not in config
        result.ncCount.add(md);
        return 0;
    }

    /**
     * Get a set of all the chunks in island
     * @param island - island
     * @return - set of pairs of x,z coordinates to check
     */
    private Set<Pair<Integer, Integer>> getChunksToScan(Island island) {
        Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionRange() * 2 + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionRange() * 2 + 16); z += 16) {
                chunkSnapshot.add(new Pair<>(x >> 4, z >> 4));
            }
        }
        return chunkSnapshot;
    }

    private void tidyUp() {
        // Finalize calculations
        result.rawBlockCount.addAndGet((long)(result.underWaterBlockCount.get() * addon.getSettings().getUnderWaterMultiplier()));

        // Set the death penalty
        if (this.addon.getSettings().isSumTeamDeaths())
        {
            for (UUID uuid : this.island.getMemberSet())
            {
                this.result.deathHandicap.addAndGet(this.addon.getPlayers().getDeaths(this.world, uuid));
            }
        }
        else
        {
            // At this point, it may be that the island has become unowned.
            this.result.deathHandicap.set(this.island.getOwner() == null ? 0 :
                this.addon.getPlayers().getDeaths(this.world, this.island.getOwner()));
        }

        long blockAndDeathPoints = this.result.rawBlockCount.get();

        if (this.addon.getSettings().getDeathPenalty() > 0)
        {
            // Proper death penalty calculation.
            blockAndDeathPoints -= this.result.deathHandicap.get() * this.addon.getSettings().getDeathPenalty();
        }
        this.result.level.set(calculateLevel(blockAndDeathPoints));

        // Calculate how many points are required to get to the next level
        long nextLevel = this.result.level.get();
        long blocks = blockAndDeathPoints;
        while (nextLevel < this.result.level.get() + 1 && blocks - blockAndDeathPoints < MAX_AMOUNT) {
            nextLevel = calculateLevel(++blocks);
        }
        this.result.pointsToNextLevel.set(blocks - blockAndDeathPoints);

        // Report
        result.report = getReport();
        // All done.
        if (onExit != null) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), onExit);
        }
    }


    private long calculateLevel(long blockAndDeathPoints) {
        String calcString = addon.getSettings().getLevelCalc();
        String withValues = calcString.replace("blocks", String.valueOf(blockAndDeathPoints)).replace("level_cost", String.valueOf(this.addon.getSettings().getLevelCost()));
        return (long)eval(withValues) - this.island.getLevelHandicap() - result.initialLevel.get();
    }

    private List<String> getReport() {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island in " + addon.getPlugin().getIWM().getFriendlyName(island.getWorld()) + " at " + Util.xyz(island.getCenter().toVector()));
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d",result.rawBlockCount.get()));
        reportLines.add("Formula to calculate island level: " + addon.getSettings().getLevelCalc());
        reportLines.add("Level cost = " + addon.getSettings().getLevelCost());
        reportLines.add("Deaths handicap = " + result.deathHandicap.get());
        reportLines.add("Initial island level = " + (0L - result.initialLevel.get()));
        reportLines.add("Level calculated = " + result.level.get());
        reportLines.add(LINE_BREAK);
        int total = 0;
        if (!result.uwCount.isEmpty()) {
            reportLines.add("Underwater block count (Multiplier = x" + addon.getSettings().getUnderWaterMultiplier() + ") value");
            reportLines.add("Total number of underwater blocks = " + String.format("%,d",result.uwCount.size()));
            reportLines.addAll(sortedReport(total, result.uwCount));
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d",result.mdCount.size()));
        reportLines.addAll(sortedReport(total, result.mdCount));

        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",result.ofCount.size()));
        Iterable<Multiset.Entry<Material>> entriesSortedByCount = result.ofCount.entrySet();
        Iterator<Entry<Material>> it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<Material> type = it.next();
            Integer limit = addon.getSettings().getBlockLimits().get(type.getElement());
            String explain = ")";
            if (limit == null) {
                Material generic = type.getElement();
                limit = addon.getSettings().getBlockLimits().get(generic);
                explain = " - All types)";
            }
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks (max " + limit + explain);
        }
        reportLines.add(LINE_BREAK);
        reportLines.add("Blocks on island that are not in config.yml");
        reportLines.add("Total number = " + String.format("%,d",result.ncCount.size()));
        entriesSortedByCount = result.ncCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<Material> type = it.next();
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
        }
        reportLines.add(LINE_BREAK);

        return reportLines;
    }

    private Collection<String> sortedReport(int total, Multiset<Material> MaterialCount) {
        Collection<String> r = new ArrayList<>();
        Iterable<Multiset.Entry<Material>> entriesSortedByCount = Multisets.copyHighestCountFirst(MaterialCount).entrySet();
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
     * @return the result
     */
    public Results getResult() {
        return result;
    }

    /**
     * Results class
     *
     */
    public class Results {
        private List<String> report;
        private final Multiset<Material> mdCount = HashMultiset.create();
        private final Multiset<Material> uwCount = HashMultiset.create();
        private final Multiset<Material> ncCount = HashMultiset.create();
        private final Multiset<Material> ofCount = HashMultiset.create();
        // AtomicLong and AtomicInteger must be used because they are changed by multiple concurrent threads
        private AtomicLong rawBlockCount = new AtomicLong(0);
        private AtomicLong underWaterBlockCount = new AtomicLong(0);
        private AtomicLong level = new AtomicLong(0);
        private AtomicInteger deathHandicap = new AtomicInteger(0);
        private AtomicLong pointsToNextLevel = new AtomicLong(0);
        private AtomicLong initialLevel = new AtomicLong(0);

        /**
         * @return the deathHandicap
         */
        public int getDeathHandicap() {
            return deathHandicap.get();
        }

        /**
         * @return the report
         */
        public List<String> getReport() {
            return report;
        }
        /**
         * Set level
         * @param level - level
         */
        public void setLevel(long level) {
            this.level.set(level);
        }
        /**
         * @return the level
         */
        public long getLevel() {
            return level.get();
        }
        /**
         * @return the pointsToNextLevel
         */
        public long getPointsToNextLevel() {
            return pointsToNextLevel.get();
        }

        public long getInitialLevel() {
            return initialLevel.get();
        }

        public void setInitialLevel(long initialLevel) {
            this.initialLevel.set(initialLevel);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Results [report=" + report + ", mdCount=" + mdCount + ", uwCount=" + uwCount + ", ncCount="
                    + ncCount + ", ofCount=" + ofCount + ", rawBlockCount=" + rawBlockCount + ", underWaterBlockCount="
                    + underWaterBlockCount + ", level=" + level + ", deathHandicap=" + deathHandicap
                    + ", pointsToNextLevel=" + pointsToNextLevel + ", initialLevel=" + initialLevel + "]";
        }

    }

    private static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
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

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
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
        }.parse();
    }
}
