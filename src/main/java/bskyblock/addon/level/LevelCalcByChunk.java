package bskyblock.addon.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

import bskyblock.addon.level.event.IslandPostLevelEvent;
import bskyblock.addon.level.event.IslandPreLevelEvent;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Pair;


public class LevelCalcByChunk {

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private boolean checking = true;
    private BukkitTask task;

    private Level addon;

    private Set<Pair<Integer, Integer>> chunksToScan;
    private Island island;
    private World world;
    private User asker;
    private UUID targetPlayer;
    private Results result;

    // Copy the limits hashmap
    HashMap<MaterialData, Integer> limitCount;
    private boolean report;
    private long oldLevel;


    public LevelCalcByChunk(final Level addon, final Island island, final UUID targetPlayer, final User asker, final boolean report) {
        this.addon = addon;
        this.island = island;
        this.world = island != null ? island.getCenter().getWorld() : null;
        this.asker = asker;
        this.targetPlayer = targetPlayer;
        this.limitCount = new HashMap<>(addon.getSettings().getBlockLimits());
        this.report = report;
        this.oldLevel = addon.getIslandLevel(targetPlayer);

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
            if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionRange()) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                // Check if the block coord is inside the protection zone and if not, don't count it
                if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionRange()) {
                    continue;
                }

                for (int y = 0; y < island.getCenter().getWorld().getMaxHeight(); y++) {
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
        Set<Pair<Integer, Integer>> chunkSnapshot = new HashSet<>();
        for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionRange() + 16); x += 16) {
            for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionRange() + 16); z += 16) {
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
        result.rawBlockCount += (long)((double)result.underWaterBlockCount * addon.getSettings().getUnderWaterMultiplier());
        // Set the death penalty
        result.deathHandicap = addon.getPlayers().getDeaths(island.getOwner());
        // Set final score
        result.score = (result.rawBlockCount / addon.getSettings().getLevelCost()) - result.deathHandicap - island.getLevelHandicap();
        // Run any modifications
        // Get the permission multiplier if it is available
        int levelMultiplier = 1;
        Player player = addon.getServer().getPlayer(targetPlayer);
        if (player != null) {
            // Get permission multiplier                
            for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
                if (perms.getPermission().startsWith(Constants.PERMPREFIX + "island.multiplier.")) {
                    String spl[] = perms.getPermission().split(Constants.PERMPREFIX + "island.multiplier.");
                    if (spl.length > 1) {
                        if (!NumberUtils.isDigits(spl[1])) {
                            addon.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                        } else {
                            // Get the max value should there be more than one
                            levelMultiplier = Math.max(levelMultiplier, Integer.valueOf(spl[1]));
                        }
                    }
                }
                // Do some sanity checking
                if (levelMultiplier < 1) {
                    levelMultiplier = 1;
                }
            }
        }
        // Calculate how many points are required to get to the next level
        long pointsToNextLevel = (addon.getSettings().getLevelCost() * (result.score + 1 + island.getLevelHandicap())) - ((result.rawBlockCount * levelMultiplier) - (result.deathHandicap * addon.getSettings().getDeathPenalty()));
        // Sometimes it will return 0, so calculate again to make sure it will display a good value
        if(pointsToNextLevel == 0) pointsToNextLevel = (addon.getSettings().getLevelCost() * (result.score + 2 + island.getLevelHandicap()) - ((result.rawBlockCount * levelMultiplier) - (result.deathHandicap * addon.getSettings().getDeathPenalty())));

        // All done.
        informPlayers(saveLevel(island, targetPlayer, pointsToNextLevel));

    }

    private void informPlayers(IslandPreLevelEvent event) {
        // Fire the island post level calculation event
        final IslandPostLevelEvent event3 = new IslandPostLevelEvent(targetPlayer, island, event.getLevel(), event.getPointsToNextLevel());
        addon.getServer().getPluginManager().callEvent(event3);

        if(event3.isCancelled() || asker == null) {
            return;
        }
        // Tell the asker
        asker.sendMessage("island.level.island-level-is", "[level]", String.valueOf(addon.getIslandLevel(targetPlayer)));
        // Console  
        if (report) {
            sendConsoleReport(asker);
        }
        // Check if player - if so show some more info
        if (!(asker instanceof Player)) {
            return;
        }
        // Player
        if (addon.getSettings().getDeathPenalty() != 0) {
            asker.sendMessage("island.level.deaths", "[number]", String.valueOf(result.deathHandicap));
        }
        // Send player how many points are required to reach next island level
        if (event.getPointsToNextLevel() >= 0) {
            asker.sendMessage("island.level.required-points-to-next-level", "[points]", String.valueOf(event.getPointsToNextLevel()));
        }
        // Tell other team members
        if (addon.getIslandLevel(targetPlayer) != oldLevel) {
            for (UUID member : island.getMemberSet()) {
                if (!member.equals(asker.getUniqueId())) {
                    User.getInstance(member).sendMessage("island.level.island-level-is", "[level]", String.valueOf(addon.getIslandLevel(targetPlayer)));
                }
            }
        }
    }

    private IslandPreLevelEvent saveLevel(Island island, UUID targetPlayer, long pointsToNextLevel) {
        // Fire the pre-level event
        final IslandPreLevelEvent event = new IslandPreLevelEvent(targetPlayer, island, result.score);
        event.setPointsToNextLevel(pointsToNextLevel);
        addon.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            // Save the value
            addon.setIslandLevel(island.getOwner(), event.getLevel());
            if (addon.getPlayers().inTeam(targetPlayer)) {
                //plugin.getLogger().info("DEBUG: player is in team");
                for (UUID member : addon.getIslands().getMembers(targetPlayer)) {
                    //plugin.getLogger().info("DEBUG: updating team member level too");
                    if (addon.getIslandLevel(member) != event.getLevel()) {
                        addon.setIslandLevel(member, event.getLevel());
                    }
                }
                if (addon.getPlayers().inTeam(targetPlayer)) {
                    UUID leader = addon.getIslands().getTeamLeader(targetPlayer);
                    if (leader != null) {
                        addon.getTopTen().addEntry(leader, event.getLevel());
                    }
                } else {
                    addon.getTopTen().addEntry(targetPlayer, event.getLevel());
                }
            }
        }
        return event;
    }

    private void sendConsoleReport(User asker) {
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
            reportLines.addAll(sortedReport(total, result.uwCount));
        }
        reportLines.add("Regular block count");
        reportLines.add("Total number of blocks = " + String.format("%,d",result.mdCount.size()));
        reportLines.addAll(sortedReport(total, result.mdCount));

        reportLines.add("Blocks not counted because they exceeded limits: " + String.format("%,d",result.ofCount.size()));
        //entriesSortedByCount = Multisets.copyHighestCountFirst(ofCount).entrySet();
        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = result.ofCount.entrySet();
        Iterator<Entry<MaterialData>> it = entriesSortedByCount.iterator();
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

    private Collection<String> sortedReport(int total, Multiset<MaterialData> materialDataCount) {
        Collection<String> result = new ArrayList<>();
        Iterable<Multiset.Entry<MaterialData>> entriesSortedByCount = Multisets.copyHighestCountFirst(materialDataCount).entrySet();
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
                result.add(type.toString() + ":" 
                        + String.format("%,d",en.getCount()) + " blocks x " + value + " = " + (value * en.getCount()));
                total += (value * en.getCount());
            }
        }
        result.add("Subtotal = " + total);
        result.add("==================================");
        return result;
    }

    /**
     * Results class
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
