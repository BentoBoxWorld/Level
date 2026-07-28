package world.bentobox.level.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Files;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import world.bentobox.level.CommonTestSetup;

/**
 * Verifies that {@link BlockConfig#getLimit(Object)} lookups are
 * case-insensitive and agree regardless of how the caller identifies the
 * block (Material, exact-case custom ID, or lowercased key) — the level
 * calculation lowercases keys while the donation paths pass IDs as-is, so
 * both must resolve to the same limit.
 */
class BlockConfigLimitsTest extends CommonTestSetup {

    private BlockConfig blockConfig;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        YamlConfiguration config = new YamlConfiguration();
        config.set("limits.COBBLESTONE", 100); // old-style material key
        config.set("limits.MyCrystal", 5); // mixed-case custom block ID
        config.set("limits.oraxen:my_gem", 7); // namespaced custom block ID
        File file = Files.createTempFile("blockconfig", ".yml").toFile();
        file.deleteOnExit();
        blockConfig = new BlockConfig(addon, config, file);
    }

    @Test
    @DisplayName("Material lookup finds a limit declared with an old-style uppercase key")
    void materialLookup() {
        assertEquals(100, blockConfig.getLimit(Material.COBBLESTONE));
    }

    @Test
    @DisplayName("String lookups find limits regardless of case")
    void stringLookupCaseInsensitive() {
        assertEquals(100, blockConfig.getLimit("cobblestone"));
        assertEquals(100, blockConfig.getLimit("COBBLESTONE"));
        assertEquals(5, blockConfig.getLimit("MyCrystal"));
        assertEquals(5, blockConfig.getLimit("mycrystal"));
        assertEquals(7, blockConfig.getLimit("oraxen:my_gem"));
        assertEquals(7, blockConfig.getLimit("ORAXEN:MY_GEM"));
    }

    @Test
    @DisplayName("Unknown keys have no limit")
    void unknownKey() {
        assertNull(blockConfig.getLimit("diamond_block"));
        assertNull(blockConfig.getLimit(Material.DIAMOND_BLOCK));
    }
}
