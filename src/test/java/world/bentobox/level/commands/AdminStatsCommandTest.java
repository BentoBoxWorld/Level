package world.bentobox.level.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.objects.TopTenData;

/**
 * @author tastybento
 */
public class AdminStatsCommandTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;
    @Mock
    private PlayersManager pm;
    @Mock
    private User user;
    
    private AdminStatsCommand asc;
    private TopTenData ttd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Player manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUser(anyString())).thenReturn(user);
        // topTen
        when(addon.getManager()).thenReturn(manager);
        // User
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Top ten
        ttd = new TopTenData(world);
        Map<String, Long> topten = new HashMap<>();
        Random r = new Random();
        for (int i = 0; i < 1000; i++) {
            topten.put(UUID.randomUUID().toString(), r.nextLong(20000));
        }
        ttd.setTopTen(topten);
        asc = new AdminStatsCommand(addon, ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.AdminStatsCommand#setup()}.
     */
    @Test
    public void testSetup() {
        assertEquals("bskyblock.admin.stats", asc.getPermission());
        assertFalse(asc.isOnlyPlayer());
        assertEquals("admin.stats.description", asc.getDescription());

    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.AdminStatsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        assertFalse(asc.execute(user, "", List.of()));
        verify(user).sendMessage("admin.stats.title");
        verify(user).sendMessage("admin.stats.no-data");
    }

    /**
     * Test method for
     * {@link world.bentobox.level.commands.AdminStatsCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringLevels() {
        Map<World, TopTenData> map = new HashMap<>();
        map.put(world, ttd);
        when(manager.getTopTenLists()).thenReturn(map);
        assertTrue(asc.execute(user, "", List.of()));
        verify(user).sendMessage("admin.stats.title");
        verify(user, never()).sendMessage("admin.stats.no-data");
    }

}
