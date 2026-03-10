---
name: Verify Robot Build
description: Verify a Robocode sample robot build by building the robocode.samples module and checking that the expected .class and .properties artifacts exist under .sandbox/robots. Use when the user asks to verify a new robot was built, confirm a robot is discoverable by Robocode, or check whether a sample robot was packaged correctly.
model: fast
readonly: false
---

# Verify Robot Build

## Goal

Confirm that a sample robot was built and copied into `.sandbox/robots` with the expected compiled class and metadata file.

## Inputs

- Robot classname such as `sample.Tab`
- Optional module override if the robot does not live in `robocode.samples`

## Workflow

1. Derive the package path and simple class name from the robot classname.
   - `sample.Tab` -> package path `sample`, class name `Tab`
2. Build the focused module first.
   - Default command: `./gradlew :robocode.samples:jar`
   - Only fall back to `./gradlew build` if the focused build is not appropriate for the requested robot or the project structure clearly requires a full build.
3. Verify the expected artifacts in `.sandbox/robots`:
   - `.sandbox/robots/<package-path>/<ClassName>.class`
   - `.sandbox/robots/<package-path>/<ClassName>.properties`
4. If artifacts are missing, inspect the source and resource locations that should have produced them:
   - `robocode.samples/src/main/java/<package-path>/<ClassName>.java`
   - `robocode.samples/src/main/resources/<package-path>/<ClassName>.properties`
5. Report the result clearly:
   - build command used
   - whether build succeeded
   - whether each expected artifact exists
   - likely cause if something is missing

## Commands

Use these defaults unless the repo layout changes:

```bash
./gradlew :robocode.samples:jar
```

Expected artifact paths for `sample.Tab`:

```text
.sandbox/robots/sample/Tab.class
.sandbox/robots/sample/Tab.properties
```

## Failure Triage

- If the build fails, report the relevant compile or resource error before checking artifacts.
- If `.class` exists but `.properties` is missing, check whether the resource file was created under `src/main/resources`.
- If `.properties` exists but `.class` is missing, check whether the Java source exists, compiles, and matches the classname in the properties file.
- If both files exist but the robot still is not discoverable later, compare `robot.classname` with the Java package and class name.

## Notes

- Prefer the targeted module build over a full repo build for speed.
- Do not claim discovery success from source files alone; require the copied `.sandbox/robots` artifacts.
- Keep the final report short and explicit about what was verified versus what was not.
