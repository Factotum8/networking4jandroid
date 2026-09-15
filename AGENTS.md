# AGENTS.md — networking4jandroid

> This file follows the `AGENTS.md` convention, picked up by default by AI agents (Codex CLI, Claude Code, etc.).

Related documents:
- [`README.md`](./README.md) — scope of this repo (offline Android contacts/graph app, public Google Play track — see the "Track 2" split from the private [networking4j](https://github.com/Factotum8/networking4j) repo).

## What this project is

A standalone, offline Android app: a personal contacts/relationship graph
organizer, trimmed down from the private networking4j CRM for public Google
Play distribution. No server, no network calls — see `README.md` for the
full scope and what's deliberately excluded (AI features, any coupling to
networking4j's server or 1Password-based secrets).

## Technology stack

```
Kotlin, Android (Room for local storage, Compose for UI) — not yet
scaffolded; see README.md "Status". Ported reference domain models and
graph-layout math live under reference/ (from networking4j, snapshot
2026-09-15, not part of this repo's build).
```

## Agent working rules (always apply, not just in the first session)

1. **Do not invent or guess.** Ambiguity in a task is a reason to stop and ask, not to silently pick an option.
2. **Questions first → then a plan → agreement → then code.** Don't start writing code without an approved plan, unless the task is trivial (a one-line fix/typo can go straight through).
3. A fork in the road or missing information during work — ask again, don't proceed "at your own discretion."
4. After a major stage — a short report on what's done and what's next, wait for confirmation.
5. Explicit user permission ("your call") waives rule 1 for that specific decision — but not by default.
6. **Prefer sub-agents for independent, parallelizable work.** When a task splits into unrelated chunks — searching across different parts of the codebase, reading multiple unrelated files, running independent checks — dispatch sub-agents for them concurrently (in a single batch) instead of doing them one by one. This keeps the main context lean and cuts wall-clock time. For a single sequential step (one file read, one small edit) handle it directly — spinning up a sub-agent there adds overhead without any speed-up.

---

## Code style

> TBD once the Android project is scaffolded (Kotlin style guide, Room/Compose conventions). The networking4j repo's Python-specific style rules (Pixi, PEP 8, etc.) don't apply here and were intentionally not carried over.
