package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link AdminTopCommand}
 */
public class AdminTopCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private LevelsManager manager;
    @Mock
    private PlayersManager pm;

    private AdminTopCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(plugin.getPlayers()).thenReturn(pm);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        cmd = new AdminTopCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("admin.top"));
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("top", cmd.getLabel());
    }

    @Test
    public void testExecuteEmptyTopTen() {
        when(manager.getTopTen(any(), any(Integer.class))).thenReturn(Collections.emptyMap());
        assertTrue(cmd.execute(user, "top", Collections.emptyList()));
        verify(user).sendMessage("island.top.gui-title");
    }

    @Test
    public void testExecuteWithTopTenEntries() {
        Map<String, Long> topTen = new LinkedHashMap<>();
        topTen.put(uuid.toString(), 100L);
        when(manager.getTopTen(any(), any(Integer.class))).thenReturn(topTen);
        when(plugin.getIslands().getIslandById(anyString())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(uuid);
        User ownerUser = org.mockito.Mockito.mock(User.class);
        when(ownerUser.getName()).thenReturn("tastybento");
        when(pm.getUser(uuid)).thenReturn(ownerUser);

        assertTrue(cmd.execute(user, "top", Collections.emptyList()));
        verify(user).sendMessage("island.top.gui-title");
    }

    @Test
    public void testExecuteSkipsIslandNotFound() {
        Map<String, Long> topTen = new LinkedHashMap<>();
        topTen.put("some-island-id", 100L);
        when(manager.getTopTen(any(), any(Integer.class))).thenReturn(topTen);
        when(plugin.getIslands().getIslandById(anyString())).thenReturn(Optional.empty());

        assertTrue(cmd.execute(user, "top", Collections.emptyList()));
        // gui-title still sent, but no display message
        verify(user).sendMessage("island.top.gui-title");
    }
}
