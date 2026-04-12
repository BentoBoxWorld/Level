package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.panels.TopLevelPanel;

/**
 * Tests for {@link IslandTopCommand}
 */
public class IslandTopCommandTest extends CommonTestSetup {

    @Mock
    private User user;

    private IslandTopCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(true);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        cmd = new IslandTopCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("island.top"));
        assertTrue(cmd.isOnlyPlayer());
        assertEquals("top", cmd.getLabel());
    }

    @Test
    public void testExecuteOpensPanelAndReturnsTrue() {
        try (MockedStatic<TopLevelPanel> mockedPanel = mockStatic(TopLevelPanel.class)) {
            assertTrue(cmd.execute(user, "top", Collections.emptyList()));
            mockedPanel.verify(() -> TopLevelPanel.openPanel(any(), any(User.class), any(), anyString()));
        }
    }
}
