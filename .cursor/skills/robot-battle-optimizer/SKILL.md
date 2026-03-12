---
name: robot-battle-optimizer
description: Optimize any Robocode robot for best battle performance by analyzing its structure, identifying tunable parameters, and iterating on values through battles. Use when the user asks to optimize a robot, tune robot parameters, improve battle performance, or iterate on a Robocode bot.
---

# Robot Battle Optimizer

## Goal

Optimize any Robocode robot for best battle performance: analyze the robot, identify tunable parameters, build, verify.

## When to Use

Apply this skill when the user asks to optimize a robot, tune parameters for battles, improve battle performance, or iterate on a Robocode bot.

## Inputs

- **Robot classname** (required): e.g. `sample.Tab`, `sample.Corners`, `sample.RamFire`
- **Opponents** (optional): specific robots to optimize against. Default: `sample.MyFirstJuniorRobot`, `sample.SittingDuck`, `sample.Corners`, `sample.RamFire`
- **Module** (optional): build module if not `robocode.samples` (e.g. `robocode.tests.robots`)

## Phase 1: Locate and Analyze the Robot

1. Derive package path and class name from the robot classname (e.g. `sample.Tab` → package `sample`, class `Tab`).
2. Find the Java source: typically `robocode.samples/src/main/java/<package>/<Class>.java` or equivalent in other modules.
3. Read the robot source and identify:
   - **Movement parameters**: distances in `ahead()`, `back()`, `turnAheadLeft()`, `turnAheadRight()`, etc.
   - **Gun/radar parameters**: angles in `turnGunRight()`, `turnGunLeft()`, scan sweep amounts.
   - **Fire parameters**: power values in `fire()` (1–3).
   - **Evasion parameters**: values in `onHitByBullet()`, `onHitWall()`, `onHitRobot()`.
4. If the robot uses hardcoded literals, extract them into named fields so they can be tuned.

## Phase 2: Optimize for Battle Performance

### Robocode Battle Mechanics (generic guidance)

| Parameter type       | Effect                                                  | Optimization guidance                                                                                        |
|----------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **firePower**        | 1–3. Higher = more damage per shot, more energy cost.   | Power 2 is a strong default. Power 3 for aggressive play vs weak opponents. Power 1 for energy conservation. |
| **moveDistance**     | Pixels per move. Larger = more movement, harder to hit. | 120–200 improves survivability vs accurate shooters.                                                         |
| **gunSweepAngle**    | Degrees to spin gun when scanning.                      | 180 = faster scan cycle. 360 = full coverage. Smaller = quicker target acquisition.                        |
| **evasion distance** | Distance moved when hit (e.g. in `onHitByBullet`).      | 100–150. Slightly higher improves evasion.                                                                   |
| **evasion angle**    | Angle offset (90 = perpendicular to bullet).            | Keep ~90 for perpendicular evasion.                                                                          |

### Optimization workflow

1. Identify which parameters exist in the robot and their current values.
2. Apply recommended values based on parameter type (see table above).

## Commands

```bash
# Build (adjust module as needed)
./gradlew :robocode.samples:jar

# Verify artifacts
ls -la .sandbox/robots/<package-path>/<ClassName>.class .sandbox/robots/<package-path>/<ClassName>.properties
```

## Notes

- JuniorRobot uses simplified APIs (`ahead`, `back`, `turnGunRight`, `turnGunTo`, `fire`, `turnAheadLeft`).
- Standard Robot uses `setAhead`, `setTurnGunRight`, `fire`, etc. Same principles apply.
- Colors do not affect performance.
- Optimize against a mix: SittingDuck (easy), similar-tier bots, Corners (defensive), RamFire (aggressive).

## Delegation

When available, delegate to the **Robot Battle Optimizer** subagent via the Task tool for autonomous execution. Provide the robot classname and any optional inputs (opponents, module).
