# Block Donation Feature - Implementation Plan

**Branch:** `feature/block-donation`
**Issue:** https://github.com/BentoBoxWorld/Level/issues/220
**Status:** Implemented - awaiting build verification

## Problem

Island level is calculated purely by scanning physical blocks. If a player removes blocks (to build elsewhere, or cleans up), their level drops. Players need a way to permanently "bank" block value that survives block removal.

## Solution

Players can **donate** blocks from their inventory to permanently raise their island level. Donated blocks are consumed (removed from inventory) and their point value is permanently recorded. This value persists across level recalculations since it's stored separately from scanned blocks.

## Design Decisions

- **GUI-based donation** via `/island donate` - opens a chest-style inventory where players drag blocks in, see a real-time point preview, and confirm/cancel
- **Quick hand donation** via `/island donate hand [amount]` - donates blocks held in hand for power users
- **Flag-controlled** - new `ISLAND_BLOCK_DONATION` protection flag, default rank = OWNER, configurable down to MEMBER
- **Irreversible** - once donated, blocks cannot be retrieved ("no take-backsies")
- **Auditable** - donation log with timestamps, donor UUID, material, count, and point value
- **Backwards compatible** - new fields in `IslandLevels` are null-safe; legacy records without donation fields load cleanly
- **Default enabled** - donations are allowed by default for this release

## Files Created

| File | Purpose |
|------|---------|
| `commands/IslandDonateCommand.java` | Player `/island donate` command with `hand` subcommand |
| `panels/DonationPanel.java` | Chest-style GUI for donating blocks |

## Files Modified

### 1. `IslandLevels.java` - Data Model

Added `@Expose` fields with null-safe getters (backwards compatibility):
- `donatedBlocks: Map<String, Integer>` - Material name to count
- `donatedPoints: Long` - Total point value of all donations
- `donationLog: List<DonationRecord>` - Audit trail
- `DonationRecord` - inner record with timestamp, donorUUID, material, count, points
- `addDonation()` - helper to record a donation

### 2. `LevelsManager.java` - Donation API

Added methods:
- `donateBlocks(island, donorUUID, material, count, points)` - records donation, saves async
- `getDonatedPoints(island)` - returns total donated points
- `getDonatedBlocks(island)` - returns the donated blocks map

### 3. `IslandLevelCalculator.java` - Calculation Integration

In `tidyUp()`, donated points are added to `rawBlockCount` (which is actually a point accumulator) after underwater multiplier and before death penalty.

In `getReport()`, a "Donated blocks" section is appended listing all donated materials with their counts and point values.

### 4. `Level.java` - Flag Registration & Command Registration

- Registered `ISLAND_BLOCK_DONATION` flag (Protection type, default OWNER rank)
- Added `IslandDonateCommand` to player commands

### 5. `DetailsPanel.java` - DONATED Tab

Added `DONATED` to the `Tab` enum. In `updateFilters()`, the new tab reads from `levelsData.getDonatedBlocks()` and converts material names back to Material objects for display.

### 6. `Results.java` - Donated Points Field

Added `donatedPoints: AtomicLong` with getter/setter for inclusion in the report.

### 7. `detail_panel.yml` - Panel Layout

Added DONATED tab button at position 1:7 with HOPPER icon.

### 8. `en-US.yml` - Locale Entries

Added entries for:
- `protection.flags.ISLAND_BLOCK_DONATION.*` - flag name/description/hint
- `island.donate.*` - all command messages, GUI labels
- `level.gui.buttons.donated.*` - tab button in DetailsPanel

## Level Calculation Flow (Updated)

1. `Pipeliner` queues island for calculation
2. `IslandLevelCalculator` scans chunks, counts physical blocks (as point values)
3. In `tidyUp()`:
   - Raw block points computed (physical blocks + underwater multiplier)
   - **Donated points added to raw block points** <- NEW
   - Death penalty applied
   - Formula calculates level from total points
   - Report generated (includes donated blocks section) <- NEW
4. `LevelsManager.setIslandResults()` persists results
   - **Donated blocks/points are NOT overwritten** - they live in separate fields

## GUI Design (DonationPanel)

```
Row 1: [border] [border] [border] [border] [info ] [border] [border] [border] [border]
Row 2: [border] [slot ] [slot ] [slot ] [slot ] [slot ] [slot ] [slot ] [border]
Row 3: [border] [slot ] [slot ] [slot ] [slot ] [slot ] [slot ] [slot ] [border]
Row 4: [border] [cancel] [border] [border] [preview] [border] [border] [border] [confirm]
```

- **Info pane** (slot 4): Shows current total donated points
- **Slots** (14 slots in rows 2-3): Drag blocks from inventory
- **Preview** (slot 31): Live point total of items in slots
- **Cancel** (slot 28, red glass): Returns all items
- **Confirm** (slot 34, green glass): Consumes items, records donation

On GUI close without confirm -> items returned to player (safety).

## Testing Checklist

- [ ] `/island donate` opens GUI
- [ ] Drag blocks into GUI, preview updates
- [ ] Confirm consumes blocks and records points
- [ ] Cancel returns all blocks
- [ ] Close GUI without confirm returns blocks
- [ ] `/island donate hand` donates held blocks
- [ ] `/island donate hand 10` donates specific amount
- [ ] Non-block items rejected
- [ ] Zero-value blocks rejected
- [ ] Flag permission check (owner default, configurable)
- [ ] Must be on own island
- [ ] Donated points appear in level calculation
- [ ] Admin report shows donated blocks section
- [ ] DONATED tab in detail panel shows donated blocks
- [ ] Backwards compatibility: load island with no donation fields
- [ ] Multiple donations accumulate correctly
- [ ] Donation log records all entries with timestamps
