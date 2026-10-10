# Graph Report - pw_wind_n_sails  (2026-10-08)

## Corpus Check
- Corpus is ~41,236 words - fits in a single context window. You may not need a graph.

## Summary
- 807 nodes · 2241 edges · 44 communities (11 shown, 33 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 154 edges (avg confidence: 0.86)
- Token cost: 1,200 input · 850 output

## Community Hubs (Navigation)
- Entity & Block Render Pipelines
- Rendering Buffer & Vertex Consumers
- Sailing Physics & Hull Collision Tests
- Block Entity State & Packets
- Client Mod Compatibility & Level Rendering
- Fabric Client Events & Lifecycle Registration
- Sailboat Entity Core & Leash Interactions
- Wind Particle & Weather Ambience
- Wind State Sync & Management
- Mod Registration & Creative Tabs
- Sailboat Cleats & Mooring Damping
- Anchor Entity & Leash Mechanics
- Item Interaction & Tooltips
- Mooring Constraint & Position Sync
- Sailing HUD & Telemetry Overlays
- Wind & Rig Commands
- Server Networking & Packet Dispatch
- Wind Gust Simulation & Dynamics
- Daily Wind Forecasting & Generation
- Cleat Mooring Constraints & Offsets
- Sport Sailboat Model & Rigging
- Tree Wind & Environmental Ambience
- Aerodynamics & Sailing Physics Mechanics
- Flagpole Item & Placement Logic
- Anchor Item & Throw Logic
- Water Surface & Wind Visualizer Mesh
- Daily Wind Profile Day Types
- Mod Icon Asset
- Flagpole Block Texture Asset
- Flag Entity Texture Asset
- Anchor Item Texture Asset
- Flagpole Item Texture Asset
- Sailboat Item Texture Asset
- Sport Sailboat Item Texture Asset

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

## Communities (44 total, 33 thin omitted)

### Community 0 - "Entity & Block Render Pipelines"
Cohesion: 0.05
Nodes (11): AnchorModel, AnchorRenderer, AnchorRenderState, FlagModel, FlagpoleBlockEntityRenderer, FlagpoleRenderState, SailboatModel, SailboatRenderer (+3 more)

### Community 1 - "Rendering Buffer & Vertex Consumers"
Cohesion: 0.07
Nodes (5): CleatHighlightRenderer, MooringAim, Target, HeldCleat, ChunkReady

### Community 2 - "Sailing Physics & Hull Collision Tests"
Cohesion: 0.08
Nodes (7): PhysicsResult, SailingPhysics, WindVector, HullCollisionTest, MooringAndCleatTest, SailingAcceptanceTests, SailingPhysicsTest

### Community 4 - "Client Mod Compatibility & Level Rendering"
Cohesion: 0.07
Nodes (9): PlaneCompatHelper, VisualizerMode, BRIGHT_CYAN, OFF, SEAFOAM_WHITE, SOFT_SKY, WARM_AMBER, WATER_SHADOW (+1 more)

### Community 5 - "Fabric Client Events & Lifecycle Registration"
Cohesion: 0.08
Nodes (4): WindAndSailsClient, MooringClickPayload, SailboatInputPayload, WindSyncPayload

### Community 7 - "Wind Particle & Weather Ambience"
Cohesion: 0.11
Nodes (4): AvatarRendererMixin, ParticleMixin, AbstractBoatMixin, MooringConstraint

### Community 9 - "Mod Registration & Creative Tabs"
Cohesion: 0.09
Nodes (4): Release Changelog, Wind & Sails Mod Documentation, SportSailboatItem, WindAndSailsMod

### Community 14 - "Mooring Constraint & Position Sync"
Cohesion: 0.14
Nodes (4): Cleat Mooring & Anchor Mechanics, Gunwale Hiking & Righting Moment, Anchor Entity Texture, Sailboat Entity Model Texture

### Community 15 - "Sailing HUD & Telemetry Overlays"
Cohesion: 0.17
Nodes (3): Sailing Telemetry HUD Overlay, SailboatHudOverlay, WindHudOverlay

### Community 25 - "Aerodynamics & Sailing Physics Mechanics"
Cohesion: 0.24
Nodes (5): Apparent Wind Calculation, Hydrodynamics & Keel Leeway Damping, Points of Sail Mechanics, Harmonized Wind System & Gusts, WindAndSailsConfig

### Community 32 - "Daily Wind Profile Day Types"
Cohesion: 0.25
Nodes (6): WindDayType, CALM, GUSTY_WIND, LIGHT_BREEZE, MODERATE_BREEZE, STORMY_GALE

## Knowledge Gaps
- **30 isolated node(s):** `SEAFOAM_WHITE`, `BRIGHT_CYAN`, `WARM_AMBER`, `SOFT_SKY`, `WATER_SHADOW` (+25 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 155 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SailboatEntity` connect `Sailboat Entity Core & Leash Interactions` to `Entity & Block Render Pipelines`, `Rendering Buffer & Vertex Consumers`, `Sailing Physics & Hull Collision Tests`, `Client Mod Compatibility & Level Rendering`, `Fabric Client Events & Lifecycle Registration`, `Wind Particle & Weather Ambience`, `Mod Registration & Creative Tabs`, `Sailboat Persistence & Rudder/Heel State`, `Sailboat Cleats & Mooring Damping`, `Item Interaction & Tooltips`, `Mooring Constraint & Position Sync`, `Sailing HUD & Telemetry Overlays`, `Sailboat Leash & Collision Restrictions`, `Entity Spawning & Interaction Lifecycle`, `Cleat Mooring Constraints & Offsets`, `Sport Sailboat Model & Rigging`, `Sailboat Rendering & Animation State`, `Hull Collision & Stepping Calculation`, `Water Surface & Wind Visualizer Mesh`?**
  _High betweenness centrality (0.299) - this node is a cross-community bridge._
- **Are the 20 inferred relationships involving `WindVector` (e.g. with `.testBeamReachAndBroadReachPropulsion()` and `.testFullyEasedSailLuffingAndTrimming()`) actually correct?**
  _`WindVector` has 20 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SEAFOAM_WHITE`, `BRIGHT_CYAN`, `WARM_AMBER` to the rest of the system?**
  _30 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Entity & Block Render Pipelines` be split into smaller, more focused modules?**
  _Cohesion score 0.05286006128702758 - nodes in this community are weakly interconnected._
- **Why does `WindManager` connect `Server Networking & Packet Dispatch` to `Entity & Block Render Pipelines`, `Fabric Client Events & Lifecycle Registration`, `Wind Particle & Weather Ambience`, `Wind State Sync & Management`, `Mod Registration & Creative Tabs`, `Mooring Constraint & Position Sync`, `Sailing HUD & Telemetry Overlays`, `Wind & Rig Commands`, `Wind Gust Simulation & Dynamics`, `Entity Spawning & Interaction Lifecycle`, `Tree Wind & Environmental Ambience`, `Aerodynamics & Sailing Physics Mechanics`, `Water Surface & Wind Visualizer Mesh`, `Brigadier Command Argument Parsers`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Should `Rendering Buffer & Vertex Consumers` be split into smaller, more focused modules?**
  _Cohesion score 0.07272727272727272 - nodes in this community are weakly interconnected._
- **Why does `WindAndSailsMod` connect `Mod Registration & Creative Tabs` to `Entity & Block Render Pipelines`, `Rendering Buffer & Vertex Consumers`, `Block Entity State & Packets`, `Client Mod Compatibility & Level Rendering`, `Fabric Client Events & Lifecycle Registration`, `Sailboat Entity Core & Leash Interactions`, `Anchor Entity & Leash Mechanics`, `Item Interaction & Tooltips`, `Mooring Constraint & Position Sync`, `Entity Spawning & Interaction Lifecycle`, `Sport Sailboat Model & Rigging`, `Flagpole Item & Placement Logic`, `Anchor Item & Throw Logic`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._