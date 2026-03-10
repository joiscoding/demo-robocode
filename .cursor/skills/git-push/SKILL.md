---
name: git-push
description: Push commits to the fork remote (joiscoding/demo-robocode). Use when the user asks to push, push changes, or deploy to their fork. Default is git push fork main.
---

# Git Push

## Default Command

When pushing changes from this repository, use:

```bash
git push fork main
```

## Remote Setup

| Remote   | Repo                     | Purpose                           |
|----------|--------------------------|-----------------------------------|
| `origin` | robo-code/robocode       | Upstream/main repo                |
| `fork`   | joiscoding/demo-robocode | User's fork (default push target) |

## Notes

- `git push origin main` pushes to the upstream repo, not the fork.
- Use `git push fork main` to push to the user's fork.
