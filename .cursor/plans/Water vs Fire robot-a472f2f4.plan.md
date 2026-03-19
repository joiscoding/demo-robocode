<!-- a472f2f4 -->
---
todos:
  - id: create-water-java
    content: Create Water.java with anti-fire movement and gunnery
    status: pending
  - id: create-water-properties
    content: Create Water.properties for Robocode discovery
    status: pending
  - id: build-verify
    content: Build robocode.samples and verify Water under .sandbox/robots
    status: pending
isProject: false
---
# Water vs Fire robot

## Goal
Add **Water** (`sample.Water`), a blue-themed bot tuned to beat fire-style samples: stationary spinners (`Fire`), trackers that sit still (`TrackFire`), and chargers (`RamFire`).

## Strategy
- `setAdjustGunForRobotTurn(true)` so the body can strafe while the gun aims.
- Keep distance in a **sweet band** (default ~140–380); close in if too far, back up if too close (weak vs ram and high-power point blank).
- **Lateral strafe** (perpendicular to line of sight) while in band to dodge linear aim.
- **Gun**: align like `TrackFire`; fire power scales with distance and own energy; respect `getGunHeat() == 0`.
- **HitByBulletEvent**: short perpendicular dodge; **HitRobot**: fire and create space.

## Files
| File | Purpose |
|------|---------|
| `robocode.samples/src/main/java/sample/Water.java` | Robot |
| `robocode.samples/src/main/resources/sample/Water.properties` | Metadata |

## Tunable fields (in class)
Ideal min/max distance, strafe step, gun sweep, aim threshold, fire powers, dodge distances, colors.
