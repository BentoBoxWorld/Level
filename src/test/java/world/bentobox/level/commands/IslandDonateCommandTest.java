package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.Level;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.panels.DonationPanel;

/**
 * Tests for {@link IslandDonateCommand}
 */
class IslandDonateCommandTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private LevelsManager manager;
    @Mock
    private BlockConfig blockConfig;
    @Mock
    private Player player;
    @Mock
    private PlayerInventory inventory;

    private IslandDonateCommand cmd;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(addon.getBlockConfig()).thenReturn(blockConfig);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isPlayer()).thenReturn(true);
        when(user.getPlayer()).thenReturn(player);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0, String.class));
        when(user.getTranslation("island.donate.hand.keyword")).thenReturn("hand");
        when(user.getLocation()).thenReturn(location);

        when(player.getInventory()).thenReturn(inventory);

        Location loc = mock(Location.class);
        when(user.getLocation()).thenReturn(loc);

        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(island.onIsland(any())).thenReturn(true);
        when(island.isAllowed(any(User.class), any(Flag.class))).thenReturn(true);

        cmd = new IslandDonateCommand(addon, ic);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testSetup() {
        assertTrue(cmd.getPermission().contains("island.donate"));
        assertTrue(cmd.isOnlyPlayer());
        assertEquals("donate", cmd.getLabel());
    }

    @Test
    void testExecuteNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        assertFalse(cmd.execute(user, "donate", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    void testExecuteNotOnIsland() {
        when(island.onIsland(any())).thenReturn(false);
        assertFalse(cmd.execute(user, "donate", Collections.emptyList()));
        verify(user).sendMessage("island.donate.must-be-on-island");
    }

    @Test
    void testExecuteFlagDenied() {
        when(island.isAllowed(any(User.class), any(Flag.class))).thenReturn(false);
        assertFalse(cmd.execute(user, "donate", Collections.emptyList()));
        verify(user).sendMessage("island.donate.no-permission");
    }

    @Test
    void testExecuteNoArgsOpensDonationPanel() {
        try (MockedStatic<DonationPanel> mockedPanel = mockStatic(DonationPanel.class)) {
            assertTrue(cmd.execute(user, "donate", Collections.emptyList()));
            mockedPanel.verify(() -> DonationPanel.openPanel(any(), any(), any(User.class), any()));
        }
    }

    @Test
    void testExecuteHandAirItem() {
        ItemStack airStack = mock(ItemStack.class);
        when(airStack.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(airStack);

        assertFalse(cmd.execute(user, "donate", List.of("hand")));
        verify(user).sendMessage("island.donate.hand.not-block");
    }

    @Test
    void testExecuteHandNonBlockItem() {
        ItemStack swordStack = mock(ItemStack.class);
        when(swordStack.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(inventory.getItemInMainHand()).thenReturn(swordStack);

        assertFalse(cmd.execute(user, "donate", List.of("hand")));
        verify(user).sendMessage("island.donate.hand.not-block");
    }

    @Test
    void testExecuteHandBlockNoValue() {
        // Material.STONE is a real block (isBlock()=true, isAir()=false) so no stubbing needed for those
        ItemStack stoneStack = mock(ItemStack.class);
        when(stoneStack.getType()).thenReturn(Material.STONE);
        when(stoneStack.getAmount()).thenReturn(1);
        when(inventory.getItemInMainHand()).thenReturn(stoneStack);
        when(blockConfig.getValue(any(), any())).thenReturn(null);

        assertFalse(cmd.execute(user, "donate", List.of("hand")));
        verify(user).sendMessage("island.donate.no-value");
    }

    @Test
    void testTabCompleteNoArgs() {
        // When no args, should suggest "hand"
        var result = cmd.tabComplete(user, "donate", Collections.emptyList());
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("hand"));
    }
}
