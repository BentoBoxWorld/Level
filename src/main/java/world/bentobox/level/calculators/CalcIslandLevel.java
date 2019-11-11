package world.bentobox.level.calculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;


public class CalcIslandLevel {

    private static final String LINE_BREAK = "==================================";

    private final Level addon;

    private final Set<Pair<Integer, Integer>> chunksToScan;
    private final Island island;
    private final Results result;
    private final Runnable onExit;

    // Copy the limits hash map
    private final HashMap<Material, Integer> limitCount;
    private final World world;
    private final List<World> worlds;

    private int count;


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
        this.worlds = new ArrayList<>();
        this.worlds.add(world);
        if (addon.getSettings().isNether()) {
            World netherWorld = addon.getPlugin().getIWM().getNetherWorld(world);
            if (netherWorld != null) this.worlds.add(netherWorld);
        }
        if (addon.getSettings().isEnd()) {
            World endWorld = addon.getPlugin().getIWM().getEndWorld(world);
            if (endWorld != null) this.worlds.add(endWorld);
        }
        this.limitCount = new HashMap<>(addon.getSettings().getBlockLimits());
        this.onExit = onExit;

        // Results go here
        result = new Results();

        // Set the initial island handicap
        result.initialLevel = addon.getInitialIslandLevel(island);

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);
        count = 0;
        chunksToScan.forEach(c -> {
            PaperLib.getChunkAtAsync(world, c.x, c.z).thenAccept(ch -> {
                ChunkSnapshot snapShot = ch.getChunkSnapshot();
                Bukkit.getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
                    this.scanChunk(snapShot);
                    count++;
                    if (count == chunksToScan.size()) {
                        this.tidyUp();
                    }
                });
            });
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
            result.underWaterBlockCount += count;
            result.uwCount.add(bd.getMaterial());
        } else {
            result.rawBlockCount += count;
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
        result.rawBlockCount += (long)(result.underWaterBlockCount * addon.getSettings().getUnderWaterMultiplier());

        // Set the death penalty
        if (this.addon.getSettings().isSumTeamDeaths())
        {
            for (UUID uuid : this.island.getMemberSet())
            {
                this.result.deathHandicap += this.addon.getPlayers().getDeaths(this.world, uuid);
            }
        }
        else
        {
            // At this point, it may be that the island has become unowned.
            this.result.deathHandicap = this.island.getOwner() == null ? 0 :
                this.addon.getPlayers().getDeaths(this.world, this.island.getOwner());
        }

        long blockAndDeathPoints = this.result.rawBlockCount;

        if (this.addon.getSettings().getDeathPenalty() > 0)
        {
            // Proper death penalty calculation.
            blockAndDeathPoints -= this.result.deathHandicap * this.addon.getSettings().getDeathPenalty();
        }

        this.result.level = blockAndDeathPoints / this.addon.getSettings().getLevelCost() - this.island.getLevelHandicap() - result.initialLevel;


        // Calculate how many points are required to get to the next level
        this.result.pointsToNextLevel = this.addon.getSettings().getLevelCost() -
                (blockAndDeathPoints % this.addon.getSettings().getLevelCost());

        // Report
        result.report = getReport();
        // All done.
        if (onExit != null) {
            Bukkit.getScheduler().runTask(addon.getPlugin(), onExit);
        }
    }


    private List<String> getReport() {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island in " + addon.getPlugin().getIWM().getFriendlyName(island.getWorld()) + " at " + Util.xyz(island.getCenter().toVector()));
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d",result.rawBlockCount));
        reportLines.add("Level cost = " + addon.getSettings().getLevelCost());
        reportLines.add("Deaths handicap = " + result.deathHandicap);
        reportLines.add("Initial island level = " + (0L - result.initialLevel));
        reportLines.add("Level calculated = " + result.level);
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
        private long rawBlockCount = 0;
        private long underWaterBlockCount = 0;
        private long level = 0;
        private int deathHandicap = 0;
        private long pointsToNextLevel = 0;
        private long initialLevel = 0;

        /**
         * @return the deathHandicap
         */
        public int getDeathHandicap() {
            return deathHandicap;
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
        public void setLevel(int level) {
            this.level = level;
        }
        /**
         * @return the level
         */
        public long getLevel() {
            return level;
        }
        /**
         * @return the pointsToNextLevel
         */
        public long getPointsToNextLevel() {
            return pointsToNextLevel;
        }

        public long getInitialLevel() {
            return initialLevel;
        }

        public void setInitialLevel(long initialLevel) {
            this.initialLevel = initialLevel;
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
}
