---
name: kill-games
description: Terminate all running Robocode game processes. Use when the user asks to kill, stop, quit, or close Robocode games, or to shut down running Robocode instances.
---

# Kill Games

## Goal

Terminate all running Robocode processes so no game windows remain active.

## Workflow

1. Run `pkill -f robocode` to kill all processes whose command line contains "robocode".
2. If `pkill` returns 0, processes were found and killed. If it returns non-zero (no matches), no Robocode processes were running.
3. Use `|| true` so the command does not fail when no processes match.

## Command

```bash
pkill -f robocode || true
```

## Notes

- Matches the Robocode main class (`robocode.Robocode`), `robocode.sh`, and any Robocode-related processes.
- On macOS/Linux, `pkill -f` matches the full command line.
- The `|| true` ensures the skill completes successfully even when no processes are running.
