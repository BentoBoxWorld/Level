package bskyblock.addon.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Pair;

public class Scanner {

    private static final int MAX_CHUNKS = 50;
    private static final long SPEED = 5;
    private boolean checking = true;
    private BukkitTask task;

    private Level addon;

    private Set<Pair<Integer, Integer>> chunksToScan;
    private Island island;
    private User asker;
    private Results result;

    // Copy the limits hashmap
    HashMap<MaterialData, Integer> limitCount;


    public Scanner(Level addon, Island island, User asker) {
        this.addon = addon;
        this.island = island;
        this.asker = asker;
        this.limitCount = new HashMap<>(addon.getSettings().getBlockLimits());
        // Results go here
        result = new Results();

        // Get chunks to scan
        chunksToScan = getChunksToScan(island);

        // Start checking
        checking = true;

        // Start a recurring task until done or cancelled
        task = addon.getServer().getScheduler().runTaskTimer(addon.getBSkyBlock(), () -> {
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
                    chunkSnapshot.add(island.getWorld().getChunkAt(pair.x, pair.z).getChunkSnapshot());
                    it.remove();
                }
                // Move to next step
                checking = false;
                checkChunksAsync(chunkSnapshot);
            }
        }, 0L, SPEED);
    }

    private void checkChunksAsync(Set<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        addon.getServer().getScheduler().runTaskAsynchronously(addon.getBSkyBlock(), () -> {

            for (ChunkSnapshot chunk: chunkSnapshot) {
                scanChunk(chunk);
            }
            // Nothing happened, change state
            checking = true;
        });  

    }

    @SuppressWarnings("deprecation")
    private void scanChunk(ChunkSnapshot chunk) {
        for (int x = 0; x< 16; x++) { 
            // Check if the block coord is inside the protection zone and if not, don't count it
            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + (island.getProtectionRange() * 2)) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coord is inside the protection zone and if not, don't count it
                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + (island.getProtectionRange() * 2)) {
                    continue;
                }

                for (int y = 0; y < island.getWorld().getMaxHeight(); y++) {
                    Material blockType = chunk.getBlockType(x, y, z);
                    boolean belowSeaLevel = (addon.getSettings().getSeaHeight() > 0 && y<=addon.getSettings().getSeaHeight()) ? true : false;
                    // Air is free
                    if (!blockType.equals(Material.AIR)) {
                        checkBlock(blockType, chunk.getBlockData(x, y, z), belowSeaLevel);
                    }
                }
            }
        }
    }

    private void checkBlock(Material type, int blockData, boolean belowSeaLevel) {
        // Currently, there is no alternative to using block data (Feb 2018)
        @SuppressWarnings("deprecation")
        MaterialData md = new MaterialData(type, (byte) blockData);
        int count = limitCount(md);
        if (count > 0) {
            if (belowSeaLevel) {
                result.underWaterBlockCount += count;                                                    
                result.uwCount.add(md);
            } else {
                result.rawBlockCount += count;
                result.mdCount.add(md); 
            } 
        }
    }

    /**
     * Checks if a block has been limited or not and whether a block has any value or not
     * @param md
     * @return value of the block if can be counted
     */
    private int limitCount(MaterialData md) {
        MaterialData generic = new MaterialData(md.getItemType());
        if (limitCount.containsKey(md) && addon.getSettings().getBlockValues().containsKey(md)) {
            int count = limitCount.get(md);
            if (count > 0) {
                limitCount.put(md, --count);
                return addon.getSettings().getBlockValues().get(md);
            } else {
                result.ofCount.add(md);
                return 0;
            }
        } else if (limitCount.containsKey(generic) && addon.getSettings().getBlockValues().containsKey(generic)) {
            int count = limitCount.get(generic);
            if (count > 0) {  
                limitCount.put(generic, --count);
                return addon.getSettings().getBlockValues().get(generic);
            } else {
                result.ofCount.add(md);
                return 0;
            }
        } else if (addon.getSettings().getBlockValues().containsKey(md)) {
            return addon.getSettings().getBlockValues().get(md);
        } else if (addon.getSettings().getBlockValues().containsKey(generic)) {
            return addon.getSettings().getBlockValues().get(generic);
        } else {
            result.ncCount.add(md);
            return 0;
        }
    }

    /**
     * Get a set of all the chunks in island
     * @param island
     * @return
     */
    private Set<Pair<Integer, Integer>> getChunksToScan(Island island) {
        // Get the chunks coords
        Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + (island.getProtectionRange() *2) + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + (island.getProtectionRange() * 2) + 16); z += 16) {                
                chunkSnapshot.add(new Pair<>(x/16,z/16));
            }
        }
        return chunkSnapshot;
    }

    private void tidyUp() {
        // Cancel
        task.cancel();
        // Finalize calculations
        result.rawBlockCount += (long)((double)result.underWaterBlockCount * addon.getSettings().getUnderWaterMultiplier());
        // Set the death penalty
        result.deathHandicap = BSkyBlock.getInstance().getPlayers().getDeaths(island.getOwner()) * addon.getSettings().getDeathPenalty();
        // Set final score
        result.score = (result.rawBlockCount / addon.getSettings().getLevelCost()) - result.deathHandicap;
        // Run any modifications
        // Save the value
        addon.setIslandLevel(island.getOwner(), result.score);
        // All done.
        // Tell the asker the result
        if (asker.isPlayer() && asker.isOnline()) {
            asker.sendMessage("island.level.island-level-is", "[level]", String.valueOf(result.score));
        } else {
            // Console
            sendConsoleReport(asker, island);
        }
    }

    private void sendConsoleReport(User asker, Island island) {
        List<String> reportLines = new ArrayList<>();
        // provide counts
        reportLines.add("Level Log for island at " + island.getCenter());
        reportLines.add("Island owner UUID = " + island.getOwner());
        reportLines.add("Total block value count = " + String.format("%,d",result.rawBlockCount));
        reportLines.add("Level cost = " + addon.getSettings().getLevelCost());
        //reportLines.add("Level multiplier = " + levelMultiplier + " (Player must be online to get a permission multiplier)");
        //reportLines.add("Schematic level handicap = " + levelHandicap + " (level is reduced by this amount)");
        reportLines.add("Deaths handicap = " + result.deathHandicap);
        reportLines.add("Level calculated = " + result.score);
        reportLines.add("==================================");
        int total = 0;
        if (!result.uwCount.isEmpty()) {
            reportLines.add("Underwater block count (Multiplier = x" + addon.getSettings().getUnderWaterMultiplier() + ") value");
            reportLines.add("Total number of underwater blocks = " + String.format("%,d",result.uwCount.size()));
            Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                    Multisets.copyHighestCountFirst(result.uwCount).entrySet();
            Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
            while (it.hasNext()) {
                Entry<MaterialData> en = it.next();
                MaterialData type = en.getElement();

                int value = 0;
                if (addon.getSettings().getBlockValues().containsKey(type)) {
                    // Specific
                    value = addon.getSettings().getBlockValues().get(type);
                } else if (addon.getSettings().getBlockValues().containsKey(new MaterialData(type.getItemType()))) {
                    // Generic
                    value = addon.getSettings().getBlockValues().get(new MaterialData(type.getItemType()));
                }
                if (value > 0) {
                    reportLines.add(type.toString() + ":" 
                            + String.format("%,d",en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
                    total += (value * en.getCount());
                }
            }
            reportLines.add("Subtotal = " + total);
            reportLines.add("==================================");
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d",result.mdCount.size()));
        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                result.mdCount.entrySet();
        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<MaterialData> en = it.next();
            MaterialData type = en.getElement();
            int value = 0;
            if (addon.getSettings().getBlockValues().containsKey(type)) {
                // Specific
                value = addon.getSettings().getBlockValues().get(type);
            } else if (addon.getSettings().getBlockValues().containsKey(new MaterialData(type.getItemType()))) {
                // Generic
                value = addon.getSettings().getBlockValues().get(new MaterialData(type.getItemType()));
            }
            if (value > 0) {
                reportLines.add(type.toString() + ":" 
                        + String.format("%,d",en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
                total += (value * en.getCount());
            }
        }
        reportLines.add("Total = " + total);
        reportLines.add("==================================");
        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",result.ofCount.size()));
        //entriesSortedByCount = Multisets.copyHighestCountFirst(ofCount).entrySet();
        entriesSortedByCount = result.ofCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<MaterialData> type = it.next();
            Integer limit = addon.getSettings().getBlockLimits().get(type.getElement());
            String explain = ")";
            if (limit == null) {
                MaterialData generic = new MaterialData(type.getElement().getItemType());
                limit = addon.getSettings().getBlockLimits().get(generic);
                explain = " - All types)";
            }
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks (max " + limit + explain);
        }
        reportLines.add("==================================");
        reportLines.add("Blocks on island that are not in config.yml");
        reportLines.add("Total number = " + String.format("%,d",result.ncCount.size()));
        //entriesSortedByCount = Multisets.copyHighestCountFirst(ncCount).entrySet();
        entriesSortedByCount = result.ncCount.entrySet();
        it = entriesSortedByCount.iterator();
        while (it.hasNext()) {
            Entry<MaterialData> type = it.next();
            reportLines.add(type.getElement().toString() + ": " + String.format("%,d",type.getCount()) + " blocks");
        }                        
        reportLines.add("=================================");

        for (String line : reportLines) {
            asker.sendRawMessage(line);
        }
    }

    /**
     * Results class
     * @author ben
     *
     */
    public class Results {
        Multiset<MaterialData> mdCount = HashMultiset.create();
        Multiset<MaterialData> uwCount = HashMultiset.create();
        Multiset<MaterialData> ncCount = HashMultiset.create();
        Multiset<MaterialData> ofCount = HashMultiset.create();
        long rawBlockCount = 0;
        Island island;
        long underWaterBlockCount = 0;
        long score = 0;
        int deathHandicap = 0;
    }
}
