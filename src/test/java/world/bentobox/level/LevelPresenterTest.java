package world.bentobox.level;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.level.calculators.PlayerLevel;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, LevelPresenter.class})
public class LevelPresenterTest {

    private BentoBox plugin;
    private Level addon;
    private PlayerLevel pl;

    @Before
    public void setUp() throws Exception {
        plugin = mock(BentoBox.class);
        addon = mock(Level.class);
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
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(UUID.randomUUID());
        
        pl = mock(PlayerLevel.class);
        PowerMockito.whenNew(PlayerLevel.class).withAnyArguments().thenReturn(pl);

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
        // Verify PlayerLevel was called
        Mockito.verify(pl);
    }

}
