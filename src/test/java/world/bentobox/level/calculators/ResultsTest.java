package world.bentobox.level.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.level.calculators.Results.Result;

/**
 * Tests for {@link Results}
 */
class ResultsTest {

    private Results results;

    @BeforeEach
    void setUp() {
        results = new Results();
    }

    // --- State ---

    @Test
    void testDefaultStateIsAvailable() {
        assertEquals(Result.AVAILABLE, results.getState());
    }

    @Test
    void testConstructorWithInProgress() {
        Results r = new Results(Result.IN_PROGRESS);
        assertEquals(Result.IN_PROGRESS, r.getState());
    }

    @Test
    void testConstructorWithTimeout() {
        Results r = new Results(Result.TIMEOUT);
        assertEquals(Result.TIMEOUT, r.getState());
    }

    // --- Level ---

    @Test
    void testSetAndGetLevel() {
        results.setLevel(42L);
        assertEquals(42L, results.getLevel());
    }

    @Test
    void testDefaultLevelIsZero() {
        assertEquals(0L, results.getLevel());
    }

    // --- Points to next level ---

    @Test
    void testSetAndGetPointsToNextLevel() {
        results.setPointsToNextLevel(100L);
        assertEquals(100L, results.getPointsToNextLevel());
    }

    @Test
    void testDefaultPointsToNextLevelIsZero() {
        assertEquals(0L, results.getPointsToNextLevel());
    }

    // --- Total points ---

    @Test
    void testSetAndGetTotalPoints() {
        results.setTotalPoints(500L);
        assertEquals(500L, results.getTotalPoints());
    }

    @Test
    void testDefaultTotalPointsIsZero() {
        assertEquals(0L, results.getTotalPoints());
    }

    // --- Death handicap ---

    @Test
    void testSetAndGetDeathHandicap() {
        results.setDeathHandicap(3);
        assertEquals(3, results.getDeathHandicap());
    }

    @Test
    void testDefaultDeathHandicapIsZero() {
        assertEquals(0, results.getDeathHandicap());
    }

    // --- Points from current level ---

    @Test
    void testSetAndGetPointsFromCurrentLevel() {
        results.setPointsFromCurrentLevel(75L);
        assertEquals(75L, results.getPointsFromCurrentLevel());
    }

    @Test
    void testDefaultPointsFromCurrentLevelIsZero() {
        assertEquals(0L, results.getPointsFromCurrentLevel());
    }

    // --- Donated points ---

    @Test
    void testSetAndGetDonatedPoints() {
        results.setDonatedPoints(250L);
        assertEquals(250L, results.getDonatedPoints());
    }

    @Test
    void testDefaultDonatedPointsIsZero() {
        assertEquals(0L, results.getDonatedPoints());
    }

    // --- Initial count ---

    @Test
    void testSetAndGetInitialCount() {
        results.setInitialCount(999L);
        assertEquals(999L, results.getInitialCount());
    }

    @Test
    void testDefaultInitialCountIsZero() {
        assertEquals(0L, results.getInitialCount());
    }

    // --- Report ---

    @Test
    void testReportIsNullByDefault() {
        assertNull(results.getReport());
    }

    // --- Multisets ---

    @Test
    void testMdCountStartsEmpty() {
        assertNotNull(results.getMdCount());
        assertTrue(results.getMdCount().isEmpty());
    }

    @Test
    void testUwCountStartsEmpty() {
        assertNotNull(results.getUwCount());
        assertTrue(results.getUwCount().isEmpty());
    }

    @Test
    void testMdCountCanAddElements() {
        results.getMdCount().add(Material.STONE, 5);
        assertEquals(5, results.getMdCount().count(Material.STONE));
    }

    @Test
    void testUwCountCanAddElements() {
        results.getUwCount().add(Material.SAND, 3);
        assertEquals(3, results.getUwCount().count(Material.SAND));
    }

    // --- Result enum ---

    @Test
    void testResultEnumValues() {
        assertEquals(3, Result.values().length);
        assertEquals(Result.IN_PROGRESS, Result.valueOf("IN_PROGRESS"));
        assertEquals(Result.AVAILABLE, Result.valueOf("AVAILABLE"));
        assertEquals(Result.TIMEOUT, Result.valueOf("TIMEOUT"));
    }
}
