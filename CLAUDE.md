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

**Panel template upgrades:** Files under `panels/` are copied to the addon's data folder (`plugins/BentoBox/addons/Level/panels/`) on first run and are **not** overwritten on upgrade. If a release modifies a panel template (new tabs, buttons, slots, etc.), the release notes must instruct users to delete the affected on-disk panel file so it regenerates — otherwise existing servers will silently keep the old layout.

## Code Conventions

- Null safety via Eclipse JDT annotations (`@NonNull`, `@Nullable`) — honour these on public APIs
- BentoBox framework patterns: `CompositeCommand` for commands, `@ConfigEntry`/`@ConfigComment` for config, `@StoreAt` for database objects
- Pre- and post-events (`IslandPreLevelEvent`, `IslandLevelCalculatedEvent`) follow BentoBox's cancellable event pattern — fire both when adding new calculation triggers

## Dependency Source Lookup

When you need to inspect source code for a dependency (e.g., BentoBox, addons):

1. **Check local Maven repo first**: `~/.m2/repository/` — sources jars are named `*-sources.jar`
2. **Check the workspace**: Look for sibling directories or Git submodules that may contain the dependency as a local project (e.g., `../bentoBox`, `../addon-*`)
3. **Check Maven local cache for already-extracted sources** before downloading anything
4. Only download a jar or fetch from the internet if the above steps yield nothing useful

Prefer reading `.java` source files directly from a local Git clone over decompiling or extracting a jar.

In general, the latest version of BentoBox should be targeted.

## Project Layout

Related projects are checked out as siblings under `~/git/`:

**Core:**
- `bentobox/` — core BentoBox framework

**Game modes:**
- `addon-acidisland/` — AcidIsland game mode
- `addon-bskyblock/` — BSkyBlock game mode
- `Boxed/` — Boxed game mode (expandable box area)
- `CaveBlock/` — CaveBlock game mode
- `OneBlock/` — AOneBlock game mode
- `SkyGrid/` — SkyGrid game mode
- `RaftMode/` — Raft survival game mode
- `StrangerRealms/` — StrangerRealms game mode
- `Brix/` — plot game mode
- `parkour/` — Parkour game mode
- `poseidon/` — Poseidon game mode
- `gg/` — gg game mode

**Addons:**
- `addon-level/` — island level calculation
- `addon-challenges/` — challenges system
- `addon-welcomewarpsigns/` — warp signs
- `addon-limits/` — block/entity limits
- `addon-invSwitcher/` / `invSwitcher/` — inventory switcher
- `addon-biomes/` / `Biomes/` — biomes management
- `Bank/` — island bank
- `Border/` — world border for islands
- `Chat/` — island chat
- `CheckMeOut/` — island submission/voting
- `ControlPanel/` — game mode control panel
- `Converter/` — ASkyBlock to BSkyBlock converter
- `DimensionalTrees/` — dimension-specific trees
- `discordwebhook/` — Discord integration
- `Downloads/` — BentoBox downloads site
- `DragonFights/` — per-island ender dragon fights
- `ExtraMobs/` — additional mob spawning rules
- `FarmersDance/` — twerking crop growth
- `GravityFlux/` — gravity addon
- `Greenhouses-addon/` — greenhouse biomes
- `IslandFly/` — island flight permission
- `IslandRankup/` — island rankup system
- `Likes/` — island likes/dislikes
- `Limits/` — block/entity limits
- `lost-sheep/` — lost sheep adventure
- `MagicCobblestoneGenerator/` — custom cobblestone generator
- `PortalStart/` — portal-based island start
- `pp/` — pp addon
- `Regionerator/` — region management
- `Residence/` — residence addon
- `TopBlock/` — top ten for OneBlock
- `TwerkingForTrees/` — twerking tree growth
- `Upgrades/` — island upgrades (Vault)
- `Visit/` — island visiting
- `weblink/` — web link addon
- `CrowdBound/` — CrowdBound addon

**Data packs:**
- `BoxedDataPack/` — advancement datapack for Boxed

**Documentation & tools:**
- `docs/` — main documentation site
- `docs-chinese/` — Chinese documentation
- `docs-french/` — French documentation
- `BentoBoxWorld.github.io/` — GitHub Pages site
- `website/` — website
- `translation-tool/` — translation tool

Check these for source before any network fetch.

## Key Dependencies (source locations)

- `world.bentobox:bentobox` → `~/git/bentobox/src/`
