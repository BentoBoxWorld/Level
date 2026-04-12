package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link AdminSetInitialLevelCommand}
 */
public class AdminSetInitialLevelCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private LevelsManager manager;

    private AdminSetInitialLevelCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getPlayers()).thenReturn(pm);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getPlayers()).thenReturn(pm);
        when(addon.getIslands()).thenReturn(im);

        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(pm.getUUID(anyString())).thenReturn(uuid);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(island.getUniqueId()).thenReturn(uuid.toString());

        cmd = new AdminSetInitialLevelCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("admin.level.sethandicap"));
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("sethandicap", cmd.getLabel());
    }

    @Test
    public void testCanExecuteWrongArgCount() {
        assertFalse(cmd.canExecute(user, "sethandicap", List.of("player1")));
        verify(user).sendMessage(eq("commands.help.header"), anyString(), anyString());
    }

    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID("unknown")).thenReturn(null);
        assertFalse(cmd.canExecute(user, "sethandicap", List.of("unknown", "100")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("unknown"));
    }

    @Test
    public void testCanExecuteInvalidNumber() {
        assertFalse(cmd.canExecute(user, "sethandicap", List.of("tastybento", "notanumber")));
        verify(user).sendMessage("admin.level.sethandicap.invalid-level");
    }

    @Test
    public void testCanExecutePlayerNoIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        assertFalse(cmd.canExecute(user, "sethandicap", List.of("tastybento", "100")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    public void testCanExecuteValid() {
        assertTrue(cmd.canExecute(user, "sethandicap", List.of("tastybento", "100")));
    }

    @Test
    public void testCanExecuteValidWithPlus() {
        assertTrue(cmd.canExecute(user, "sethandicap", List.of("tastybento", "+50")));
    }

    @Test
    public void testCanExecuteValidWithMinus() {
        assertTrue(cmd.canExecute(user, "sethandicap", List.of("tastybento", "-20")));
    }

    @Test
    public void testExecuteSetAbsolute() {
        when(manager.getInitialCount(island)).thenReturn(50L);
        cmd.canExecute(user, "sethandicap", List.of("tastybento", "100"));
        assertTrue(cmd.execute(user, "sethandicap", List.of("tastybento", "100")));
        verify(manager).setInitialIslandCount(island, 100L);
    }

    @Test
    public void testExecuteSetPlus() {
        when(manager.getInitialCount(island)).thenReturn(50L);
        cmd.canExecute(user, "sethandicap", List.of("tastybento", "+10"));
        assertTrue(cmd.execute(user, "sethandicap", List.of("tastybento", "+10")));
        verify(manager).setInitialIslandCount(island, 60L);
    }

    @Test
    public void testExecuteSetMinus() {
        when(manager.getInitialCount(island)).thenReturn(50L);
        cmd.canExecute(user, "sethandicap", List.of("tastybento", "-20"));
        assertTrue(cmd.execute(user, "sethandicap", List.of("tastybento", "-20")));
        verify(manager).setInitialIslandCount(island, 30L);
    }
}
