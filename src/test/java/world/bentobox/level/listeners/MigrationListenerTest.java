package world.bentobox.level.listeners;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link MigrationListener}
 */
public class MigrationListenerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;

    private MigrationListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        listener = new MigrationListener(addon);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnBentoBoxReadyCallsLoadTopTens() {
        BentoBoxReadyEvent event = new BentoBoxReadyEvent();
        listener.onBentoBoxReady(event);
        verify(manager).loadTopTens();
    }
}
