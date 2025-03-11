# **Level Plugin Documentation**  

📌 **Repository:** [BentoBoxWorld/Level](https://github.com/BentoBoxWorld/Level)  
📌 **Purpose:** This document provides documentation for all major classes in the Level plugin.  
📌 **Target Audience:** Programmers or maintainers of this code.

## **Table of Contents**
- [Core Classes](#core-classes)
- [Managers](#managers)
- [Calculators](#calculators)
- [Commands](#commands)
- [Events](#events)
- [Panels (GUI)](#panels-gui)
- [Utilities](#utilities)

---

## **1️⃣ Core Classes**  
### **Level**
📍 [`Level.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/Level.java)  
Handles configuration, plugin hooks, and initialization. Level uses the BentoBox Addon API, which follows the Plugin style.  
**Main Methods:**  
- `onLoad()`, `onEnable()`, `allLoaded()`, `getIslandLevel()`, `setIslandLevel()`

`allLoaded()` is a method that BentoBox calls when all the Addons have been loaded, which usually means that all the BentoBox worlds have been loaded. This is important because Level will be referencing worlds.
`getIslandLevel()` is used by the Level addon, but is also used by external Plugins to obtain island levels for players.

### **LevelPladdon**
📍 [`LevelPladdon.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/LevelPladdon.java)  
A wrapper for registering the Level addon. Pladdons are wrappers that make Addons Plugins. This is required because servers like Paper
do byte-code-level conversions when the code is loaded and so the Addons have to be Plugins to benefit from this.  
**Main Method:**  
- `getAddon()` 

---

## **2️⃣ Managers**  
### **LevelsManager**
📍 [`LevelsManager.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/LevelsManager.java)  
Manages island level calculations, leaderboards, and formatting.  
**Main Methods:**  
- `calculateLevel()`, `getIslandLevel()`, `getTopTen()`

### **PlaceholderManager**
📍 [`PlaceholderManager.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/PlaceholderManager.java)  
Handles dynamic placeholders.  
**Main Methods:**  
- `registerPlaceholders()`, `getRankName()`, `getVisitedIslandLevel()`

---

## **3️⃣ Calculators**  
### **EquationEvaluator**
📍 [`EquationEvaluator.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/calculators/EquationEvaluator.java)  
Evaluates mathematical expressions dynamically.  
**Main Method:**  
- `eval(String)`

### **IslandLevelCalculator**
📍 [`IslandLevelCalculator.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/calculators/IslandLevelCalculator.java)  
Computes island levels by scanning blocks and entities.  
**Main Methods:**  
- `scanIsland()`, `calculateLevel()`

### **UltimateStackerCalc**
📍 [`UltimateStackerCalc.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/calculators/UltimateStackerCalc.java)  
Handles calculations for stacked blocks from the plugin UltimateStacker.  
**Main Method:**  
- `addStackers()`

---

## **4️⃣ Commands**  
📌 [Command Handlers Directory](https://github.com/BentoBoxWorld/Level/tree/develop/src/main/java/world/bentobox/level/commands)  

- **Admin Commands**
  - [`AdminLevelStatusCommand.java`](#) → Displays islands in calculation queue.
  - [`AdminSetInitialLevelCommand.java`](#) → Sets initial island level.
  
- **Player Commands**
  - [`IslandLevelCommand.java`](#) → Triggers level calculation.
  - [`IslandTopCommand.java`](#) → Shows top island levels.

---

## **5️⃣ Events**  
### **IslandActivitiesListeners**
📍 [`IslandActivitiesListeners.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/listeners/IslandActivitiesListeners.java)  
Handles **island creation, deletion, and ownership changes**.  
**Main Events:**  
- `onNewIsland()`, `onIslandDelete()`, `onNewIslandOwner()`

---

## **6️⃣ Panels (GUI)**  
### **TopLevelPanel**
📍 [`TopLevelPanel.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/panels/TopLevelPanel.java)  
Displays the **top 10 ranked islands** in a GUI.  
**Main Methods:**  
- `build()`, `createPlayerButton()`, `openPanel()`

### **ValuePanel**
📍 [`ValuePanel.java`](https://github.com/BentoBoxWorld/Level/blob/develop/src/main/java/world/bentobox/level/panels/ValuePanel.java)  
Displays **block values** for island levels.  
**Main Methods:**  
- `build()`, `createMaterialButton()`, `openPanel()`

---

## **7️⃣ Utilities**  
📌 [Utility Classes Directory](https://github.com/BentoBoxWorld/Level/tree/develop/src/main/java/world/bentobox/level/util)  

- **Utils.java** → General helper methods.
- **ConversationUtils.java** → Handles player text input in conversations.

