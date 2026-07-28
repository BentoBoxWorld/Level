package world.bentobox.level.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import world.bentobox.level.config.BlockConfig;

/**
 * Unit tests for {@link Utils#calculateDonatedPoints(BlockConfig, World, Map)} —
 * the single source of truth for how donated blocks contribute to the level.
 */
class UtilsCalculateDonatedPointsTest {

    private BlockConfig blockConfig;
    private World world;

    @BeforeEach
    void setUp() {
        blockConfig = mock(BlockConfig.class);
        world = mock(World.class);
        // Mockito returns 0 (not null) for unstubbed Integer-returning methods, which would
        // read as "limit of 0" — the real BlockConfig returns null when no limit is configured.
        when(blockConfig.getLimit(any())).thenReturn(null);
    }

    @Test
    @DisplayName("Uses current block values and lowercases the donation key")
    void usesCurrentValues() {
        when(blockConfig.getValue(any(), eq("iron_block"))).thenReturn(3);
        assertEquals(1500L, Utils.calculateDonatedPoints(blockConfig, world, Map.of("IRON_BLOCK", 500)));
    }

    @Test
    @DisplayName("Caps each block type at its current limit")
    void capsAtLimit() {
        when(blockConfig.getValue(any(), eq("iron_block"))).thenReturn(3);
        when(blockConfig.getLimit("iron_block")).thenReturn(200);
        assertEquals(600L, Utils.calculateDonatedPoints(blockConfig, world, Map.of("IRON_BLOCK", 500)));
    }

    @Test
    @DisplayName("Blocks with no configured value contribute nothing")
    void noValueMeansZero() {
        when(blockConfig.getValue(any(), anyString())).thenReturn(null);
        assertEquals(0L, Utils.calculateDonatedPoints(blockConfig, world, Map.of("IRON_BLOCK", 500)));
    }

    @Test
    @DisplayName("Sums across multiple donated block types")
    void sumsAcrossTypes() {
        when(blockConfig.getValue(any(), eq("iron_block"))).thenReturn(3);
        when(blockConfig.getValue(any(), eq("oraxen:my_gem"))).thenReturn(10);
        when(blockConfig.getLimit("oraxen:my_gem")).thenReturn(2);
        // 100 * 3 + min(5, 2) * 10 = 320
        assertEquals(320L, Utils.calculateDonatedPoints(blockConfig, world,
                Map.of("IRON_BLOCK", 100, "oraxen:my_gem", 5)));
    }
}
