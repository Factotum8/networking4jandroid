# networking4jandroid

Standalone, offline Android app for personal contacts / relationship graph —
a trimmed public Google Play version of the ideas from
[networking4j](https://github.com/Factotum8/networking4j) (private Python/
FastAPI/Neo4j CRM, not related to this repo's build).

## Scope

- **Fully offline / client-only.** No server, no network calls. Local
  storage on-device (SQLite/Room). This is deliberate: avoids Google Play
  Data Safety / third-party personal data complications that a networked app
  handling other people's personal info would trigger.
- Contacts + relationship graph only. No AI features by default. If AI ever
  gets added, the user supplies their own API key in settings, stored in
  Android Keystore — never a key bundled by the developer.
- Positioned in the Play Store as a personal contacts/relationship
  organizer.
- Needs its own Privacy Policy page before a Play Store listing (required
  even for a fully local app that handles personal data).
- Target distribution: public Google Play listing (this is the "public"
  counterpart to networking4j's private, cloud-hosted client-server track,
  which lives in the other repo and is out of scope here).

## `reference/`

Snapshots of domain model and graph-layout code copied from the
networking4j repo (2026-09-15) as a starting point for porting to
Kotlin/Room — not part of this repo's build, not maintained in sync with
the source repo:

- `reference/models/` — Pydantic domain models (`contact.py`, `relative.py`,
  `relationships.py`, `dimensions.py`, `enums.py`, `graph.py`) defining
  contacts, relatives, relationship links, the "circle"/dimension
  categorization, and the graph snapshot shape.
- `reference/ui/graph_layout.py` — pure-Python concentric-circle graph
  layout math (3 rings + unassigned cluster), framework-agnostic, worth
  porting directly.
- `reference/ui/labels.py` — Russian label translations for circles/
  dimensions, for UI text parity.

## Status

Gradle/Android project scaffolded (Kotlin, Room, Compose, Hilt; single
`:app` module, `applicationId`/package `com.factotum8.networking4jandroid`).
The `reference/models/` domain models (`Contact`, `Relative`, the six
dimension nodes, the property-less dimension links, `Knows`) are ported to
Room entities/DAOs under `app/src/main/java/.../data/local/`. No real UI
yet — `MainActivity` just shows a placeholder screen so the app builds and
runs.

`InteractionType`/`ActionType` and their node models (`Interaction`,
`Action`) are intentionally not ported yet: `reference/models/enums.py`
references them, but the corresponding `interaction.py`/`action.py` were
never snapshotted into `reference/`. Porting them is a follow-up stage
once those source models are available.

Next steps: open in Android Studio (or run `./gradlew :app:compileDebugKotlin`
locally) to confirm the scaffold actually compiles — unverified from this
environment, which has no JDK/Android SDK installed — then build out a real
UI (contact list/detail, the graph screen) on top of these entities.
