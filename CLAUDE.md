# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Level** is a BentoBox add-on for Minecraft that calculates island levels based on block types and counts, maintains top-ten leaderboards, and provides competitive metrics for players on game modes like BSkyBlock and AcidIsland.

## Build & Test Commands

```bash
# Build
mvn clean package

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=LevelTest

# Run a specific test method
mvn test -Dtest=LevelTest#testMethodName

# Full build with coverage
mvn verify
```

Java 21 is required. The build produces a shaded JAR (includes PanelUtils).

## Architecture

### Entry Points
- `LevelPladdon` — Bukkit plugin entry point; instantiates `Level` via the `Pladdon` interface
- `Level` — main addon class; loads config, registers commands/listeners/placeholders, and hooks into optional third-party plugins

### Lifecycle
`onLoad()` → `onEnable()` → `allLoaded()`

`allLoaded()` is where integrations with other BentoBox add-ons (Warps, Visit) are established, since those may not be loaded yet during `onEnable()`.

### Key Classes

| Class | Role |
|---|---|
| `LevelsManager` | Central manager: island level cache, top-ten lists, database reads/writes |
| `Pipeliner` | Async queue; limits concurrent island calculations (configurable) |
| `IslandLevelCalculator` | Core chunk-scanning algorithm; supports multiple block stacker plugins |
| `Results` | Data object returned by a completed calculation |
| `ConfigSettings` | Main config bound to `config.yml` via BentoBox's `@ConfigEntry` annotations |
| `BlockConfig` | Block point-value mappings from `blockconfig.yml` |
| `PlaceholderManager` | Registers PlaceholderAPI placeholders |

### Package Layout
```
world/bentobox/level/
├── calculators/     # IslandLevelCalculator, Pipeliner, Results, EquationEvaluator
├── commands/        # Player and admin sub-commands
├── config/          # ConfigSettings, BlockConfig
├── events/          # IslandPreLevelEvent, IslandLevelCalculatedEvent
├── listeners/       # Island activity, join/leave, migration listeners
├── objects/         # IslandLevels, TopTenData (database-persisted objects)
├── panels/          # GUI panels (top-ten, details, block values)
├── requests/        # API request handlers for inter-addon queries
└── util/            # Utils, ConversationUtils, CachedData
```

### Island Level Calculation Flow
1. A calculation request enters `Pipeliner` (async queue, default concurrency = 1)
2. `IslandLevelCalculator` scans island chunks using chunk snapshots (non-blocking)
3. Block counts are looked up against `BlockConfig` point values
4. An equation (configurable formula) converts total points → island level
5. Results are stored via `LevelsManager` and fired as `IslandLevelCalculatedEvent`

### Optional Plugin Integrations
Level hooks into these plugins when present: WildStacker, RoseStacker, UltimateStacker (block counts), AdvancedChests, ItemsAdder, Oraxen (custom blocks), and the BentoBox Warps/Visit add-ons.

## Testing

Tests live in `src/test/java/world/bentobox/level/`. The framework is JUnit 5 + Mockito 5 + MockBukkit. `CommonTestSetup` is a shared base class that sets up the MockBukkit server and BentoBox mocks — extend it for new test classes.

JaCoCo coverage reports are generated during `mvn verify`.

## Configuration Resources

| File | Location in JAR | Purpose |
|---|---|---|
| `config.yml` | `src/main/resources/` | Main settings (level cost formula, world inclusion, etc.) |
| `blockconfig.yml` | `src/main/resources/` | Points per block type |
| `locales/` | `src/main/resources/locales/` | Translation strings |
| `panels/` | `src/main/resources/panels/` | GUI layout definitions |

## Code Conventions

- Null safety via Eclipse JDT annotations (`@NonNull`, `@Nullable`) — honour these on public APIs
- BentoBox framework patterns: `CompositeCommand` for commands, `@ConfigEntry`/`@ConfigComment` for config, `@StoreAt` for database objects
- Pre- and post-events (`IslandPreLevelEvent`, `IslandLevelCalculatedEvent`) follow BentoBox's cancellable event pattern — fire both when adding new calculation triggers
