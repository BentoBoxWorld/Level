package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        when(user.getTranslation("island.donate.inv.keyword")).thenReturn("inv");
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        when(user.getTranslationOrNothing(anyString(), anyString(), anyString())).thenReturn("");
        when(user.getLocale()).thenReturn(Locale.US);
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
        // When no args, should suggest "hand" and "inv"
        var result = cmd.tabComplete(user, "donate", Collections.emptyList());
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("hand"));
        assertTrue(result.get().contains("inv"));
    }

    @Test
    void testTabCompleteFirstArgFromBentoBoxFlow() {
        // BentoBox passes the leaf command label as args.get(0); the partial first
        // user arg sits in args.get(1). Empty string = bare "/island donate <TAB>".
        var result = cmd.tabComplete(user, "donate", List.of("donate", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("hand"));
        assertTrue(result.get().contains("inv"));
    }

    @Test
    void testTabCompleteSecondArgAfterHandSuggestsHeldAmount() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(7);
        when(inventory.getItemInMainHand()).thenReturn(stack);

        var result = cmd.tabComplete(user, "donate", List.of("donate", "hand", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("7"));
    }

    @Test
    void testTabCompleteAfterInvSuggestsNothing() {
        var result = cmd.tabComplete(user, "donate", List.of("donate", "inv", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void testExecuteInvEmptyInventory() {
        when(inventory.getStorageContents()).thenReturn(new ItemStack[] { null, null, null });

        assertFalse(cmd.execute(user, "donate", List.of("inv")));
        verify(user).sendMessage("island.donate.empty");
        verify(manager, never()).donateBlocks(any(), any(UUID.class), anyString(), anyInt(), anyLong());
    }

    @Test
    void testExecuteInvNoValuableBlocks() {
        // Stone with no value, sword (not a block)
        ItemStack stone = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(stone.getAmount()).thenReturn(5);
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[] { stone, sword });
        when(blockConfig.getValue(any(), eq(Material.STONE))).thenReturn(null);

        assertFalse(cmd.execute(user, "donate", List.of("inv")));
        verify(user).sendMessage("island.donate.empty");
        verify(manager, never()).donateBlocks(any(), any(UUID.class), anyString(), anyInt(), anyLong());
    }

    @Test
    void testExecuteInvShowsConfirmationPrompt() {
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.getType()).thenReturn(Material.DIAMOND_BLOCK);
        when(diamond.getAmount()).thenReturn(2);
        ItemStack gold = mock(ItemStack.class);
        when(gold.getType()).thenReturn(Material.GOLD_BLOCK);
        when(gold.getAmount()).thenReturn(3);
        // Non-donatable item is ignored, not destroyed
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);

        when(inventory.getStorageContents())
                .thenReturn(new ItemStack[] { diamond, sword, gold });
        when(blockConfig.getValue(any(), eq(Material.DIAMOND_BLOCK))).thenReturn(100);
        when(blockConfig.getValue(any(), eq(Material.GOLD_BLOCK))).thenReturn(50);

        assertTrue(cmd.execute(user, "donate", List.of("inv")));
        // The confirmation header should have been requested via getTranslation
        verify(user).getTranslation("island.donate.inv.confirm-header");
        // No donation yet — only confirmation requested
        verify(manager, never()).donateBlocks(any(), any(UUID.class), anyString(), anyInt(), anyLong());
    }
}
