package world.bentobox.level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.config.Settings;
import world.bentobox.level.objects.TopTenData;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, DatabaseSetup.class, PanelBuilder.class})
public class TopTenTest {

    @Mock
    private Level addon;
    @Mock
    private World world;
    @Mock
    private BentoBox plugin;
    @Mock
    private static AbstractDatabaseHandler<Object> handler;
    private List<Object> topTen;
    @Mock
    private IslandsManager im;
    @Mock
    private Player player;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Inventory inv;
    @Mock
    private LevelPresenter lp;
    @Mock
    private Settings settings;


    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() {
        // This has to be done beforeClass otherwise the tests will interfere with each other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
    }


    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(addon.getPlugin()).thenReturn(plugin);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getWorld(anyString())).thenReturn(world);
        Server server = mock(Server.class);
        when(server.getPlayer(any(UUID.class))).thenReturn(player);
        when(Bukkit.getServer()).thenReturn(server);
        // Has perms
        when(player.hasPermission(anyString())).thenReturn(true);
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

        // User
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(eq("island.top.gui-heading"), eq("[name]"), anyString(), eq("[rank]"), anyString())).thenReturn("gui-heading");
        when(user.getTranslation(eq("island.top.island-level"),eq("[level]"), anyString())).thenReturn("island-level");
        when(user.getPlayer()).thenReturn(player);

        // Player Manager
        when(addon.getPlayers()).thenReturn(pm);
        when(pm.getName(any())).thenReturn("player1",
                "player2",
                "player3",
                "player4",
                "player5",
                "player6",
                "player7",
                "player8",
                "player9",
                "player10"
                );
        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        ItemMeta itemMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);

        // Inventory GUI
        when(Bukkit.createInventory(any(), anyInt(), anyString())).thenReturn(inv);

        // Level presenter
        when(addon.getLevelPresenter()).thenReturn(lp);
        when(lp.getLevelString(anyLong())).thenAnswer((Answer<String>) invocation -> String.valueOf(invocation.getArgument(0, Long.class)));
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
    public void testAddEntryNotOwner() throws Exception {
        // Database
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        assertEquals(0, tt.getTopTenList(world).getTopTen().size());
    }

    @Test
    public void testAddEntryIsOwner() throws Exception  {
        when(im.isOwner(any(), any())).thenReturn(true);
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        assertTrue(tt.getTopTenList(world).getTopTen().get(ownerUUID) == 200L);
    }

    @Test
    public void testAddEntryIsOwnerNoPermission() throws Exception  {
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
    public void testGetGUIFullTopTen() {
        TopTen tt = new TopTen(addon);
        tt.getGUI(world, user, "bskyblock");
        verify(user).getTranslation(eq("island.top.gui-title"));
        verify(player).openInventory(inv);
        int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
        for (int i : SLOTS) {
            verify(inv).setItem(eq(i), any());
        }

    }

    @Test
    public void testGetGUINoPerms() {
        when(player.hasPermission(anyString())).thenReturn(false);
        TopTen tt = new TopTen(addon);
        tt.getGUI(world, user, "bskyblock");
        verify(user).getTranslation(eq("island.top.gui-title"));
        verify(player).openInventory(inv);
        int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
        for (int i : SLOTS) {
            verify(inv, Mockito.never()).setItem(eq(i), any());
        }

    }

    @Test
    public void testGetGUINoTopTen() throws Exception {
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        tt.getGUI(world, user, "bskyblock");
        verify(user).getTranslation(eq("island.top.gui-title"));
        verify(player).openInventory(inv);
        int[] SLOTS = new int[] {4, 12, 14, 19, 20, 21, 22, 23, 24, 25};
        for (int i : SLOTS) {
            verify(inv, Mockito.never()).setItem(eq(i), any());
        }

    }

    @Test
    public void testGetTopTenList() {
        TopTen tt = new TopTen(addon);
        TopTenData ttdList = tt.getTopTenList(world);
        assertEquals(plugin, ttdList.getPlugin());
    }

    @Test
    public void testGetTopTenListNewWorld() {
        TopTen tt = new TopTen(addon);
        TopTenData ttdList = tt.getTopTenList(mock(World.class));
        assertEquals(plugin, ttdList.getPlugin());
    }

    @Test
    public void testRemoveEntry() throws Exception {
        // Add entry
        when(im.isOwner(any(), any())).thenReturn(true);
        when(handler.loadObjects()).thenReturn(new ArrayList<>());
        TopTen tt = new TopTen(addon);
        UUID ownerUUID = UUID.randomUUID();
        tt.addEntry(world, ownerUUID, 200L);
        assertTrue(tt.getTopTenList(world).getTopTen().get(ownerUUID) == 200L);
        // Remove it
        tt.removeEntry(world, ownerUUID);
        assertNull(tt.getTopTenList(world).getTopTen().get(ownerUUID));
    }

    @Test
    public void testSaveTopTen() throws Exception {
        TopTen tt = new TopTen(addon);
        tt.saveTopTen();
        verify(handler).saveObject(any());
    }

}
