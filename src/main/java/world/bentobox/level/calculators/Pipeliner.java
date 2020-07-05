package world.bentobox.level.calculators;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.Level;

/**
 * A pipeliner that will process one island at a time
 * @author tastybento
 *
 */
public class Pipeliner {

    private static final int START_DURATION = 10; // 10 seconds
    private final Queue<IslandLevelCalculator> processQueue;
    private final BukkitTask task;
    private boolean inProcess;
    private final Level addon;
    private long time;
    private long count;

    /**
     * Construct the pipeliner
     */
    public Pipeliner(Level addon) {
        this.addon = addon;
        processQueue = new ConcurrentLinkedQueue<>();
        // Loop continuously - check every tick if there is an island to scan
        task = Bukkit.getScheduler().runTaskTimer(BentoBox.getInstance(), () -> {
            if (!BentoBox.getInstance().isEnabled()) {
                cancel();
                return;
            }
            // One island at a time
            if (inProcess || processQueue.isEmpty()) return;

            IslandLevelCalculator iD = processQueue.poll();
            // Ignore deleted or unonwed islands
            if (iD.getIsland().isDeleted() || iD.getIsland().isUnowned()) return;
            // Start the process
            inProcess = true;
            // Start the scanning of a island with the first chunk
            scanChunk(iD);

        }, 1L, 1L);
    }

    private void cancel() {
        task.cancel();
    }

    /**
     * @return number of islands currently in the queue or in process
     */
    public int getIslandsInQueue() {
        return inProcess ? processQueue.size() + 1 : processQueue.size();
    }

    /**
     * Scans one chunk of an island and adds the results to a results object
     * @param iD
     */
    private void scanChunk(IslandLevelCalculator iD) {
        if (iD.getIsland().isDeleted() || iD.getIsland().isUnowned()) {
            // Island is deleted, so finish early with nothing
            addon.log("Canceling island level calculation - island has been deleted, or has become unowned.");
            inProcess = false;
            iD.getR().complete(null);
            return;
        }
        // Scan the next chunk
        iD.scanNextChunk().thenAccept(r -> {
            if (!Bukkit.isPrimaryThread()) {
                addon.getPlugin().logError("scanChunk not on Primary Thread!");
            }
            if (Boolean.TRUE.equals(r)) {
                // scanNextChunk returns true if there are more chunks to scan
                scanChunk(iD);
            } else {
                // Done
                inProcess = false;
                iD.getR().complete(iD.getResults());
            }
        });

    }


    /**
     * Adds an island to the scanning queue
     * @param island - the island to scan
     *
     */
    public CompletableFuture<Results> addIsland(Island island) {
        CompletableFuture<Results> r = new CompletableFuture<>();
        processQueue.add(new IslandLevelCalculator(addon, island, r));
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


}
