//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.level.listeners;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.level.Level;


/**
 * This listener checks when BentoBox is ready and then tries to migrate Levels addon database, if it is required.
 */
public class MigrationListener implements Listener
{
    public MigrationListener(Level addon)
    {
        this.addon = addon;
    }

    @EventHandler
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Load TopTens
        this.addon.getManager().loadTopTens();
        /*
         * DEBUG code to generate fake islands and then try to level them all.
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            getPlugin().getAddonsManager().getGameModeAddons().stream()
            .filter(gm -> !settings.getGameModes().contains(gm.getDescription().getName()))
            .forEach(gm -> {
                for (int i = 0; i < 1000; i++) {
                    try {
                        NewIsland.builder().addon(gm).player(User.getInstance(UUID.randomUUID())).name("default").reason(Reason.CREATE).noPaste().build();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            });
            // Queue all islands DEBUG

            getIslands().getIslands().stream().filter(Island::isOwned).forEach(is -> {

                this.getManager().calculateLevel(is.getOwner(), is).thenAccept(r ->
                log("Result for island calc " + r.getLevel() + " at " + is.getCenter()));

            });
       }, 60L);*/
    }


    private final Level addon;
}
