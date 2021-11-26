package world.bentobox.level.calculators;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;
import world.bentobox.level.calculators.Results.Result;

/**
 * A pipeliner that will process one island at a time
 * @author tastybento
 *
 */
public class Pipeliner {

    private static final int START_DURATION = 10; // 10 seconds
    private final Queue<IslandLevelCalculator> toProcessQueue;
    private final Map<IslandLevelCalculator, Long> inProcessQueue;
    private final BukkitTask task;
    private final Level addon;
    private long time;
    private long count;

    /**
     * Construct the pipeliner
     */
    public Pipeliner(Level addon) {
        this.addon = addon;
        toProcessQueue = new ConcurrentLinkedQueue<>();
        inProcessQueue = new HashMap<>();
        // Loop continuously - check every tick if there is an island to scan
        task = Bukkit.getScheduler().runTaskTimer(BentoBox.getInstance(), () -> {
            if (!BentoBox.getInstance().isEnabled()) {
                cancel();
                return;
            }
            // Complete the current to Process queue first
            if (!inProcessQueue.isEmpty() || toProcessQueue.isEmpty()) return;
            for (int j = 0; j < addon.getSettings().getConcurrentIslandCalcs() && !toProcessQueue.isEmpty(); j++) {
                IslandLevelCalculator iD = toProcessQueue.poll();
                // Ignore deleted or unonwed islands
                if (!iD.getIsland().isDeleted() && !iD.getIsland().isUnowned()) {
                    inProcessQueue.put(iD, System.currentTimeMillis());
                    // Start the scanning of a island with the first chunk
                    scanIsland(iD);
                }
            }
        }, 1L, 10L);
    }

    private void cancel() {
        task.cancel();
    }

    /**
     * @return number of islands currently in the queue or in process
     */
    public int getIslandsInQueue() {
        return inProcessQueue.size() + toProcessQueue.size();
    }

    /**
     * Scans one chunk of an island and adds the results to a results object
     * @param iD
     */
    private void scanIsland(IslandLevelCalculator iD) {
        if (iD.getIsland().isDeleted() || iD.getIsland().isUnowned() || task.isCancelled()) {
            // Island is deleted, so finish early with nothing
            inProcessQueue.remove(iD);
            iD.getR().complete(null);
            return;
        }
        iD.scanIsland(this);
    }


    /**
     * Adds an island to the scanning queue but only if the island is not already in the queue
     * @param island  - the island to scan
     * @return CompletableFuture of the results. Results will be null if the island is already in the queue
     */
    public CompletableFuture<Results> addIsland(Island island) {
        // Check if queue already contains island and it's not an island zero calculation
        if (inProcessQueue.keySet().parallelStream().filter(IslandLevelCalculator::isNotZeroIsland)
                .map(IslandLevelCalculator::getIsland).anyMatch(island::equals)
                || toProcessQueue.parallelStream().filter(IslandLevelCalculator::isNotZeroIsland)
                .map(IslandLevelCalculator::getIsland).anyMatch(island::equals)) {
            return CompletableFuture.completedFuture(new Results(Result.IN_PROGRESS));
        }
        return addToQueue(island, false);
    }

    /**
     * Adds an island to the scanning queue
     * @param island  - the island to scan
     * @return CompletableFuture of the results
     */
    public CompletableFuture<Results> zeroIsland(Island island) {
        return addToQueue(island, true);
    }

    private CompletableFuture<Results> addToQueue(Island island, boolean zeroing) {
        CompletableFuture<Results> r = new CompletableFuture<>();
        toProcessQueue.add(new IslandLevelCalculator(addon, island, r, zeroing));
        count++;
        return r;
    }

    /**
     * Get the average time it takes to run a level check
     * @return the average time in seconds
     */
    public int getTime() {
        return time == 0 || count == 0 ? START_DURATION : (int)((double)time/count/1000);
    }

    /**
     * Submit how long a level check took
     * @param time the time to set
     */
    public void setTime(long time) {
        // Running average
        this.time += time;
    }

    /**
     * Stop the current queue.
     */
    public void stop() {
        addon.log("Stopping Level queue");
        task.cancel();
        this.inProcessQueue.clear();
        this.toProcessQueue.clear();
    }

    /**
     * @return the inProcessQueue
     */
    protected Map<IslandLevelCalculator, Long> getInProcessQueue() {
        return inProcessQueue;
    }

    /**
     * @return the task
     */
    protected BukkitTask getTask() {
        return task;
    }




}
