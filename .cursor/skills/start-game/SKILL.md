---
name: start-game
description: Start the Robocode UI from this repository. Use when the user asks to spin up the UI, launch Robocode, start the game, open the desktop app, or run the local sandbox build.
---

# Start Game

## Goal

Launch the local Robocode UI from this repository's `.sandbox` directory.

## Workflow

1. Check the terminals directory first to avoid starting a duplicate UI process.
2. If a Robocode launcher is already running and healthy, tell the user instead of launching another copy.
3. Verify `.sandbox/robocode.sh` exists.
4. If the sandbox launcher is missing, confirm the project has already been built and install the distribution into `.sandbox` from the setup jar in `build/`.
5. Start the UI from `.sandbox` with `./robocode.sh 2>&1` as a background command.
6. Wait for a healthy startup signal such as `Loaded net.sf.robocode.ui`.
7. Tell the user the UI is up, or report the startup error clearly if launch failed.

## Commands

Use these defaults unless the repo changes:

```bash
# Start the UI
./robocode.sh 2>&1
```

If `.sandbox/robocode.sh` does not exist, install from the built setup jar first:

```bash
java -jar build/robocode-*-setup.jar /absolute/path/to/repo/.sandbox silent 2>&1
```

Then launch from `.sandbox`.

## Notes

- Run the launcher from `.sandbox`, not the repository root.
- Prefer monitoring logs until the UI module loads instead of assuming startup succeeded.
- Do not kill or restart an existing healthy Robocode UI unless the user asks.
