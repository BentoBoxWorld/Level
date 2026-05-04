package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import world.bentobox.bentobox.util.Util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.calculators.Pipeliner;
import world.bentobox.level.calculators.Results;
import world.bentobox.level.calculators.Results.Result;
import world.bentobox.level.config.ConfigSettings;

/**
 * Tests for {@link AdminLevelCommand}
 */
class AdminLevelCommandTest extends CommonTestSetup {

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

    private AdminLevelCommand cmd;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getPipeliner()).thenReturn(pipeliner);
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getLevelWait()).thenReturn(0);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(false); // admin command can run as console
        when(user.isOp()).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(any(), any())).thenAnswer(i -> i.getArgument(0, String.class));

        when(pipeliner.getIslandsInQueue()).thenReturn(0);
        when(pipeliner.getTime()).thenReturn(1);
        when(pm.getUUID(anyString())).thenReturn(uuid);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(island.getMemberSet()).thenReturn(com.google.common.collect.ImmutableSet.of(uuid));

        Results results = new Results(Result.AVAILABLE);
        when(manager.calculateLevel(any(), any())).thenReturn(CompletableFuture.completedFuture(results));
        when(manager.getIslandLevelString(any(), any())).thenReturn("5");
        when(settings.isLogReportToConsole()).thenReturn(false);

        cmd = new AdminLevelCommand(addon, ic);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSetup() {
        assertTrue(cmd.getPermission().contains("admin.level"));
        assertFalse(cmd.isOnlyPlayer());
    }

    @Test
    void testExecuteConsoleScanIsland() {
        // Console with player name arg scans island
        assertTrue(cmd.execute(user, "level", List.of("tastybento")));
    }

    @Test
    void testTabCompleteNoArgs() {
        Optional<List<String>> result = cmd.tabComplete(user, "level", Collections.emptyList());
        assertFalse(result.isPresent()); // empty args => return empty Optional
    }

    @Test
    void testTabCompleteWithArgs() {
        // getOnlinePlayerList calls Bukkit.getOnlinePlayers() — stub it to return empty list
        mockedUtil.when(() -> Util.getOnlinePlayerList(any())).thenReturn(List.of());
        Optional<List<String>> result = cmd.tabComplete(user, "level", List.of("tas"));
        assertTrue(result.isPresent()); // has args => provides list (possibly empty)
    }
}
