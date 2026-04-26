package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;
import world.bentobox.level.objects.IslandLevels;
import world.bentobox.level.panels.ValuePanel;

/**
 * Tests for {@link IslandValueCommand}
 */
public class IslandValueCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private BlockConfig blockConfig;
    @Mock
    private LevelsManager manager;
    @Mock
    private ConfigSettings settings;
    @Mock
    private Player player;
    @Mock
    private PlayerInventory inventory;
    @Mock
    private IslandLevels islandLevels;

    private IslandValueCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getBlockConfig()).thenReturn(blockConfig);
        when(addon.getManager()).thenReturn(manager);
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getUnderWaterMultiplier()).thenReturn(1.0);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(true);
        when(user.getPlayer()).thenReturn(player);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation("island.donate.hand.keyword")).thenReturn("hand");
        when(user.getTranslationOrNothing(anyString())).thenReturn("");

        when(player.getInventory()).thenReturn(inventory);
        when(im.getPrimaryIsland(any(), any(UUID.class))).thenReturn(island);
        when(manager.getLevelsData(any())).thenReturn(islandLevels);
        when(islandLevels.getMdCount()).thenReturn(Collections.emptyMap());
        when(islandLevels.getUwCount()).thenReturn(Collections.emptyMap());

        cmd = new IslandValueCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSetup() {
        assertTrue(cmd.getPermission().contains("island.value"));
        assertTrue(cmd.isOnlyPlayer());
        assertEquals("value", cmd.getLabel());
    }

    @Test
    public void testExecuteNoArgsOpenValuePanel() {
        try (MockedStatic<ValuePanel> mockedPanel = mockStatic(ValuePanel.class)) {
            assertTrue(cmd.execute(user, "value", Collections.emptyList()));
            mockedPanel.verify(() -> ValuePanel.openPanel(any(), any(), any(User.class)));
        }
    }

    @Test
    public void testExecuteTooManyArgsShowsHelp() {
        assertFalse(cmd.execute(user, "value", List.of("STONE", "extra")));
    }

    @Test
    public void testExecuteUnknownMaterial() {
        assertTrue(cmd.execute(user, "value", List.of("NOTAMATERIAL")));
        // Sends unknown-item message via Utils.sendMessage
        verify(user, times(2)).getTranslation(anyString()); // keyword lookup + unknown item message
    }

    @Test
    public void testTabCompleteEmptyArgsReturnsEmpty() {
        var result = cmd.tabComplete(user, "value", Collections.emptyList());
        assertFalse(result.isPresent());
    }
}
