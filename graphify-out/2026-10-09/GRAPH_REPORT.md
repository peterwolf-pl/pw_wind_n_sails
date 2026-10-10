# Graph Report - pw_wind_n_sails  (2026-10-08)

## Corpus Check
- 82 files · ~41,783 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 7 file(s) not represented in the graph (top: (none) 2, .properties 2, .jar 1)

## Summary
- 811 nodes · 2256 edges · 40 communities (9 shown, 31 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 154 edges (avg confidence: 0.86)
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
- VisualizerMode
- WindAndSailsClient.java
- WindVisualizerRenderer.java
- WindState
- WindAndSailsMod.java
- SailboatEntity
- .tick
- SailboatEntity.java
- SportSailboatItem.java
- SailboatHudOverlay.java
- .getInstance
- WindManager
- .createPayload
- WindTest
- SportSailboatEntity
- FlagpoleItem.java
- AnchorItem.java
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
2. `WindState` - 40 edges
3. `WindVector` - 40 edges
4. `WindAndSailsMod` - 39 edges
5. `AnchorEntity` - 29 edges
6. `WindManager` - 27 edges
7. `VisualizerMode` - 24 edges
8. `SportSailboatEntity` - 21 edges
9. `WindGust` - 18 edges
10. `FlagpoleBlock` - 16 edges

## Surprising Connections (you probably didn't know these)
- `AnchorRenderer` --references--> `AnchorEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/AnchorRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/AnchorEntity.java
- `FlagpoleBlockEntityRenderer` --references--> `FlagpoleBlockEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/FlagpoleBlockEntityRenderer.java → src/main/java/com/piotrek/pwwindnsails/block/entity/FlagpoleBlockEntity.java
- `SailboatRenderer` --references--> `SailboatEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/SailboatRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/SailboatEntity.java
- `SportSailboatRenderer` --references--> `SportSailboatEntity`  [EXTRACTED]
  src/client/java/com/piotrek/pwwindnsails/client/SportSailboatRenderer.java → src/main/java/com/piotrek/pwwindnsails/entity/SportSailboatEntity.java
- `WindAndSailsMod` --references--> `FlagpoleBlockEntity`  [EXTRACTED]
  src/main/java/com/piotrek/pwwindnsails/WindAndSailsMod.java → src/main/java/com/piotrek/pwwindnsails/block/entity/FlagpoleBlockEntity.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Sailing Dynamics & Physics Engine** — readme_apparent_wind, readme_points_of_sail, readme_hydrodynamics_keel, src_main_java_com_piotrek_pwwindnsails_physics_sailingphysics [INFERRED 0.85]

## Communities (40 total, 31 thin omitted)

### Community 0 - "FlagpoleBlockEntityRenderer.java"
Cohesion: 0.05
Nodes (11): AnchorModel, AnchorRenderer, AnchorRenderState, FlagModel, FlagpoleBlockEntityRenderer, FlagpoleRenderState, SailboatModel, SailboatRenderer (+3 more)

### Community 1 - "net.minecraft.world.phys.Vec3"
Cohesion: 0.07
Nodes (6): CleatHighlightRenderer, PlaneCompatHelper, MooringAim, Target, HeldCleat, ChunkReady

### Community 2 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (7): PhysicsResult, SailingPhysics, WindVector, HullCollisionTest, MooringAndCleatTest, SailingAcceptanceTests, SailingPhysicsTest

### Community 4 - "VisualizerMode"
Cohesion: 0.08
Nodes (9): WindHudOverlay, VisualizerMode, BRIGHT_CYAN, OFF, SEAFOAM_WHITE, SOFT_SKY, WARM_AMBER, WATER_SHADOW (+1 more)

### Community 5 - "WindAndSailsClient.java"
Cohesion: 0.08
Nodes (4): WindAndSailsClient, MooringClickPayload, SailboatInputPayload, WindSyncPayload

### Community 7 - "WindVisualizerRenderer.java"
Cohesion: 0.06
Nodes (8): Apparent Wind Calculation, Hydrodynamics & Keel Leeway Damping, Points of Sail Mechanics, Harmonized Wind System & Gusts, On-Water Wind Visualizer (TAB), ParticleMixin, TreeWindAmbienceHandler, WindAndSailsConfig

### Community 9 - "WindAndSailsMod.java"
Cohesion: 0.08
Nodes (4): Release Changelog, Wind & Sails Mod Documentation, SailboatItem, WindAndSailsMod

### Community 12 - "SailboatEntity.java"
Cohesion: 0.06
Nodes (8): Cleat Mooring & Anchor Mechanics, Gunwale Hiking & Righting Moment, AvatarRendererMixin, AnchorEntity, AbstractBoatMixin, MooringConstraint, Anchor Entity Texture, Sailboat Entity Model Texture

### Community 32 - "WindDayType"
Cohesion: 0.25
Nodes (6): WindDayType, CALM, GUSTY_WIND, LIGHT_BREEZE, MODERATE_BREEZE, STORMY_GALE

## Knowledge Gaps
- **30 isolated node(s):** `SEAFOAM_WHITE`, `BRIGHT_CYAN`, `WARM_AMBER`, `SOFT_SKY`, `WATER_SHADOW` (+25 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 155 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SailboatEntity` connect `SailboatEntity` to `FlagpoleBlockEntityRenderer.java`, `net.minecraft.world.phys.Vec3`, `org.junit.jupiter.api.Test`, `WindAndSailsClient.java`, `Override`, `WindVisualizerRenderer.java`, `WindAndSailsMod.java`, `.tick`, `SailboatEntity.java`, `SportSailboatItem.java`, `.interactHighlightedCleat`, `SailboatHudOverlay.java`, `net.minecraft.world.entity.Entity`, `net.minecraft.world.level.Level`, `SportSailboatEntity`, `.addAdditionalSaveData`, `.isHullColliding`?**
  _High betweenness centrality (0.298) - this node is a cross-community bridge._
- **Are the 20 inferred relationships involving `WindVector` (e.g. with `.testBeamReachAndBroadReachPropulsion()` and `.testFullyEasedSailLuffingAndTrimming()`) actually correct?**
  _`WindVector` has 20 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SEAFOAM_WHITE`, `BRIGHT_CYAN`, `WARM_AMBER` to the rest of the system?**
  _30 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FlagpoleBlockEntityRenderer.java` be split into smaller, more focused modules?**
  _Cohesion score 0.05381400208986416 - nodes in this community are weakly interconnected._
- **Why does `WindManager` connect `WindManager` to `FlagpoleBlockEntityRenderer.java`, `WindAndSailsClient.java`, `WindVisualizerRenderer.java`, `WindState`, `WindAndSailsMod.java`, `SailboatEntity.java`, `SailboatHudOverlay.java`, `.getInstance`, `.createPayload`, `WindTest`, `net.minecraft.world.level.Level`, `WindAndSailsCommands.java`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Should `net.minecraft.world.phys.Vec3` be split into smaller, more focused modules?**
  _Cohesion score 0.06919945725915876 - nodes in this community are weakly interconnected._
- **Why does `WindAndSailsMod` connect `WindAndSailsMod.java` to `FlagpoleBlockEntityRenderer.java`, `net.minecraft.world.phys.Vec3`, `FlagpoleBlock.java`, `VisualizerMode`, `WindAndSailsClient.java`, `SailboatEntity`, `SailboatEntity.java`, `SportSailboatItem.java`, `net.minecraft.world.level.Level`, `SportSailboatEntity`, `FlagpoleItem.java`, `AnchorItem.java`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._