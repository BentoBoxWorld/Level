package world.bentobox.level.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link LevelRequestHandler}
 */
public class LevelRequestHandlerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;

    private LevelRequestHandler handler;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(world);
        handler = new LevelRequestHandler(addon);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testNullMapReturnsZero() {
        assertEquals(0L, handler.handle(null));
    }

    @Test
    public void testEmptyMapReturnsZero() {
        assertEquals(0L, handler.handle(Collections.emptyMap()));
    }

    @Test
    public void testMissingWorldNameReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    public void testWorldNameWrongTypeReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", 42);
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    public void testPlayerWrongTypeReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        map.put("player", "not-a-uuid");
        assertEquals(0L, handler.handle(map));
    }

    @Test
    public void testMissingPlayerReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        assertEquals(0L, handler.handle(map));
    }

    @Test
    public void testWorldNotFoundReturnsZero() {
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(null);
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "nonexistent_world");
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    public void testValidInputDelegatesToManager() {
        UUID playerUUID = UUID.randomUUID();
        when(manager.getIslandLevel(any(World.class), any(UUID.class))).thenReturn(42L);

        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        map.put("player", playerUUID);

        assertEquals(42L, handler.handle(map));
    }

    @Test
    public void testHandlerLabel() {
        assertEquals("island-level", handler.getLabel());
    }
}
