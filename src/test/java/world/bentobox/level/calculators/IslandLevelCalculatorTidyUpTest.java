package world.bentobox.level.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.level.CommonTestSetup;
import world.bentobox.level.LevelsManager;
import world.bentobox.level.config.BlockConfig;
import world.bentobox.level.config.ConfigSettings;

/**
 * Pins down the contract of {@link IslandLevelCalculator#tidyUp()} for the
 * "level 0" cases described in PR #434. Each test asserts both
 * {@code pointsFromCurrentLevel} ("progress") and the interval
 * {@code pointsFromCurrentLevel + pointsToNextLevel} ("levelcost" as the
 * player sees it).
 * <p>
 * Drives the actual code path, not a reimplementation, so a failure here
 * means the production code disagrees with the asserted contract.
 */
class IslandLevelCalculatorTidyUpTest extends CommonTestSetup {

    @Mock
    private ConfigSettings settings;
    @Mock
    private LevelsManager manager;
    @Mock
    private BlockConfig blockConfig;
    @Mock
    private Pipeliner pipeliner;

    private static final long INITIAL_COUNT = 130L;
    private static final long LEVEL_COST = 130L;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Settings — linear formula, level 0 ends at initialCount + level_cost.
        when(addon.getSettings()).thenReturn(settings);
        when(settings.getLevelCalc()).thenReturn("blocks / level_cost");
        when(settings.getLevelCost()).thenReturn(LEVEL_COST);
        when(settings.isZeroNewIslandLevels()).thenReturn(true);
        when(settings.isDonationsOnly()).thenReturn(false);
        when(settings.getDeathPenalty()).thenReturn(0);
        when(settings.getUnderWaterMultiplier()).thenReturn(1.0);
        when(settings.isSumTeamDeaths()).thenReturn(false);
        when(settings.isNether()).thenReturn(false);
        when(settings.isEnd()).thenReturn(false);

        when(addon.getManager()).thenReturn(manager);
        when(manager.getDonatedPoints(any(Island.class))).thenReturn(0L);
        when(manager.getDonatedBlocks(any(Island.class))).thenReturn(Collections.emptyMap());
        when(manager.getIslandLevel(any(), any())).thenReturn(0L);
        when(manager.getInitialCount(any(Island.class))).thenReturn(INITIAL_COUNT);

        when(addon.getBlockConfig()).thenReturn(blockConfig);
        when(blockConfig.getValue(any(), any())).thenReturn(0);
        when(blockConfig.getLimit(any())).thenReturn(null);
        when(blockConfig.getBlockValues()).thenReturn(Collections.emptyMap());

        when(addon.getPipeliner()).thenReturn(pipeliner);
        when(addon.getInitialIslandCount(any(Island.class))).thenReturn(INITIAL_COUNT);

        PlayersManager players = mock(PlayersManager.class);
        when(players.getDeaths(any(), any())).thenReturn(0);
        when(addon.getPlayers()).thenReturn(players);

        // Island — tiny protection range keeps the chunks-to-scan queue small
        // (the constructor walks it, but tidyUp() does not).
        when(island.getProtectionRange()).thenReturn(16);
        when(island.getMinProtectedX()).thenReturn(0);
        when(island.getMaxProtectedX()).thenReturn(16);
        when(island.getMinProtectedZ()).thenReturn(0);
        when(island.getMaxProtectedZ()).thenReturn(16);
        when(island.getWorld()).thenReturn(world);

        Location centre = mock(Location.class);
        when(centre.toVector()).thenReturn(new Vector(0, 0, 0));
        when(island.getCenter()).thenReturn(centre);

