package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.level.calculators.PlayerLevel;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, LevelPresenter.class})
public class LevelPresenterTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private Level addon;
    @Mock
    private PlayerLevel pl;
    @Mock
    private ConfigSettings settings;
    @Mock
    private BlockConfig blockConfig;

    @Before
    public void setUp() throws Exception {
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("world");
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        // Has island
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        // In team
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        // team leader
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(UUID.randomUUID());

        // Player level
        PowerMockito.whenNew(PlayerLevel.class).withAnyArguments().thenReturn(pl);

        // Settings
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getBlockConfig()).thenReturn(blockConfig);
    }

    /**
     * Test method for {@link LevelPresenter#LevelPresenter(Level, world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testLevelPresenter() {
        new LevelPresenter(addon, plugin);
    }

    /**
     * Test method for {@link LevelPresenter#calculateIslandLevel(org.bukkit.World, world.bentobox.bentobox.api.user.User, java.util.UUID)}.
     */
    @Test
    public void testCalculateIslandLevel() {
        LevelPresenter lp = new LevelPresenter(addon, plugin);
        World world = mock(World.class);
        User sender = mock(User.class);
        UUID targetPlayer = UUID.randomUUID();
        lp.calculateIslandLevel(world, sender, targetPlayer);

        Mockito.verify(sender).sendMessage("island.level.calculating");
    }

    /**
     * Test method for {@link LevelPresenter#getLevelString(long)}.
     */
    @Test
    public void testGetLevelStringLong() {
        LevelPresenter lp = new LevelPresenter(addon, plugin);
        assertEquals("123456789", lp.getLevelString(123456789L));

    }

    /**
     * Test method for {@link LevelPresenter#getLevelString(long)}.
     */
    @Test
    public void testGetLevelStringLongShorthand() {
        when(settings.isShorthand()).thenReturn(true);
        LevelPresenter lp = new LevelPresenter(addon, plugin);
        assertEquals("123.5M", lp.getLevelString(123456789L));
        assertEquals("1.2k", lp.getLevelString(1234L));
        assertEquals("123.5G", lp.getLevelString(123456789352L));
        assertEquals("1.2T", lp.getLevelString(1234567893524L));
        assertEquals("12345.7T", lp.getLevelString(12345678345345349L));

    }
}
