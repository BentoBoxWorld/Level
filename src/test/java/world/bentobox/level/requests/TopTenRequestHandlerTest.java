package world.bentobox.level.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;

/**
 * Tests for {@link TopTenRequestHandler}
 */
class TopTenRequestHandlerTest extends CommonTestSetup {

    @Mock
    private LevelsManager manager;

    private TopTenRequestHandler handler;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        when(addon.getManager()).thenReturn(manager);
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(world);
        handler = new TopTenRequestHandler(addon);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testNullMapReturnsEmptyMap() {
        Object result = handler.handle(null);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void testEmptyMapReturnsEmptyMap() {
        Object result = handler.handle(Collections.emptyMap());
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void testMissingWorldNameReturnsEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Object result = handler.handle(map);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void testWorldNameWrongTypeReturnsEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", 123);
        Object result = handler.handle(map);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void testWorldNotFoundReturnsEmptyMap() {
        mockedBukkit.when(() -> org.bukkit.Bukkit.getWorld(any(String.class))).thenReturn(null);
        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "nonexistent_world");
        Object result = handler.handle(map);
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void testValidInputDelegatesToManager() {
        LinkedHashMap<String, Long> topTen = new LinkedHashMap<>();
        topTen.put("island-uuid-1", 500L);
        topTen.put("island-uuid-2", 400L);
        when(manager.getTopTen(any(World.class), anyInt())).thenReturn(topTen);

        Map<String, Object> map = new HashMap<>();
        map.put("world-name", "BSkyBlock_world");

        Object result = handler.handle(map);
        assertEquals(topTen, result);
    }

    @Test
    void testHandlerLabel() {
        assertEquals("top-ten-level", handler.getLabel());
    }
}
