package world.bentobox.level.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.Level;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.objects.TopTenData;

/**
 * @author tastybento
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class })
public class AdminStatsCommandTest {

	@Mock
	private CompositeCommand ic;
	private UUID uuid;
	@Mock
	private User user;
	@Mock
	private IslandsManager im;
	@Mock
	private Island island;
	@Mock
	private Level addon;
	@Mock
	private World world;
	@Mock
	private IslandWorldManager iwm;
	@Mock
	private GameModeAddon gameModeAddon;
	@Mock
	private Player p;
	@Mock
	private LocalesManager lm;
	@Mock
	private PlayersManager pm;

	private AdminStatsCommand asc;
	private TopTenData ttd;
	@Mock
	private LevelsManager manager;
	@Mock
	private Server server;

	@Before
	public void setUp() {
		// Set up plugin
		BentoBox plugin = mock(BentoBox.class);
		Whitebox.setInternalState(BentoBox.class, "instance", plugin);
		User.setPlugin(plugin);
		when(addon.getPlugin()).thenReturn(plugin);

		// Addon
		when(ic.getAddon()).thenReturn(addon);
		when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
		when(ic.getLabel()).thenReturn("island");
		when(ic.getTopLabel()).thenReturn("island");
		when(ic.getWorld()).thenReturn(world);
		when(ic.getTopLabel()).thenReturn("bsb");

		// IWM friendly name
		when(plugin.getIWM()).thenReturn(iwm);
		when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

		// World
		when(world.toString()).thenReturn("world");
		when(world.getName()).thenReturn("BSkyBlock_world");

		// Player manager
		when(plugin.getPlayers()).thenReturn(pm);
		when(pm.getUser(anyString())).thenReturn(user);
		// topTen
		when(addon.getManager()).thenReturn(manager);
		// User
		uuid = UUID.randomUUID();
		when(user.getUniqueId()).thenReturn(uuid);
		when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

		// Bukkit
		PowerMockito.mockStatic(Bukkit.class);
		when(Bukkit.getServer()).thenReturn(server);
		// Mock item factory (for itemstacks)
		ItemFactory itemFactory = mock(ItemFactory.class);
		ItemMeta itemMeta = mock(ItemMeta.class);
		when(itemFactory.getItemMeta(any())).thenReturn(itemMeta);
		when(server.getItemFactory()).thenReturn(itemFactory);
		when(Bukkit.getItemFactory()).thenReturn(itemFactory);

		// Top ten
		ttd = new TopTenData(world);
		Map<UUID, Long> topten = new HashMap<>();
		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			topten.put(UUID.randomUUID(), r.nextLong(20000));
		}
		ttd.setTopTen(topten);
		asc = new AdminStatsCommand(addon, ic);
	}

	@After
	public void tearDown() {
		User.clearUsers();
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
