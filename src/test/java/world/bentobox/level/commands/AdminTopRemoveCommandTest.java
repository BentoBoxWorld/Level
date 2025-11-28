package world.bentobox.level.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import world.bentobox.level.objects.TopTenData;

/**
 * @author tastybento
 *
 */
public class AdminTopRemoveCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private TopTenData ttd;
    @Mock
    private LevelsManager manager;

    private AdminTopRemoveCommand atrc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Player manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUser(anyString())).thenReturn(user);
        // topTen
        when(addon.getManager()).thenReturn(manager);
        // User
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        // Island
        when(island.getUniqueId()).thenReturn(uuid.toString());
        when(island.getOwner()).thenReturn(uuid);
        // Island Manager
        when(im.getIslands(any(), any(User.class))).thenReturn(List.of(island));
        when(im.getIslands(any(), any(UUID.class))).thenReturn(List.of(island));

        atrc = new AdminTopRemoveCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#AdminTopRemoveCommand(world.bentobox.level.Level, world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    public void testAdminTopRemoveCommand() {
        assertEquals("remove", atrc.getLabel());
        assertEquals("delete", atrc.getAliases().get(0));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("bskyblock.admin.top.remove", atrc.getPermission());
        assertEquals("admin.top.remove.parameters", atrc.getParameters());
        assertEquals("admin.top.remove.description", atrc.getDescription());
        assertFalse(atrc.isOnlyPlayer());

    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteWrongArgs() {
        assertFalse(atrc.canExecute(user, "delete", Collections.emptyList()));
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "BSkyBlock");
    }

    /**
     * Test method for {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknown() {
        when(pm.getUser(anyString())).thenReturn(null);
        assertFalse(atrc.canExecute(user, "delete", Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteKnown() {
        assertTrue(atrc.canExecute(user, "delete", Collections.singletonList("tastybento")));
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.admin.AdminTopRemoveCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        testCanExecuteKnown();
        assertTrue(atrc.execute(user, "delete", Collections.singletonList("tastybento")));
        verify(manager).removeEntry(world, uuid.toString());
        verify(user).sendMessage("general.success");
    }

}
