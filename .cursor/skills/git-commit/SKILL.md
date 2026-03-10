---
name: git-commit
description: Stage and commit changes with descriptive messages. Use when the user asks to commit, save changes to git, or create a commit.
---

# Git Commit

## Goal

Stage and commit changes with a clear, descriptive commit message.

## Workflow

1. Run `git status` to see what has changed.
2. Stage files: `git add <paths>` for specific files, or `git add -A` for all changes.
3. If the user did not specify a message, generate one from the diff:
   - Run `git diff --cached` (or `git diff` if nothing staged) to inspect changes.
   - Write a concise message that summarizes the change.
4. Commit: `git commit -m "Your message"`.

## Commit Message Format

Use short messages in this format:

```
[category] short description
```

Examples: `[start-game] kill and restart existing Robocode`, `[docs] update README`, `[fix] correct date formatting`.

## Notes

- Do not commit unless the user explicitly asks.
- Do not force-push or amend without being asked.
- If there are no changes to commit, report that and stop.
