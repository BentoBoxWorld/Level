package world.bentobox.level.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.config.ConfigSettings;

/**
 * Tests for {@link JoinLeaveListener}
 */
class JoinLeaveListenerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;
    @Mock
    private ConfigSettings settings;
    @Mock
    private Player player;
    @Mock
    private AddonsManager addonsManager;
    @Mock
    private GameModeAddon gameModeAddon2;

    private JoinLeaveListener listener;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getPlugin()).thenReturn(plugin);

        when(player.getUniqueId()).thenReturn(uuid);

        when(plugin.getAddonsManager()).thenReturn(addonsManager);

        // Set up a game mode addon
        AddonDescription desc = mock(AddonDescription.class);
        when(desc.getName()).thenReturn("BSkyBlock");
        when(gameModeAddon2.getDescription()).thenReturn(desc);
        World overWorld = mock(World.class);
        when(gameModeAddon2.getOverWorld()).thenReturn(overWorld);
        when(gameModeAddon2.getIslands()).thenReturn(im);
        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(island);

        when(addonsManager.getGameModeAddons()).thenReturn(List.of(gameModeAddon2));
        when(settings.getGameModes()).thenReturn(List.of()); // no excluded game modes

        listener = new JoinLeaveListener(addon);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCalcOnLoginFalseDoesNotCalculate() {
        when(settings.isCalcOnLogin()).thenReturn(false);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
        listener.onPlayerJoin(event);
        verify(manager, never()).calculateLevel(any(), any());
    }

    @Test
    void testCalcOnLoginTrueWithIslandCalculates() {
        when(settings.isCalcOnLogin()).thenReturn(true);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
        listener.onPlayerJoin(event);
        verify(manager).calculateLevel(any(UUID.class), any());
    }

    @Test
    void testCalcOnLoginTrueNoIslandDoesNotCalculate() {
        when(settings.isCalcOnLogin()).thenReturn(true);
        when(im.getIsland(any(World.class), any(UUID.class))).thenReturn(null);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
        listener.onPlayerJoin(event);
        verify(manager, never()).calculateLevel(any(), any());
    }

    @Test
    void testCalcOnLoginTrueExcludedGameModeSkips() {
        when(settings.isCalcOnLogin()).thenReturn(true);
        when(settings.getGameModes()).thenReturn(List.of("BSkyBlock")); // exclude this game mode
        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
        listener.onPlayerJoin(event);
        verify(manager, never()).calculateLevel(any(), any());
    }
}
