package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.calculators.Pipeliner;

/**
 * Tests for {@link AdminLevelStatusCommand}
 */
class AdminLevelStatusCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private Pipeliner pipeliner;

    private AdminLevelStatusCommand cmd;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getPipeliner()).thenReturn(pipeliner);
        when(user.getTranslation(any())).thenAnswer(i -> i.getArgument(0, String.class));
        cmd = new AdminLevelStatusCommand(addon, ic);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSetup() {
        assertTrue(cmd.getPermission().contains("admin.levelstatus"));
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("levelstatus", cmd.getLabel());
    }

    @Test
    void testExecuteShowsQueueSizeZero() {
        when(pipeliner.getIslandsInQueue()).thenReturn(0);
        assertTrue(cmd.execute(user, "levelstatus", Collections.emptyList()));
        verify(user).sendMessage(eq("admin.levelstatus.islands-in-queue"), eq(TextVariables.NUMBER), eq("0"));
    }

    @Test
    void testExecuteShowsQueueSizeNonZero() {
        when(pipeliner.getIslandsInQueue()).thenReturn(5);
        // The command iterates the in-process and waiting queues for diagnostics.
        // Empty maps/queues are enough to prove non-zero output reaches sendMessage.
        when(pipeliner.getInProcessQueue()).thenReturn(Collections.emptyMap());
        when(pipeliner.getToProcessQueue()).thenReturn(new LinkedList<>());
        assertTrue(cmd.execute(user, "levelstatus", Collections.emptyList()));
        verify(user).sendMessage(eq("admin.levelstatus.islands-in-queue"), eq(TextVariables.NUMBER), eq("5"));
    }
}
