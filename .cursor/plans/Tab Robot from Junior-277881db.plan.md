<!-- 277881db-5193-4941-bc22-6cca7de785d7 -->
---
todos:
  - id: "create-tab-java"
    content: "Create Tab.java from MyFirstJuniorRobot with extracted variables"
    status: pending
  - id: "add-variable-fields"
    content: "Add private fields for moveDistance, gunSweepAngle, firePower, hitByBulletTurn, hitByBulletAngle, colors"
    status: pending
  - id: "replace-literals"
    content: "Replace hardcoded literals in run(), onScannedRobot(), onHitByBullet() with variable references"
    status: pending
  - id: "create-tab-properties"
    content: "Create Tab.properties with correct classname and metadata"
    status: pending
  - id: "build-and-verify"
    content: "Build project and verify Tab appears in Robocode battles"
    status: pending
  - id: "tweak-values"
    content: "Optional: Tweak variable values (e.g. moveDistance=150, firePower=2) and test"
    status: pending
isProject: false
---
# Tab Robot from MyFirstJuniorRobot

## Goal
Create a new robot named **Tab** by duplicating `MyFirstJuniorRobot`, then extract hardcoded values into named variables so they can be tweaked easily.

## Source
- Java: [robocode.samples/src/main/java/sample/MyFirstJuniorRobot.java](robocode.samples/src/main/java/sample/MyFirstJuniorRobot.java)
- Properties: [robocode.samples/src/main/resources/sample/MyFirstJuniorRobot.properties](robocode.samples/src/main/resources/sample/MyFirstJuniorRobot.properties)

## Files to Create

| File | Purpose |
|------|---------|
| `robocode.samples/src/main/java/sample/Tab.java` | Robot class (copy of MyFirstJuniorRobot with variables) |
| `robocode.samples/src/main/resources/sample/Tab.properties` | Robot metadata for Robocode to discover Tab |

## Variables to Extract and Tweak

From `MyFirstJuniorRobot`, these literals become named fields:

| Variable | Current | Purpose |
|----------|---------|---------|
| `moveDistance` | 100 | Pixels to move forward/back in seesaw |
| `gunSweepAngle` | 360 | Degrees to spin gun when scanning |
| `firePower` | 1 | Bullet power (1–3) |
| `hitByBulletTurn` | 100 | Distance to move when hit |
| `hitByBulletAngle` | 90 | Angle offset for evasion |
| `bodyColor` | green | Body color |
| `gunColor` | black | Gun color |
| `radarColor` | blue | Radar color |

## Implementation Steps

1. **Create Tab.java** – Copy `MyFirstJuniorRobot.java`, rename class to `Tab`, add private fields for the variables above, and replace literals with those fields (initialized in `run()` or as constants).
2. **Create Tab.properties** – Copy `MyFirstJuniorRobot.properties`, update `robot.name`, `robot.classname`, and `robot.description` for Tab.
3. **Build** – Run `./gradlew build` (or the project’s build command) so Tab is compiled and available in Robocode.
4. **Verify** – Start Robocode, add `sample.Tab` to a battle with `sample.MyFirstJuniorRobot` or `sample.SittingDuck`, and confirm it runs and fires.

## Quick Tweaks to Try
- `moveDistance = 150` – wider seesaw
- `firePower = 2` – stronger shots (uses more energy)
- `gunSweepAngle = 180` – faster scan sweep
- Colors: `orange`, `blue`, `white` (from `newjuniorrobot.tpt`)
