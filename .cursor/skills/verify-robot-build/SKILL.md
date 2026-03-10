---
name: verify-robot-build
description: Verify a Robocode sample robot build by building the `robocode.samples` module and checking that the expected `.class` and `.properties` artifacts exist under `.sandbox/robots`. Use when the user asks to verify a new robot was built, confirm a robot is discoverable by Robocode, or check whether a sample robot was packaged correctly.
---

# Verify Robot Build

## When to Use

Apply this skill when the user asks to verify a robot was built, confirm a robot is discoverable, or check whether a sample robot was packaged correctly.

## How to Execute

**Delegate to the Verify Robot Build subagent** (`.cursor/agents/verify-robot-build.md`). Use the Task tool with subagent type or invoke the subagent so it runs the full workflow in its own context.

Provide the robot classname (e.g. `sample.Tab`) and any module override if the robot does not live in `robocode.samples`.

## Reference

The subagent contains the complete workflow: build, artifact verification, failure triage, and reporting. Do not duplicate that logic here.