        // Sea height — not under water, so the multiplier never fires.
        lenient().when(iwm.getSeaHeight(any())).thenReturn(0);
    }

    private IslandLevelCalculator newCalculator() {
        return new IslandLevelCalculator(addon, island, new CompletableFuture<>(), false);
    }

    @Test
    @DisplayName("At start: progress=0, interval=level_cost")
    void atStart() {
        IslandLevelCalculator calc = newCalculator();
        Results r = calc.getResults();
        r.rawBlockCount.set(INITIAL_COUNT); // 130
        calc.tidyUp();

        assertEquals(0L, r.getLevel(), "level");
        assertEquals(0L, r.getPointsFromCurrentLevel(), "progress at start");
        assertEquals(LEVEL_COST, r.getPointsFromCurrentLevel() + r.getPointsToNextLevel(),
                "interval at start");
    }

    @Test
    @DisplayName("Below start: progress goes negative, interval still equals level_cost (PR #434 claim)")
    void belowStart() {
        IslandLevelCalculator calc = newCalculator();
        Results r = calc.getResults();
        r.rawBlockCount.set(INITIAL_COUNT - 8); // 122
        calc.tidyUp();

        assertEquals(0L, r.getLevel(), "level stays at 0 (modifiedPoints<0 truncates)");
        assertEquals(-8L, r.getPointsFromCurrentLevel(),
                "progress should be -8 — the player has lost 8 blocks below the starting count");
        assertEquals(LEVEL_COST, r.getPointsFromCurrentLevel() + r.getPointsToNextLevel(),
                "interval should remain level_cost when below start");
    }

    @Test
    @DisplayName("Above start within level 0: progress=delta, interval=level_cost")
    void aboveStartLevel0() {
        IslandLevelCalculator calc = newCalculator();
        Results r = calc.getResults();
        r.rawBlockCount.set(INITIAL_COUNT + 70); // 200
        calc.tidyUp();

        assertEquals(0L, r.getLevel(), "still level 0 (70/130 truncates)");
        assertEquals(70L, r.getPointsFromCurrentLevel(), "progress = blocks - initialCount");
        assertEquals(LEVEL_COST, r.getPointsFromCurrentLevel() + r.getPointsToNextLevel(),
                "interval");
    }

    @Test
    @DisplayName("Exactly at level 1 boundary: progress=0, interval=level_cost")
    void atLevel1Boundary() {
        IslandLevelCalculator calc = newCalculator();
        Results r = calc.getResults();
        r.rawBlockCount.set(INITIAL_COUNT + LEVEL_COST); // 260 → level=1
        calc.tidyUp();

        assertEquals(1L, r.getLevel(), "level=1");
        assertEquals(0L, r.getPointsFromCurrentLevel(), "just crossed: progress=0");
        assertEquals(LEVEL_COST, r.getPointsFromCurrentLevel() + r.getPointsToNextLevel(),
                "interval");
    }

    @Test
    @DisplayName("Non-linear sqrt formula, below start: progress is negative, interval is the level-0 width")
    void belowStart_sqrtFormula() {
        // Switch to a non-linear formula. With zeroing on, modifiedPoints = blocks - initialCount,
        // and sqrt(negative) = NaN → cast to 0 → level=0. The earlier "Negative values in
        // progression while using a non-linear function" fix (c531317) was for exactly this kind
        // of formula, so PR #434 should presumably keep producing a sensible -8/<interval> here.
        when(settings.getLevelCalc()).thenReturn("sqrt(blocks)");
        IslandLevelCalculator calc = newCalculator();
        Results r = calc.getResults();
        r.rawBlockCount.set(INITIAL_COUNT - 8); // 122

        calc.tidyUp();

        assertEquals(0L, r.getLevel(), "level stays at 0 (sqrt(-8) → NaN → 0)");
        assertEquals(-8L, r.getPointsFromCurrentLevel(),
                "progress should reflect the 8-block deficit from initialCount");
        // sqrt(blocks-130) first crosses 1 at blocks=131, so the level-0 interval here is 1 block wide.
        // The exact interval isn't the point — we just want progress + remaining to be self-consistent
        // and the "remaining to next" not negative.
        long remaining = r.getPointsToNextLevel();
        assertEquals(true, remaining > 0, "pointsToNextLevel should be positive, got " + remaining);
    }
}
