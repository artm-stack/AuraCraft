# Changelog

> All notable changes to this project are documented in this file.

### 1.0.2 Patch
- Added withdrawn-effect tracking stack for players.
- Updated `/aura withdraw` to withdraw one selected effect into an `Aura Plus` item (falls back to withdrawing one token if no effects are selected).
- Updated `Aura Plus` consume behavior to restore the latest withdrawn effect first, then fallback to first-effect restore, then token gain.
- Removed dead/unreachable logic flagged in review (`EffectType` cleanup and redundant Aura Plus branch checks).
- Bumped client/server handshake protocol to reject 1.0.1 clients on 1.0.2 servers.

### 1.0.1 Patch
- Fixed effect amplifier logic: first-time effect picks now apply base level only, and amplifier increases only from duplicate picks.
- Updated duplicate-upgrade message to reflect the true bonus value.
- Added Gradle wrapper launchers (`gradlew`, `gradlew.bat`) for reproducible local builds.

### 1.0.0 Release
- Initial public release of AuraCraft for Minecraft `26.1.2` on Fabric Loader `0.19.1+`.
- Token-based effect selection and upgrades with persistent player state.
- `Aura Plus` and `Aura Reset` items.
- `/aura` command suite (`status`, `reset`, `plus`, `remove`, `withdraw`).
- Server/client compatibility handshake gate.
- Mod Menu configuration support with General and Effects tabs.
