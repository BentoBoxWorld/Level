package world.bentobox.level.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandPreclearEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.api.events.island.IslandUnregisteredEvent;
import world.bentobox.bentobox.api.events.team.TeamSetownerEvent;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.config.ConfigSettings;

/**
 * Tests for {@link IslandActivitiesListeners}
 */
class IslandActivitiesListenersTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;
    @Mock
    private Pipeliner pipeliner;
    @Mock
    private ConfigSettings settings;

    private IslandActivitiesListeners listener;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(addon.getPipeliner()).thenReturn(pipeliner);
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getPlugin()).thenReturn(plugin);

        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getUniqueId()).thenReturn(uuid.toString());
        when(island.getCenter()).thenReturn(location); // needed by event copy constructors

        Results results = new Results();
        results.setTotalPoints(100L);
        when(pipeliner.zeroIsland(any())).thenReturn(CompletableFuture.completedFuture(results));

        listener = new IslandActivitiesListeners(addon);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // --- IslandCreatedEvent ---

    @Test
    void testOnNewIslandCreatedZeroNewIslandLevelsTrue() {
        when(settings.isZeroNewIslandLevels()).thenReturn(true);
        IslandCreatedEvent event = new IslandCreatedEvent(island, uuid, false, location);
        listener.onNewIsland(event);
        // Scheduler runTaskLater is called
        verify(sch).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    @Test
    void testOnNewIslandCreatedZeroNewIslandLevelsFalse() {
        when(settings.isZeroNewIslandLevels()).thenReturn(false);
        IslandCreatedEvent event = new IslandCreatedEvent(island, uuid, false, location);
        listener.onNewIsland(event);
        verify(sch, never()).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    // --- IslandResettedEvent ---

    @Test
    void testOnIslandResettedZeroNewIslandLevelsTrue() {
        when(settings.isZeroNewIslandLevels()).thenReturn(true);
        IslandResettedEvent event = new IslandResettedEvent(island, uuid, false, location, island);
        listener.onNewIsland(event);
        verify(pipeliner).zeroIsland(island);
    }

    @Test
    void testZeroIslandFoldsInDeferredListenerCredits() {
        // Pin down the post-scan drain: setInitialIslandCount is called with
        // scan.totalPoints, then any listener-during-scan credits for chunks
        // the scan didn't visit are folded in via addToInitialCount. Without
        // this, mid-scan chunk generation would silently leave a positive
        // delta and the island would never read level=0 right after reset.
        when(settings.isZeroNewIslandLevels()).thenReturn(true);
        when(manager.drainZeroScanDeferred(island)).thenReturn(42L);
        IslandResettedEvent event = new IslandResettedEvent(island, uuid, false, location, island);
        listener.onNewIsland(event);
        verify(manager).setInitialIslandCount(island, 100L);
        verify(manager).addToInitialCount(island, 42L);
    }

    @Test
    void testZeroIslandSkipsAddWhenNoDeferredCredits() {
        // Drain returns 0 → no addToInitialCount, since the +0 noop would
        // otherwise pad the database write path with a no-op save.
        when(settings.isZeroNewIslandLevels()).thenReturn(true);
        when(manager.drainZeroScanDeferred(island)).thenReturn(0L);
        IslandResettedEvent event = new IslandResettedEvent(island, uuid, false, location, island);
        listener.onNewIsland(event);
        verify(manager).setInitialIslandCount(island, 100L);
        verify(manager, never()).addToInitialCount(any(), anyLong());
    }

    @Test
    void testOnIslandResettedZeroNewIslandLevelsFalse() {
        when(settings.isZeroNewIslandLevels()).thenReturn(false);
        IslandResettedEvent event = new IslandResettedEvent(island, uuid, false, location, island);
        listener.onNewIsland(event);
        verify(pipeliner, never()).zeroIsland(any());
    }

    // --- IslandPreclearEvent (remove from top ten) ---

    @Test
    void testOnIslandDeleteRemovesFromTopTen() {
        IslandPreclearEvent event = new IslandPreclearEvent(island, uuid, false, location, island);
        listener.onIslandDelete(event);
        verify(manager).removeEntry(world, uuid.toString());
    }

    @Test
    void testOnIslandDeleteNoWorldNoAction() {
        when(island.getWorld()).thenReturn(null);
        IslandPreclearEvent event = new IslandPreclearEvent(island, uuid, false, location, island);
        listener.onIslandDelete(event);
        verify(manager, never()).removeEntry(any(), anyString());
    }

    // --- IslandDeleteEvent ---

    @Test
    void testOnIslandDeletedCallsDeleteIsland() {
        IslandDeleteEvent event = new IslandDeleteEvent(island, uuid, false, location);
        listener.onIslandDeleted(event);
        verify(manager).deleteIsland(uuid.toString());
    }

    // --- IslandUnregisteredEvent ---

    @Test
    void testOnIslandUnregisteredRemovesFromTopTen() {
        IslandUnregisteredEvent event = new IslandUnregisteredEvent(island, uuid, false, location);
        listener.onIsland(event);
        verify(manager).removeEntry(world, uuid.toString());
    }

    // --- TeamSetownerEvent ---

    @Test
    void testOnNewIslandOwnerRemovesEntry() {
        TeamSetownerEvent event = new TeamSetownerEvent(island, UUID.randomUUID(), false, location);
        listener.onNewIslandOwner(event);
        verify(manager).removeEntry(world, uuid.toString());
    }
}
