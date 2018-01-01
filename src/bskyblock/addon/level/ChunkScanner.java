package bskyblock.addon.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.material.MaterialData;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import bskyblock.addon.level.config.Settings;

import com.google.common.collect.Multisets;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * A class that calculates the level of an island very quickly by copying island
 * chunks to a list and then processing asynchronously.
 * 
 * @author tastybento
 * 
 */
public class ChunkScanner {
    private static final boolean DEBUG = false;
    protected static final boolean LEVEL_LOGGING = false;
    private final Level addon;
    private final Set<ChunkSnapshot> finalChunk;
    private final Results result;
    private final Optional<User> asker;


    public ChunkScanner(Level plugin, Island island) {
        this.addon = plugin;
        // Get the chunks to scan
        finalChunk = getIslandChunks(island);
        this.asker = Optional.empty();
        // Create new level result
        result = new Results();
        runAsyncCount(island);
    }

    /**
     * Calculates the level of an island
     * @param addon
     * @param island - island that is being calculated
     * @param asker - the user who wants the report
     */
    public ChunkScanner(Level addon, Island island, User asker) {
        this.addon = addon;
        // Get the chunks to scan
        finalChunk = getIslandChunks(island);
        this.asker = Optional.of(asker);
        // Create new level result
        result = new Results();
        runAsyncCount(island);
    }

