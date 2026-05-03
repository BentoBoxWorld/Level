package world.bentobox.level.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Tests for {@link IslandLevels}
 */
class IslandLevelsTest {

    private static final String ISLAND_ID = "test-island-uuid";
    private IslandLevels islandLevels;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        islandLevels = new IslandLevels(ISLAND_ID);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- UniqueId ---

    @Test
    void testGetUniqueId() {
        assertEquals(ISLAND_ID, islandLevels.getUniqueId());
    }

    @Test
    void testSetUniqueId() {
        islandLevels.setUniqueId("new-id");
        assertEquals("new-id", islandLevels.getUniqueId());
    }

    // --- Level with max tracking ---

    @Test
    void testSetLevelUpdatesMaxLevel() {
        islandLevels.setLevel(5L);
        assertEquals(5L, islandLevels.getLevel());
        assertEquals(5L, islandLevels.getMaxLevel());
    }

    @Test
    void testMaxLevelDoesNotDecrease() {
        islandLevels.setLevel(10L);
        islandLevels.setLevel(3L);
        assertEquals(3L, islandLevels.getLevel());
        assertEquals(10L, islandLevels.getMaxLevel());
    }

    @Test
    void testMaxLevelUpdatesWhenHigher() {
        islandLevels.setLevel(5L);
        islandLevels.setLevel(8L);
        assertEquals(8L, islandLevels.getMaxLevel());
    }

    @Test
    void testDefaultLevelIsZero() {
        assertEquals(0L, islandLevels.getLevel());
        assertEquals(0L, islandLevels.getMaxLevel());
    }

    // --- Points to next level ---

    @Test
    void testPointsToNextLevel() {
        islandLevels.setPointsToNextLevel(150L);
        assertEquals(150L, islandLevels.getPointsToNextLevel());
    }

    // --- Total points ---

    @Test
    void testTotalPoints() {
        islandLevels.setTotalPoints(9999L);
        assertEquals(9999L, islandLevels.getTotalPoints());
    }

    // --- Initial count ---

    @Test
    void testInitialCount() {
        islandLevels.setInitialCount(500L);
        assertEquals(500L, islandLevels.getInitialCount());
    }

    @Test
    void testInitialCountDefaultNull() {
        // New island has null initial count (not yet set)
        assertEquals(null, islandLevels.getInitialCount());
    }

    // --- convertMap via getMdCount / getUwCount ---

    @Test
    void testGetMdCountConvertsStringToMaterial() {
        Map<Object, Integer> map = new HashMap<>();
        map.put("STONE", 10);
        islandLevels.setMdCount(map);

        Map<Object, Integer> result = islandLevels.getMdCount();
        assertTrue(result.containsKey(Material.STONE), "String 'STONE' should be converted to Material.STONE");
        assertEquals(10, result.get(Material.STONE));
    }

    @Test
    void testGetMdCountConvertsStringToEntityType() {
        Map<Object, Integer> map = new HashMap<>();
        map.put("ZOMBIE", 5);
        islandLevels.setMdCount(map);

        Map<Object, Integer> result = islandLevels.getMdCount();
        assertTrue(result.containsKey(EntityType.ZOMBIE), "String 'ZOMBIE' should be converted to EntityType.ZOMBIE");
        assertEquals(5, result.get(EntityType.ZOMBIE));
    }

    @Test
    void testGetMdCountKeepsUnknownStringAsIs() {
        Map<Object, Integer> map = new HashMap<>();
        map.put("unknown_xyz_block", 3);
        islandLevels.setMdCount(map);

        Map<Object, Integer> result = islandLevels.getMdCount();
        assertTrue(result.containsKey("unknown_xyz_block"), "Unknown string key should pass through unchanged");
        assertEquals(3, result.get("unknown_xyz_block"));
    }

    @Test
    void testGetMdCountKeepsNonStringKeyAsIs() {
        Map<Object, Integer> map = new HashMap<>();
        map.put(Material.DIRT, 7);
        islandLevels.setMdCount(map);

        Map<Object, Integer> result = islandLevels.getMdCount();
        assertTrue(result.containsKey(Material.DIRT));
        assertEquals(7, result.get(Material.DIRT));
    }

    @Test
    void testGetUwCountConvertsStringToMaterial() {
        Map<Object, Integer> map = new HashMap<>();
        map.put("SAND", 4);
        islandLevels.setUwCount(map);

        Map<Object, Integer> result = islandLevels.getUwCount();
        assertTrue(result.containsKey(Material.SAND));
        assertEquals(4, result.get(Material.SAND));
    }

    // --- Donation API ---

    @Test
    void testGetDonatedBlocksReturnsEmptyMapByDefault() {
        assertNotNull(islandLevels.getDonatedBlocks());
        assertTrue(islandLevels.getDonatedBlocks().isEmpty());
    }

    @Test
    void testGetDonationLogReturnsEmptyListByDefault() {
        assertNotNull(islandLevels.getDonationLog());
        assertTrue(islandLevels.getDonationLog().isEmpty());
    }

    @Test
    void testGetDonatedPointsReturnsZeroByDefault() {
        assertEquals(0L, islandLevels.getDonatedPoints());
    }

    @Test
    void testAddDonationAccumulatesBlocks() {
        islandLevels.addDonation("donor-uuid", "STONE", 5, 25L);
        assertEquals(5, islandLevels.getDonatedBlocks().get("STONE"));
    }

    @Test
    void testAddDonationAccumulatesPoints() {
        islandLevels.addDonation("donor-uuid", "STONE", 5, 25L);
        assertEquals(25L, islandLevels.getDonatedPoints());
    }

    @Test
    void testAddDonationAppendsToLog() {
        islandLevels.addDonation("donor-uuid", "STONE", 5, 25L);
        assertEquals(1, islandLevels.getDonationLog().size());
        IslandLevels.DonationRecord record = islandLevels.getDonationLog().get(0);
        assertEquals("donor-uuid", record.donorUUID());
        assertEquals("STONE", record.material());
        assertEquals(5, record.count());
        assertEquals(25L, record.points());
    }

    @Test
    void testAddDonationMultipleCallsMergeCounts() {
        islandLevels.addDonation("donor-uuid", "STONE", 3, 15L);
        islandLevels.addDonation("donor-uuid", "STONE", 7, 35L);
        assertEquals(10, islandLevels.getDonatedBlocks().get("STONE"));
        assertEquals(50L, islandLevels.getDonatedPoints());
        assertEquals(2, islandLevels.getDonationLog().size());
    }

    @Test
    void testAddDonationDifferentMaterials() {
        islandLevels.addDonation("donor-uuid", "STONE", 3, 15L);
        islandLevels.addDonation("donor-uuid", "DIRT", 2, 4L);
        assertEquals(3, islandLevels.getDonatedBlocks().get("STONE"));
        assertEquals(2, islandLevels.getDonatedBlocks().get("DIRT"));
        assertEquals(19L, islandLevels.getDonatedPoints());
    }

    // --- Deprecated initialLevel backwards-compat ---

    @Test
    void testDeprecatedInitialLevel() {
        islandLevels.setInitialLevel(100L);
        assertEquals(100L, islandLevels.getInitialLevel());
    }
}
