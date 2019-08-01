package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.level.objects.TopTenData;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, DatabaseSetup.class})
public class TopTenTest {

    @Mock
    private Level addon;
    @Mock
    private World world;
    @Mock
    private BentoBox plugin;
    @Mock
    private AbstractDatabaseHandler<Object> handler;
    private List<Object> topTen;
    @Mock
    private IslandsManager im;
    @Mock
    private Player player;
    @Mock
    private IslandWorldManager iwm;

    @Before
    public void setUp() throws Exception {
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(addon.getPlugin()).thenReturn(plugin);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getWorld(anyString())).thenReturn(world);
        Server server = mock(Server.class);
        when(server.getPlayer(any(UUID.class))).thenReturn(player);
        when(Bukkit.getServer()).thenReturn(server);
        // Has perms
        when(player.hasPermission(anyString())).thenReturn(true);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
        // Fill the top ten
        TopTenData ttd = new TopTenData();
        ttd.setUniqueId("world");
        topTen = new ArrayList<>();
        for (long i = -100; i < 100; i ++) {
            ttd.addLevel(UUID.randomUUID(), i);
            topTen.add(ttd);
        }
        when(handler.loadObjects()).thenReturn(topTen);

        // Islands
        when(addon.getIslands()).thenReturn(im);
        // World
        when(world.getName()).thenReturn("world");
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTopTen() {
        new TopTen(addon);
        PowerMockito.verifyStatic(Bukkit.class, times(200)); // 1
        Bukkit.getWorld(eq("world"));
    }

    @Test
    public void testTopTenNullWorld() {
        when(Bukkit.getWorld(anyString())).thenReturn(null);
        new TopTen(addon);
        verify(addon, times(200)).logError("TopTen world world is not known on server. Skipping...");
    }

    @Test
    @Ignore("Runs differently singularly vs in order - bug somewhere")
    public void testAddEntryNotOwner() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        tt.getTopTenList(world).getTopTen().forEach((k,v) -> {
            System.out.println(k + " --> " + v);
        });
        assertEquals(0, tt.getTopTenList(world).getTopTen().size());
    }

    @Test
    public void testAddEntryIsOwner() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        when(im.isOwner(any(), any())).thenReturn(true);
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        assertTrue(tt.getTopTenList(world).getTopTen().get(ownerUUID) == 200L);
    }

    @Test
    public void testAddEntryIsOwnerNoPermission() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(im.isOwner(any(), any())).thenReturn(true);
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        assertNull(tt
                .getTopTenList(world)
                .getTopTen()
                .get(ownerUUID));
    }

    @Test
    @Ignore
    public void testGetGUI() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    @Ignore
    public void testGetTopTenList() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    @Ignore
    public void testRemoveEntry() {
        fail("Not yet implemented"); // TODO
    }

    @Test
    @Ignore
    public void testSaveTopTen() {
        fail("Not yet implemented"); // TODO
    }

}
