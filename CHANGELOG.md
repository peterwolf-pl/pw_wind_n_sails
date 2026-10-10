# Changelog

## 1.0.7

- **Xaero's Minimap & World Map Integration**: added optional compile-time integration and custom map rendering for buoys (`BuoyWorldMapRenderer`, `BuoyTracker`), accurately marking red spherical and blue cylindrical buoys on the world map and minimap.
- **Particle Mixin Crash Consolidation**: unified wind drift logic for ash smoke, campfire smoke, and falling leaves into a single `ParticleMixin` on `Particle.class` via `move(DDD)V` variable modification, resolving mixin conflicts and client crashes.
- **Mooring & Cleat Collision Refinements**: polished aim-assist and interaction priority so fence posts and cleats register right-clicks reliably without adjacent blocks intercepting lead bindings.
- **Auto Wind HUD & Water Arrows on Boarding**: mounting a sailboat now automatically activates the wind HUD and wind stream indicators, and gracefully hides them upon dismounting.

## 1.0.6

- **Manual Wind Override & Simulation Lock**: setting wind via `/wind set` or `/windsails wind set` now locks the atmospheric state, preventing daily weather cycles from overriding manual test conditions until reset with `/wind reset` or `/windsails reset`.
- **Command Aliases & Singleplayer Access**: added root `/wind` and `/setwind` aliases, direct shorthand syntax (`/windsails wind <strength> [direction]`, `/windsails reset`), and allowed wind commands in singleplayer without requiring dedicated server operator permissions.
- **Sleek Aerodynamic Wind Arrows**: water wind arrows are now ~3x thinner with refined tapering and chevron arrowheads. TAB now toggles the chosen style ON/OFF directly instead of cycling. Active style can be selected via `/windarrow <1..5|name>` or `/windsails arrow`. Arrows now render exclusively while aboard a sailboat and include chunk-safety guards.
- **Dynamic Forecast HUD Formatting**: enhanced HUD forecast rendering with `parseForecastComponent` to properly localize and format compound forecast messages in both English and Polish.

## 1.0.5

- **Wind HUD Forecast Localization**: added full English and Polish translation keys for all daily wind forecast messages (calm doldrums, morning breeze, night trends, veering wind, and storm/gale warnings) in the wind telemetry HUD (`H` key).

## 1.0.4

- **Localization**: replaced hardcoded HUD, action bar, and mooring overlay text literals with proper translation keys. Added full English (`en_us`) and Polish (`pl_pl`) translations for all sailing HUD elements, wind telemetry & forecast, hiking status, rudder angles, points of sail, trim advice, cleat names, and mooring/anchor prompts.

## 1.0.3

- **Anchor**: right-click throws it; a second right-click on a cleat ties it. Craft with 5 iron ingots:

```
 I
III
I I
```

- **Mooring**: four cleats. Right-click hits the highlighted post or cleat, not a neighbor. A vanilla boat's lead is tied to the dock knot, so the rope is drawn.
- **Mooring physics**: within the slack the boat drifts and turns so the tied end follows the line. One line does not stretch. Two cleats on the same side hang white fenders and hold the hull half a block off the dock.
- **Shore**: the boat is not carried onto a pier or land.

## 1.0.2

- 4.5 m sport sailboat with a jib.
- Hull outline collision, dry-land dismount, wind commands, and TAB wind arrows.

## 1.0.1

- Flagpole, sail furling, hiking with Space, daily wind, and the H wind HUD.

## 1.0.0

- First release for Minecraft 26.3: sailboat, rudder, sheet, and wind.
