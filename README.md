# Peterwolf's Wind & Sails

A realistic, immersive, yet deeply playable wind and sailing mod for Minecraft 26.3 (Fabric).

---

## Features

- **Single-Mast Sailing Boat**: A 3.0m × 1.5m traditional wooden boat featuring an articulated mast, swinging boom, triangular canvas mainsail (~5m² area), and a steerable stern rudder with a cockpit tiller.
- **Realistic Sailing Physics**:
  - **Server-Authoritative**: Smooth movement, momentum, and position synchronization across multiplayer servers and clients.
  - **Apparent Wind**: Combines true world wind and boat velocity to calculate apparent wind vector and angle of attack.
  - **Points of Sail**:
    - **No-Go Zone (In Irons)**: Sailing directly upwind (< 40°) stalls the boat; the sail luffs and generates zero lift.
    - **Close-Hauled**: Sheet pulled tight, fast upwind progress with noticeable boat heel.
    - **Beam & Broad Reach**: Fast, powerful sailing propulsion with optimal sail trim.
    - **Running Downwind**: Boom eased fully out, catching following winds.
  - **Tacking**: Smoothly turn through the wind using momentum to switch tacks.
  - **Hydrodynamics & Keel**: Keel strongly damps sideways leeway slip; rudder authority is realistically proportional to water speed (nearly zero when stationary, effective when moving).
  - **Dynamic Heel**: The hull leans and heels under wind pressure on the sail.
- **Harmonized Wind System & Gusts**:
  - Seamless wind direction and strength simulation per world.
  - Smooth evolution and wandering around prevailing wind.
  - Natural wind gusts with smooth bell-curve onset, peak, and decay (duration 2–12s, strength multiplier 1.10–1.40x, directional shifts up to ±18°).
  - Clouds drift in visual agreement with wind vectors.
- **On-Water Wind Visualizer (TAB)**:
  - Press **TAB** (short tap) to toggle translucent, glowing aerodynamic wind arrows and streaks directly over water surfaces.
  - Communicates real-time wind direction, strength, and gust variations.
  - Holding TAB still displays the multiplayer player list as usual without interference.
- **Sailing Telemetry HUD**:
  - Unobtrusive gauge displayed while at the helm showing mainsheet trim percentage, boat speed in knots, and point-of-sail trim hints.

---

## Controls & Sailing Guide

| Control | Action | Details |
|---|---|---|
| **A** | Steer Port (Lewo) | Turns rudder to port; automatically returns to center when released |
| **D** | Steer Starboard (Prawo) | Turns rudder to starboard; automatically returns to center when released |
| **W** | Pull In Mainsheet | Tightens sheet, pulls boom closer to centerline for sailing upwind / close-hauled |
| **S** | Ease Mainsheet | Loosens sheet, lets boom swing out to leeward for reach and downwind courses |
| **TAB** (Tap) | Toggle Wind Hints | Toggles lightweight wind direction arrows and gusts over water |
| **Shift** (Sneak) | Dismount | Leaves the helm and steps ashore |

### Commands (Live Sensitivity Tuning)

- `/windsails rudder sensitivity` — Check current rudder sensitivity (default: 1.0)
- `/windsails rudder sensitivity <0.1 - 5.0>` — Set rudder turning sensitivity in real-time without restart
- `/windsails rudder autocenter <rate>` — Set auto-center return speed (degrees/tick, default: 2.5)
- `/windsails rudder turnrate <rate>` — Set steering rate when A/D is held (degrees/tick, default: 2.0)
- `/windsails status` — Display all current sailing settings
*(Aliases `/windsails` and `/sailing`)*

### Sailing Tips
1. **Never sail straight into the wind**: If your boat enters the no-go zone (~40° from wind direction), the sail flaps ("in irons") and you lose speed.
2. **Trim your mainsheet**: 
   - When sailing across or downwind, ease the sheet (**S**) so the boom can catch the breeze.
   - When sailing close to the wind, pull the sheet tight (**W**) to generate lift without stalling.
3. **Carry momentum through tacks**: Build up speed on a reach before turning into the wind to complete a tack onto the opposite side.
4. **Rudder needs water flow**: The rudder steers by deflecting water flow; you must have headway for the rudder to take effect.

---

## Crafting Recipe

Crafted from wooden planks, sticks, and wool:

```
[   ] [   ] [ W ]      W = Any Wool (#minecraft:wool)
[   ] [ W ] [ S ]  ->  S = Stick
[ P ] [ P ] [ P ]      P = Any Wooden Planks (#minecraft:planks)
```

---

## Architecture & Code Structure

```
com.piotrek.pwwindnsails
├── WindAndSailsMod.java              # Mod initialization, entity/item/tab/payload registration
├── WindAndSailsConfig.java           # Centralized physics, wind, and rig configuration
├── wind
│   ├── WindVector.java               # Direction (deg) & strength vector utilities
│   ├── WindGust.java                 # Smooth bell-curve natural gust lifecycle
│   ├── WindState.java                # Dimension wind state, wandering, & server/client tick
│   └── WindManager.java              # Global level wind manager & sync dispatcher
├── physics
│   └── SailingPhysics.java           # Apparent wind, lift/drag curves, sheet limits, & rudder torque
├── entity
│   └── SailboatEntity.java           # 3.0m x 1.5m sailing boat, flotation, controls, & persistence
├── item
│   └── SailboatItem.java             # Water placement & boat deployment
├── network
│   ├── WindSyncPayload.java          # Server-to-client wind & gust synchronization packet
│   └── SailboatInputPayload.java      # Client-to-server pilot rudder & mainsheet inputs
└── client
    ├── WindAndSailsClient.java       # Client mod initializer, keybinds, & tick handlers
    ├── SailboatModel.java            # Articulated model (hull, mast, boom, triangular sail, rudder)
    ├── SailboatRenderer.java         # Entity renderer with heel roll, damage wobble, & shadow
    ├── SailboatRenderState.java      # Render state
    ├── SailboatHudOverlay.java       # Telemetry HUD (sheet percentage, speed, trim hints)
    ├── WindVisualizerRenderer.java   # Lightweight linesTranslucent water arrow renderer
    └── mixin
        └── CloudRendererMixin.java   # Harmonizes cloud drift with world wind vector
```

---

## Building

Requires Java 25:

```bash
./gradlew build
```

Generated mod JAR is placed in `build/libs/pw_wind_n_sails-1.0.0+26.3.jar`.
