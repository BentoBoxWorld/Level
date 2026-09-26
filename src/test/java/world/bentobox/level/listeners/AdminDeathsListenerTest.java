package world.bentobox.level.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent;
import world.bentobox.bentobox.api.events.player.PlayerDeathsChangedEvent.Action;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link AdminDeathsListener}
 */
class AdminDeathsListenerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;
    @Mock
    private Island otherIsland;

    private AdminDeathsListener listener;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(addon.isRegisteredGameModeWorld(world)).thenReturn(true);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));
        when(im.getIslands(world, uuid)).thenReturn(List.of(island));
        listener = new AdminDeathsListener(addon);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private PlayerDeathsChangedEvent event(Action action, int amount) {
        return new PlayerDeathsChangedEvent(world, uuid, action, amount, 3, 0);
    }

    @Test
    void testSet() {
        listener.onDeathsChanged(event(Action.SET, 4));
        verify(manager).setDeaths(island, uuid, 4);
    }

    @Test
    void testReset() {
        listener.onDeathsChanged(event(Action.RESET, 0));
        verify(manager).setDeaths(island, uuid, 0);
    }

    @Test
    void testAdd() {
        listener.onDeathsChanged(event(Action.ADD, 2));
        verify(manager).addDeaths(island, uuid, 2);
    }

    @Test
    void testRemove() {
        listener.onDeathsChanged(event(Action.REMOVE, 3));
        verify(manager).removeDeaths(island, uuid, 3);
    }

    @Test
    void testAppliesToEveryIslandPlayerIsMemberOf() {
        when(otherIsland.getMemberSet()).thenReturn(ImmutableSet.of(uuid, UUID.randomUUID()));
        when(im.getIslands(world, uuid)).thenReturn(List.of(island, otherIsland));
        listener.onDeathsChanged(event(Action.REMOVE, 3));
        verify(manager).removeDeaths(island, uuid, 3);
        verify(manager).removeDeaths(otherIsland, uuid, 3);
    }

    @Test
    void testSkipsIslandsWherePlayerIsNotMember() {
        // e.g. trusted/coop islands returned by the lookup
        when(otherIsland.getMemberSet()).thenReturn(ImmutableSet.of(UUID.randomUUID()));
        when(im.getIslands(world, uuid)).thenReturn(List.of(otherIsland));
        listener.onDeathsChanged(event(Action.RESET, 0));
        verify(manager, never()).setDeaths(any(), any(), anyInt());
    }

    @Test
    void testIgnoresUnregisteredWorld() {
        when(addon.isRegisteredGameModeWorld(world)).thenReturn(false);
        listener.onDeathsChanged(event(Action.RESET, 0));
        verifyNoInteractions(manager);
    }
}
