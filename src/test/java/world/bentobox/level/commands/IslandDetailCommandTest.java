package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.panels.DetailsPanel;

/**
 * Tests for {@link IslandDetailCommand}
 */
class IslandDetailCommandTest extends CommonTestSetup {

    @Mock
    private User user;

    private IslandDetailCommand cmd;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(true);
        when(user.getTranslation(any())).thenAnswer(i -> i.getArgument(0, String.class));
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        cmd = new IslandDetailCommand(addon, ic);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSetup() {
        assertTrue(cmd.getPermission().contains("island.detail"));
        assertTrue(cmd.isOnlyPlayer());
        assertEquals("detail", cmd.getLabel());
    }

    @Test
    void testExecuteNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        assertFalse(cmd.execute(user, "detail", Collections.emptyList()));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testExecuteWithIslandOpensPanelAndReturnsTrue() {
        try (MockedStatic<DetailsPanel> mockedPanel = mockStatic(DetailsPanel.class)) {
            assertTrue(cmd.execute(user, "detail", Collections.emptyList()));
            mockedPanel.verify(() -> DetailsPanel.openPanel(any(), any(), any(User.class)));
        }
    }
}
