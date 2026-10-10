# Graph Report - pw_wind_n_sails  (2026-10-09)

## Corpus Check
- 103 files · ~48,679 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 8 file(s) not represented in the graph (top: (none) 2, .properties 2, .jar 2)

## Summary
- 959 nodes · 2739 edges · 37 communities (14 shown, 23 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 166 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b6d4525d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- FlagpoleBlockEntityRenderer.java
- net.minecraft.world.phys.Vec3
- org.junit.jupiter.api.Test
- FlagpoleBlock.java
- WindAndSailsClient.java
- WindSyncPayload
- ParticleMixin.java
- WindState
- WindAndSailsMod.java
- SailboatEntity
- .tick
- SailboatEntity.java
- FlagpoleItem.java
- BuoyMapTarget
- BuoyEntity
- curseforge.json
- curseforge-upload.js
- XaeroWorldMapCompat.java
- net.minecraft.world.level.Level
- SportSailboatEntity
- .extractCleatStates
- BuoyItem.java
- WindDayType
- Mod Icon Texture
- Flagpole Block Texture
- Flag Entity Texture
- Anchor Item Icon
- Flagpole Item Icon
- Sailboat Item Icon
- Sport Sailboat Item Icon

## God Nodes (most connected - your core abstractions)
1. `SailboatEntity` - 155 edges
2. `BuoyEntity` - 46 edges
3. `WindAndSailsMod` - 45 edges
4. `WindVector` - 41 edges
5. `WindState` - 40 edges
6. `BuoyMapTarget` - 33 edges
7. `AnchorEntity` - 33 edges
8. `WindManager` - 28 edges
9. `Reader` - 26 edges
10. `Context` - 25 edges

## Surprising Connections (you probably didn't know these)
- `AnchorRenderer` --references--> `AnchorEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/AnchorRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/AnchorEntity.java
- `BuoyRenderer` --references--> `BuoyEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/BuoyRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/BuoyEntity.java
- `FlagpoleBlockEntityRenderer` --references--> `FlagpoleBlockEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/FlagpoleBlockEntityRenderer.java → src/main/java/com/piotrek/pwwindnsails/block/entity/FlagpoleBlockEntity.java
- `SailboatRenderer` --references--> `SailboatEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/SailboatRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/SailboatEntity.java
- `SportSailboatRenderer` --references--> `SportSailboatEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/SportSailboatRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/SportSailboatEntity.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Sailing Dynamics & Physics Engine** — readme_apparent_wind, readme_points_of_sail, readme_hydrodynamics_keel, src_main_java_com_piotrek_pwwindnsails_physics_sailingphysics [INFERRED 0.85]

## Communities (37 total, 23 thin omitted)

### Community 0 - "FlagpoleBlockEntityRenderer.java"
Cohesion: 0.05
Nodes (14): AnchorModel, AnchorRenderer, AnchorRenderState, BuoyModel, BuoyRenderer, BuoyRenderState, FlagModel, FlagpoleBlockEntityRenderer (+6 more)

### Community 1 - "net.minecraft.world.phys.Vec3"
Cohesion: 0.07
Nodes (6): CleatHighlightRenderer, PlaneCompatHelper, MooringAim, Target, HeldCleat, ChunkReady

### Community 2 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (12): Apparent Wind Calculation, Hydrodynamics & Keel Leeway Damping, Points of Sail Mechanics, TreeWindAmbienceHandler, PhysicsResult, SailingPhysics, WindVector, BuoyPhysicsTest (+4 more)

### Community 4 - "WindAndSailsClient.java"
Cohesion: 0.05
Nodes (10): WindAndSailsClient, WindHudOverlay, VisualizerMode, BRIGHT_CYAN, OFF, SEAFOAM_WHITE, SOFT_SKY, WARM_AMBER (+2 more)

### Community 5 - "WindSyncPayload"
Cohesion: 0.20
Nodes (3): MooringClickPayload, SailboatInputPayload, WindSyncPayload

### Community 7 - "ParticleMixin.java"
Cohesion: 0.13
Nodes (3): AvatarRendererMixin, ParticleMixin, AbstractBoatMixin

### Community 8 - "WindState"
Cohesion: 0.05
Nodes (7): Harmonized Wind System & Gusts, WindAndSailsCommands, DailyWindProfile, WindGust, WindManager, WindState, WindTest

### Community 9 - "WindAndSailsMod.java"
Cohesion: 0.08
Nodes (4): Release Changelog, Wind & Sails Mod Documentation, SailboatItem, WindAndSailsMod

### Community 12 - "SailboatEntity.java"
Cohesion: 0.06
Nodes (9): Cleat Mooring & Anchor Mechanics, Gunwale Hiking & Righting Moment, Sailing Telemetry HUD Overlay, On-Water Wind Visualizer (TAB), SailboatHudOverlay, MooringConstraint, WindAndSailsConfig, Anchor Entity Texture (+1 more)

### Community 15 - "BuoyMapTarget"
Cohesion: 0.10
Nodes (6): BuoyMapTarget, BuoyTracker, BuoyWorldMapRenderer, Context, Provider, Reader

### Community 17 - "curseforge.json"
Cohesion: 0.14
Nodes (13): changelog, changelogType, dependencies, description, enabled, manualRelease, media, gallery (+5 more)

### Community 18 - "curseforge-upload.js"
Cohesion: 0.19
Nodes (8): { execSync }, fetchCurseForgeVersions(), fetchProjectFiles(), fs, https, main(), path, uploadFileToCurseForge()

### Community 29 - "BuoyItem.java"
Cohesion: 0.13
Nodes (3): AnchorItem, BuoyItem, SportSailboatItem

### Community 32 - "WindDayType"
Cohesion: 0.25
Nodes (6): WindDayType, CALM, GUSTY_WIND, LIGHT_BREEZE, MODERATE_BREEZE, STORMY_GALE

## Knowledge Gaps
- **46 isolated node(s):** `projectId`, `title`, `summary`, `description`, `enabled` (+41 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 180 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SailboatEntity` connect `SailboatEntity` to `FlagpoleBlockEntityRenderer.java`, `net.minecraft.world.phys.Vec3`, `org.junit.jupiter.api.Test`, `WindAndSailsClient.java`, `Override`, `ParticleMixin.java`, `WindAndSailsMod.java`, `.tick`, `SailboatEntity.java`, `FlagpoleItem.java`, `.getCleatHolderEntity`, `net.minecraft.world.level.Level`, `SportSailboatEntity`, `.extractCleatStates`, `.isHullColliding`, `BuoyItem.java`?**
  _High betweenness centrality (0.232) - this node is a cross-community bridge._
- **What connects `projectId`, `title`, `summary` to the rest of the system?**
  _46 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FlagpoleBlockEntityRenderer.java` be split into smaller, more focused modules?**
  _Cohesion score 0.05133599831685252 - nodes in this community are weakly interconnected._
- **Why does `WindAndSailsMod` connect `WindAndSailsMod.java` to `FlagpoleBlockEntityRenderer.java`, `net.minecraft.world.phys.Vec3`, `FlagpoleBlock.java`, `WindAndSailsClient.java`, `WindSyncPayload`, `SailboatEntity`, `SailboatEntity.java`, `FlagpoleItem.java`, `BuoyMapTarget`, `BuoyEntity`, `XaeroWorldMapCompat.java`, `net.minecraft.world.level.Level`, `SportSailboatEntity`, `BuoyItem.java`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `net.minecraft.world.phys.Vec3` be split into smaller, more focused modules?**
  _Cohesion score 0.0744047619047619 - nodes in this community are weakly interconnected._
- **Why does `BuoyEntity` connect `BuoyEntity` to `FlagpoleBlockEntityRenderer.java`, `net.minecraft.world.phys.Vec3`, `org.junit.jupiter.api.Test`, `Override`, `WindAndSailsMod.java`, `SailboatEntity.java`, `BuoyMapTarget`, `net.minecraft.world.level.Level`, `BuoyItem.java`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `org.junit.jupiter.api.Test` be split into smaller, more focused modules?**
  _Cohesion score 0.05939629990262902 - nodes in this community are weakly interconnected._