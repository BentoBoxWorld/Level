package bentobox.addon.level.calculators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import bentobox.addon.level.Level;

import com.google.common.collect.Multisets;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;


public class CalcIslandLevel {

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private boolean checking;
    private final BukkitTask task;

    private final Level addon;

    private final Set<Pair<Integer, Integer>> chunksToScan;
    private final Island island;
    private final World world;
    private final Results result;
    private final Runnable onExit;

    // Copy the limits hash map
    private final HashMap<Material, Integer> limitCount;


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
        this.world = island.getCenter().getWorld();
        this.limitCount = new HashMap<>(addon.getSettings().getBlockLimits());
        this.onExit = onExit;

        // Results go here
        result = new Results();

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);

        // Start checking
        checking = true;

        // Start a recurring task until done or cancelled
        task = addon.getServer().getScheduler().runTaskTimer(addon.getPlugin(), () -> {
            Set<ChunkSnapshot> chunkSnapshot = new HashSet<>();
            if (checking) {
                Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
                if (!it.hasNext()) {
                    // Nothing left
                    tidyUp();
                    return;
                }
                // Add chunk snapshots to the list
                while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                    Pair<Integer, Integer> pair = it.next();
                    if (!world.isChunkLoaded(pair.x, pair.z)) {
                        world.loadChunk(pair.x, pair.z);
                        chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                        world.unloadChunk(pair.x, pair.z);
                    } else {
                        chunkSnapshot.add(world.getChunkAt(pair.x, pair.z).getChunkSnapshot());
                    }
                    it.remove();
                }
                // Move to next step
                checking = false;
                checkChunksAsync(chunkSnapshot);
            }
        }, 0L, SPEED);
    }

    private void checkChunksAsync(final Set<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        addon.getServer().getScheduler().runTaskAsynchronously(addon.getPlugin(), () -> {
            for (ChunkSnapshot chunk: chunkSnapshot) {
                scanChunk(chunk);
            }
            // Nothing happened, change state
            checking = true;
        });

    }

    private void scanChunk(ChunkSnapshot chunk) {
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

                for (int y = 0; y < island.getCenter().getWorld().getMaxHeight(); y++) {
                    Material blockData = chunk.getBlockType(x, y, z);
                    boolean belowSeaLevel = addon.getSettings().getSeaHeight() > 0 && y <= addon.getSettings().getSeaHeight();
                    // Air is free
                    if (!blockData.equals(Material.AIR)) {
                        checkBlock(blockData, belowSeaLevel);
                    }
                }
            }
        }
    }

    private void checkBlock(Material md, boolean belowSeaLevel) {
        int count = limitCount(md);
        if (belowSeaLevel) {
            result.underWaterBlockCount += count;
            result.uwCount.add(md);
        } else {
            result.rawBlockCount += count;
            result.mdCount.add(md);
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
        } else if (addon.getSettings().getBlockValues().containsKey(md)) {
            return getValue(md);
        } else {
            result.ncCount.add(md);
            return 0;
        }
    }

    /**
     * Get value of a material
     * World blocks trump regular block values
     * @param md Material
     * @return value of a material
     */
    private int getValue(Material md) {
        if (addon.getSettings().getWorldBlockValues().containsKey(world) && addon.getSettings().getWorldBlockValues().get(world).containsKey(md)) {
            return addon.getSettings().getWorldBlockValues().get(world).get(md);
        }
        return addon.getSettings().getBlockValues().getOrDefault(md, 0);
    }

    /**
     * Get a set of all the chunks in island
     * @param island - island
     * @return - set of all the chunks in the island to scan
     */
    private Set<Pair<Integer, Integer>> getChunksToScan(Island island) {
        Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionRange() * 2 + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionRange() * 2 + 16); z += 16) {
                Pair<Integer, Integer> pair = new Pair<>(world.getBlockAt(x, 0, z).getChunk().getX(), world.getBlockAt(x, 0, z).getChunk().getZ());
                chunkSnapshot.add(pair);
            }
        }
        return chunkSnapshot;
    }

    private void tidyUp() {
        // Cancel
        task.cancel();
        // Finalize calculations
        result.rawBlockCount += (long)(result.underWaterBlockCount * addon.getSettings().getUnderWaterMultiplier());
        // Set the death penalty
        result.deathHandicap = addon.getPlayers().getDeaths(world, island.getOwner());
        // Set final score
        result.level = (result.rawBlockCount / addon.getSettings().getLevelCost()) - result.deathHandicap - island.getLevelHandicap();
        // Calculate how many points are required to get to the next level
        result.pointsToNextLevel = addon.getSettings().getLevelCost() - (result.rawBlockCount % addon.getSettings().getLevelCost());
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
        reportLines.add("Level calculated = " + result.level);
        reportLines.add("==================================");
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
        reportLines.add("==================================");
        reportLines.add("Blocks on island that are not in config.yml");
        reportLines.add("Total number = " + String.format("%,d",result.ncCount.size()));
        entriesSortedByCount = result.ncCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<Material> type = it.next();
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
        }
        reportLines.add("=================================");

        return reportLines;
    }

    private Collection<String> sortedReport(int total, Multiset<Material> MaterialCount) {
        Collection<String> result = new ArrayList<>();
        Iterable<Multiset.Entry<Material>> entriesSortedByCount = Multisets.copyHighestCountFirst(MaterialCount).entrySet();
        for (Entry<Material> en : entriesSortedByCount) {
            Material type = en.getElement();

            int value = 0;
            if (addon.getSettings().getBlockValues().containsKey(type)) {
                // Specific
                value = addon.getSettings().getBlockValues().get(type);
            }
            result.add(type.toString() + ":"
                    + String.format("%,d", en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
            total += (value * en.getCount());
        }
        result.add("Subtotal = " + total);
        result.add("==================================");
        return result;
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

    }
}
