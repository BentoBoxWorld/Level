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
class LevelRequestHandlerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;

    private LevelRequestHandler handler;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(world);
        handler = new LevelRequestHandler(addon);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testNullMapReturnsZero() {
        assertEquals(0L, handler.handle(null));
    }

    @Test
    void testEmptyMapReturnsZero() {
        assertEquals(0L, handler.handle(Collections.emptyMap()));
    }

    @Test
    void testMissingWorldNameReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    void testWorldNameWrongTypeReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", 42);
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    void testPlayerWrongTypeReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        map.put("player", "not-a-uuid");
        assertEquals(0L, handler.handle(map));
    }

    @Test
    void testMissingPlayerReturnsZero() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        assertEquals(0L, handler.handle(map));
    }

    @Test
    void testWorldNotFoundReturnsZero() {
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(null);
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "nonexistent_world");
        map.put("player", UUID.randomUUID());
        assertEquals(0L, handler.handle(map));
    }

    @Test
    void testValidInputDelegatesToManager() {
        UUID playerUUID = UUID.randomUUID();
        when(manager.getIslandLevel(any(World.class), any(UUID.class))).thenReturn(42L);

        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");
        map.put("player", playerUUID);

        assertEquals(42L, handler.handle(map));
    }

    @Test
    void testHandlerLabel() {
        assertEquals("island-level", handler.getLabel());
    }
}
