# AGENTS.md

## Cursor Cloud specific instructions

### Project overview

Robocode is a Java desktop programming game (Swing GUI) built with Gradle 9.3.1. See `README.md` and the comment at the top of `build.gradle.kts` for general build/run commands.

### JDK requirement

The CI targets **JDK 17** (see `.github/workflows/assembly.yml`). JDK 17 must be installed and used for builds. Set:
```
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Build and run

- **Build:** `./gradlew build -x test` (skips tests; see known test issues below)
- **Run UI:** `cd .sandbox && DISPLAY=:1 ./robocode.sh` — launches the Swing GUI on the desktop display
- **Compile check (lint):** `./gradlew compileJava` — no dedicated linter (checkstyle/PMD/SpotBugs) is configured

### Running tests

- `./gradlew :robocode.core:test` — passes reliably
- `:robocode.host:test` — **pre-existing failure**: `JarJarTest` fails with `InaccessibleObjectException` due to JDK module access restrictions on `URL.setURLStreamHandlerFactory`
- `:robocode.tests:test` — **pre-existing failure**: integration tests fail with `NullPointerException` in `RepositoryManager.update()` because the engine initialization does not complete properly in the Gradle test runner context

### Launching the Robocode GUI

The `.sandbox/` directory is produced by the Gradle build (specifically by `robocode.content` tasks). To show the GUI on the Desktop pane, use `DISPLAY=:1` (not `:99`). The startup log should show `Loaded net.sf.robocode.ui` when healthy.

### Skills

The `.cursor/skills/` directory contains reusable workflows: `start-game`, `verify-robot-build`, `git-commit`, `git-push`. Read the relevant SKILL.md before using.
