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
public class ResultsTest {

    private Results results;

    @BeforeEach
    public void setUp() {
        results = new Results();
    }

    // --- State ---

    @Test
    public void testDefaultStateIsAvailable() {
        assertEquals(Result.AVAILABLE, results.getState());
    }

    @Test
    public void testConstructorWithInProgress() {
        Results r = new Results(Result.IN_PROGRESS);
        assertEquals(Result.IN_PROGRESS, r.getState());
    }

    @Test
    public void testConstructorWithTimeout() {
        Results r = new Results(Result.TIMEOUT);
        assertEquals(Result.TIMEOUT, r.getState());
    }

    // --- Level ---

    @Test
    public void testSetAndGetLevel() {
        results.setLevel(42L);
        assertEquals(42L, results.getLevel());
    }

    @Test
    public void testDefaultLevelIsZero() {
        assertEquals(0L, results.getLevel());
    }

    // --- Points to next level ---

    @Test
    public void testSetAndGetPointsToNextLevel() {
        results.setPointsToNextLevel(100L);
        assertEquals(100L, results.getPointsToNextLevel());
    }

    @Test
    public void testDefaultPointsToNextLevelIsZero() {
        assertEquals(0L, results.getPointsToNextLevel());
    }

    // --- Total points ---

    @Test
    public void testSetAndGetTotalPoints() {
        results.setTotalPoints(500L);
        assertEquals(500L, results.getTotalPoints());
    }

    @Test
    public void testDefaultTotalPointsIsZero() {
        assertEquals(0L, results.getTotalPoints());
    }

    // --- Death handicap ---

    @Test
    public void testSetAndGetDeathHandicap() {
        results.setDeathHandicap(3);
        assertEquals(3, results.getDeathHandicap());
    }

    @Test
    public void testDefaultDeathHandicapIsZero() {
        assertEquals(0, results.getDeathHandicap());
    }

    // --- Donated points ---

    @Test
    public void testSetAndGetDonatedPoints() {
        results.setDonatedPoints(250L);
        assertEquals(250L, results.getDonatedPoints());
    }

    @Test
    public void testDefaultDonatedPointsIsZero() {
        assertEquals(0L, results.getDonatedPoints());
    }

    // --- Initial count ---

    @Test
    public void testSetAndGetInitialCount() {
        results.setInitialCount(999L);
        assertEquals(999L, results.getInitialCount());
    }

    @Test
    public void testDefaultInitialCountIsZero() {
        assertEquals(0L, results.getInitialCount());
    }

    // --- Report ---

    @Test
    public void testReportIsNullByDefault() {
        assertNull(results.getReport());
    }

    // --- Multisets ---

    @Test
    public void testMdCountStartsEmpty() {
        assertNotNull(results.getMdCount());
        assertTrue(results.getMdCount().isEmpty());
    }

    @Test
    public void testUwCountStartsEmpty() {
        assertNotNull(results.getUwCount());
        assertTrue(results.getUwCount().isEmpty());
    }

    @Test
    public void testMdCountCanAddElements() {
        results.getMdCount().add(Material.STONE, 5);
        assertEquals(5, results.getMdCount().count(Material.STONE));
    }

    @Test
    public void testUwCountCanAddElements() {
        results.getUwCount().add(Material.SAND, 3);
        assertEquals(3, results.getUwCount().count(Material.SAND));
    }

    // --- Result enum ---

    @Test
    public void testResultEnumValues() {
        assertEquals(3, Result.values().length);
        assertEquals(Result.IN_PROGRESS, Result.valueOf("IN_PROGRESS"));
        assertEquals(Result.AVAILABLE, Result.valueOf("AVAILABLE"));
        assertEquals(Result.TIMEOUT, Result.valueOf("TIMEOUT"));
    }
}
