package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.calculators.Results.Result;
import world.bentobox.level.config.ConfigSettings;

/**
 * Tests for {@link IslandLevelCommand}
 */
public class IslandLevelCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private LevelsManager manager;
    @Mock
    private Pipeliner pipeliner;
    @Mock
    private ConfigSettings settings;

    private IslandLevelCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getPipeliner()).thenReturn(pipeliner);
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getLevelWait()).thenReturn(0);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(true);
        when(user.isOp()).thenReturn(false);
        when(user.hasPermission(anyString())).thenReturn(false);
        when(user.getTranslation(any())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(any(), any())).thenAnswer(i -> i.getArgument(0, String.class));

        when(pipeliner.getIslandsInQueue()).thenReturn(0);
        when(pipeliner.getTime()).thenReturn(1);

        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(island.getMemberSet()).thenReturn(com.google.common.collect.ImmutableSet.of(uuid));

        cmd = new IslandLevelCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("island.level"));
    }

    @Test
    public void testExecuteConsoleNoArgsShowsError() {
        when(user.isPlayer()).thenReturn(false);
        assertFalse(cmd.execute(user, "level", Collections.emptyList()));
        verify(user).sendMessage("general.errors.use-in-game");
    }

    @Test
    public void testExecuteUnknownPlayerArg() {
        when(pm.getUUID(anyString())).thenReturn(null);
        assertFalse(cmd.execute(user, "level", List.of("unknownplayer")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), anyString());
    }

    @Test
    public void testExecuteOtherPlayerLevelRequest() {
        UUID otherUUID = UUID.randomUUID();
        when(pm.getUUID("otherguy")).thenReturn(otherUUID);
        when(manager.getIslandLevelString(any(), eq(otherUUID))).thenReturn("10");

        assertTrue(cmd.execute(user, "level", List.of("otherguy")));
        verify(user).sendMessage(eq("island.level.island-level-is"), anyString(), anyString());
    }

    @Test
    public void testExecuteSelfNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(cmd.execute(user, "level", Collections.emptyList()));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    public void testExecuteSelfWithIslandSendsCalculating() {
        Results results = new Results(Result.AVAILABLE);
        when(manager.calculateLevel(any(), any())).thenReturn(CompletableFuture.completedFuture(results));
        when(manager.getIslandLevelString(any(), any())).thenReturn("5");

        assertTrue(cmd.execute(user, "level", Collections.emptyList()));
        verify(user).sendMessage("island.level.calculating");
    }

    @Test
    public void testExecuteSelfResultInProgress() {
        Results results = new Results(Result.IN_PROGRESS);
        when(manager.calculateLevel(any(), any())).thenReturn(CompletableFuture.completedFuture(results));

        cmd.execute(user, "level", Collections.emptyList());
        verify(user).sendMessage("island.level.in-progress");
    }

    @Test
    public void testExecuteSelfResultTimeout() {
        Results results = new Results(Result.TIMEOUT);
        when(manager.calculateLevel(any(), any())).thenReturn(CompletableFuture.completedFuture(results));

        cmd.execute(user, "level", Collections.emptyList());
        verify(user).sendMessage("island.level.time-out");
    }

    @Test
    public void testExecuteSelfNullResultIsIgnored() {
        when(manager.calculateLevel(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        // Should not throw
        assertTrue(cmd.execute(user, "level", Collections.emptyList()));
    }
}
