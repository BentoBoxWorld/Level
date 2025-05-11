# 🌟 Level Add-on for BentoBox
[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/Level)](https://ci.codemc.org/job/BentoBoxWorld/job/Level/)[
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Level&metric=bugs)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Level)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Level&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Level)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Level&metric=ncloc)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Level)

## 🔍 What is Level?

**Level** is the ultimate competition booster for your BentoBox server! Designed for game modes like **BSkyBlock**, **AcidIsland**, and more, this powerful add-on turns your island into a battleground of **block-by-block domination**.

Every block you place counts toward your **island level**—and every block you lose could cost you your spot in the rankings. Whether you're aiming for the top ten or just flexing your creative builds, **Level** adds stakes, strategy, and excitement to your sky-high journey.

📘 [Full Documentation](https://docs.bentobox.world/en/latest/addons/Level/)  
📦 [Official Downloads](https://download.bentobox.world)

---

## 🚀 Getting Started

Ready to level up? Here's how to launch **Level** on your server:

1. Drop the **Level** add-on `.jar` into your BentoBox `addons` folder.
2. Restart your server and let the magic happen.
3. A new `Level` data folder and `config.yml` will be created.
4. Open `config.yml` and customize block values, settings, and behavior to suit your game mode.
5. Restart your server again to apply changes.

Now you’re all set—go build something worth leveling for! 🏗️

---

## 🔄 Upgrading

When updating, always read the **release notes**!  
Some updates might require a fresh `config.yml`, so make backups and review changes carefully.

---

## 🛡️ Permissions

**Level** integrates directly with your permissions plugin, giving players the tools to compete while letting admins keep control.

Default permissions for **BSkyBlock**, **AcidIsland**, and **CaveBlock**:

```
permissions:    
  bskyblock.intopten:             # Show up in top 10
    default: true
  bskyblock.island.level:         # Use /is level
    default: true
  bskyblock.island.top:           # Use /is top
    default: true
  bskyblock.island.value:         # Use /is value
    default: true
  bskyblock.admin.level:          # Admin access to /is level
    default: true
  bskyblock.admin.topten:         # Admin access to /is topten
    default: true
```

⚠️ Players need `intopten` to appear in the leaderboard!

---

## ⚙️ Configuration: Make It Yours

The `config.yml` file gives you total control over how leveling works. Here's a breakdown of what you can tweak:

### 🎮 Game Mode Hook
Tell Level which BentoBox game modes it should connect to.

### ⚙️ General Settings
- **Underwater Block Multiplier** – Give bonus points for blocks below sea level.
- **Level Cost** – Set how many points are needed to gain 1 island level.
- **Level Wait** – Add a cooldown between level scans.
- **Death Penalty** – Punish deaths with level loss.
- **Sum Team Deaths** – Choose whether to track team deaths or just the leader's.
- **Reset on Island Reset / Team Join** – Wipe the death count when teams change or islands are reset.

### 🚫 Block Limits
Cap the number of specific blocks that count toward level (e.g., only 200 DIAMOND_BLOCKs count).

Format:
```
DIAMOND_BLOCK: 200
```

### 💎 Block Values
Assign point values to blocks to reward rare or hard-to-get materials.

Format:
```
STONE: 1
DIAMOND_BLOCK: 100
```

Blocks not listed are worth **0**. `AIR` is always ignored.

### 🌍 World-Specific Values
Customize block values for individual worlds or game modes.

Example:
```yaml
worlds:
  AcidIsland_world:
    SAND: 0
    SANDSTONE: 0
    ICE: 0
```

In this setup, **AcidIsland** disables points for sand-based blocks while using default values for everything else.

---

## 🏁 Final Words

**Level** isn’t just a numbers game—it’s a **challenge**, a **competition**, and a **celebration** of creativity.  
Whether you're climbing the ranks or just making your mark, Level brings out the best in your builds.

💡 Need help or want to contribute? Join the community at [bentobox.world](https://bentobox.world) and show us what your island is made of!

Now go get that top spot. 🌌  
— The BentoBox Team