    private void runAsyncCount(Island island) {
        // Run AsyncTask to count blocks in the chunk snapshots
        addon.getServer().getScheduler().runTaskAsynchronously(addon.getBSkyBlock(), new Runnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                // Copy the limits hashmap
                HashMap<MaterialData, Integer> limitCount = new HashMap<MaterialData, Integer>(Settings.blockLimits);
                // Calculate the island score
                for (ChunkSnapshot chunk: finalChunk) {
                    for (int x = 0; x< 16; x++) { 
                        // Check if the block coord is inside the protection zone and if not, don't count it
                        if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + (island.getProtectionRange() * 2)) {
                            if (DEBUG)
                                addon.getLogger().info("Block is outside protected area - x = " + (chunk.getX() * 16 + x));
                            continue;
                        }
                        for (int z = 0; z < 16; z++) {
                            // Check if the block coord is inside the protection zone and if not, don't count it
                            if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + (island.getProtectionRange() * 2)) {
                                if (DEBUG)
                                    addon.getLogger().info("Block is outside protected area - z = " + (chunk.getZ() * 16 + z));
                                continue;
                            }

                            for (int y = 0; y < island.getWorld().getMaxHeight(); y++) {
                                Material type = chunk.getBlockType(x, y, z);
                                // Currently, there is no alternative to using block data (Dec 2017)
                                MaterialData md = new MaterialData(type, (byte) chunk.getBlockData(x, y, z));                                    
                                MaterialData generic = new MaterialData(type);                                    
                                if (!type.equals(Material.AIR)) { // AIR
                                    if (DEBUG)
                                        addon.getLogger().info("Block is inside protected area " + (chunk.getX() * 16) + "," + (chunk.getZ() * 16 + z));
                                    if (DEBUG)
                                        addon.getLogger().info("Block is " + md + "[" + generic +"]");
                                    if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
                                        int count = limitCount.get(md);
                                        if (DEBUG)
                                            addon.getLogger().info("DEBUG: Count for non-generic " + md + " is " + count);
                                        if (count > 0) {
                                            limitCount.put(md, --count);
                                            if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                result.underWaterBlockCount += Settings.blockValues.get(md);                                                    
                                                result.uwCount.add(md);
                                            } else {
                                                result.rawBlockCount += Settings.blockValues.get(md);
                                                result.mdCount.add(md); 
                                            }
                                        } else {
                                            result.ofCount.add(md);
                                        }
                                    } else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
                                        int count = limitCount.get(generic);
                                        if (DEBUG)
                                            addon.getLogger().info("DEBUG: Count for generic " + generic + " is " + count);
                                        if (count > 0) {  
                                            limitCount.put(generic, --count);
                                            if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                                result.underWaterBlockCount += Settings.blockValues.get(generic);
                                                result.uwCount.add(md);
                                            } else {
                                                result.rawBlockCount += Settings.blockValues.get(generic);
                                                result.mdCount.add(md); 
                                            }
                                        } else {
                                            result.ofCount.add(md);
                                        }
                                    } else if (Settings.blockValues.containsKey(md)) {
                                        if (DEBUG)
                                            addon.getLogger().info("DEBUG: Adding " + md + " = " + Settings.blockValues.get(md));
                                        if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                            result.underWaterBlockCount += Settings.blockValues.get(md);
                                            result.uwCount.add(md);
                                        } else {
                                            result.rawBlockCount += Settings.blockValues.get(md);
                                            result.mdCount.add(md); 
                                        }
                                    } else if (Settings.blockValues.containsKey(generic)) {
                                        if (DEBUG)
                                            addon.getLogger().info("DEBUG: Adding " + generic + " = " + Settings.blockValues.get(generic));
                                        if (Settings.seaHeight > 0 && y<=Settings.seaHeight) {
                                            result.underWaterBlockCount += Settings.blockValues.get(generic);
                                            result.uwCount.add(md);
                                        } else {
                                            result.rawBlockCount += Settings.blockValues.get(generic);
                                            result.mdCount.add(md); 
                                        }
                                    } else {
                                        result.ncCount.add(md);
                                    }
                                }
                            }
                        }
                    }
                }

                result.rawBlockCount += (long)((double)result.underWaterBlockCount * Settings.underWaterMultiplier);
                if (DEBUG)
                    addon.getLogger().info("DEBUG: block count = "+result.rawBlockCount);
                // Set the death penalty
                result.deathHandicap = BSkyBlock.getInstance().getPlayers().getDeaths(island.getOwner()) * Settings.deathpenalty;
                // Set final score
                result.score = (result.rawBlockCount / Settings.levelCost) - result.deathHandicap;

                // Return to main thread
                addon.getServer().getScheduler().runTask(addon.getBSkyBlock(), new Runnable() {

                    @Override
                    public void run() {
                        // Run any modifications

                        // All done.
                        if (asker.isPresent()) {
                            // Tell the asker the result
                            if (asker.get().isPlayer() && asker.get().isOnline()) {
                                asker.get().sendLegacyMessage("Your level is " + result.score);
                            } else {
                                // Console
                                sendConsoleReport(asker);
                            }
                        }
                    }

                    private void sendConsoleReport(Optional<User> asker) {
                        List<String> reportLines = new ArrayList<>();
                        // provide counts
                        reportLines.add("Level Log for island at " + island.getCenter());
                        reportLines.add("Island owner UUID = " + island.getOwner());
                        reportLines.add("Total block value count = " + String.format("%,d",result.rawBlockCount));
                        reportLines.add("Level cost = " + Settings.levelCost);
                        //reportLines.add("Level multiplier = " + levelMultiplier + " (Player must be online to get a permission multiplier)");
                        //reportLines.add("Schematic level handicap = " + levelHandicap + " (level is reduced by this amount)");
                        reportLines.add("Deaths handicap = " + result.deathHandicap);
                        reportLines.add("Level calculated = " + result.score);
                        reportLines.add("==================================");
                        int total = 0;
                        if (!result.uwCount.isEmpty()) {
                            reportLines.add("Underwater block count (Multiplier = x" + Settings.underWaterMultiplier + ") value");
                            reportLines.add("Total number of underwater blocks = " + String.format("%,d",result.uwCount.size()));
                            Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                                    Multisets.copyHighestCountFirst(result.uwCount).entrySet();
                            Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
                            while (it.hasNext()) {
                                Entry<MaterialData> en = it.next();
                                MaterialData type = en.getElement();

                                int value = 0;
                                if (Settings.blockValues.containsKey(type)) {
                                    // Specific
                                    value = Settings.blockValues.get(type);
                                } else if (Settings.blockValues.containsKey(new MaterialData(type.getItemType()))) {
                                    // Generic
                                    value = Settings.blockValues.get(new MaterialData(type.getItemType()));
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
                        //Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                        //        Multisets.copyHighestCountFirst(mdCount).entrySet();
                        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = 
                                result.mdCount.entrySet();
                        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
                        while (it.hasNext()) {
                            Entry<MaterialData> en = it.next();
                            MaterialData type = en.getElement();
                            int value = 0;
                            if (Settings.blockValues.containsKey(type)) {
                                // Specific
                                value = Settings.blockValues.get(type);
                            } else if (Settings.blockValues.containsKey(new MaterialData(type.getItemType()))) {
                                // Generic
                                value = Settings.blockValues.get(new MaterialData(type.getItemType()));
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
                            Integer limit = Settings.blockLimits.get(type.getElement());
                            String explain = ")";
                            if (limit == null) {
                                MaterialData generic = new MaterialData(type.getElement().getItemType());
                                limit = Settings.blockLimits.get(generic);
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
                            asker.get().sendLegacyMessage(line);
                        }
                    }

                });

            }

        });


    }

    private Set<ChunkSnapshot> getIslandChunks(Island island) {
        // Check if player's island world is the nether or overworld and adjust accordingly
        final World world = island.getWorld();
        // Get the chunks
        if (DEBUG)
            addon.getLogger().info("DEBUG: Getting chunks. Protection range = " + island.getProtectionRange());
        //long nano = System.nanoTime();
        Set<ChunkSnapshot> chunkSnapshot = new HashSet<ChunkSnapshot>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + (island.getProtectionRange() *2) + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + (island.getProtectionRange() * 2) + 16); z += 16) {
                if (!world.isChunkLoaded((int)((double)x/16), (int)((double)z/16))) {
                    // If the chunk isn't already generated, load it but don't try and generate it
                    world.loadChunk((int)((double)x/16), (int)((double)z/16), false);
                }
                // chunk is loaded
                chunkSnapshot.add(world.getBlockAt(x, 0, z).getChunk().getChunkSnapshot());

                if (DEBUG)
                    addon.getLogger().info("DEBUG: getting chunk at " + x + ", " + z);
            }
        }
        if (DEBUG)
            addon.getLogger().info("DEBUG: size of chunk snapshot = " + chunkSnapshot.size());
        return chunkSnapshot;
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
        long rawBlockCount;
        Island island;
        long underWaterBlockCount = 0;
        long score;
        int deathHandicap;
    }
}
