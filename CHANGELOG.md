# Changelog

> All notable changes to this project are documented in this file.

### 1.0.3 Patch
- Fixed `ClientHelloPayload` never being registered server-side, which could silently drop packets or disconnect clients.
- Moved `enabledEffectIds` from `SyncEffectPayload` to `UiStatePayload` so the full enabled-effect list is sent once on join rather than on every sync call.
- Bumped client/server handshake protocol to version 3 to reflect the changed packet formats.
- Fixed grammar in PvP death messages: *"killed lost"* → *"killed and lost"*, *"a Aura Plus"* → *"an Aura Plus"*.
- Fixed `resetForRepick` hardcoding the key name `[Y]` in its message; the server now sends a generic translatable string and lets the existing prompt payload show the player's actual bound key.
- Simplified the redundant `instanceof` pattern in `applyEffectById` / `removeEffectById` to a null check followed by a single unchecked cast.
- Config screen is now fully read-only when connected to a dedicated server: Save button is disabled, fields are non-editable, and a notice is displayed. Effect toggle buttons also respect this state.
- Config screen server-side sync is now submitted via `server.execute()`, fixing a client-thread race condition in singleplayer.
- Converted all remaining hardcoded `Component.literal` player-facing messages to `Component.translatable` entries.
- Added missing lang keys: `ui_disabled`, `effect_reset`, `repick_item_used`, `max_tokens_reached`, `extra_token_gained`, `withdrawn_effect_restored`, `pvp_lost_token`.
- Replaced private constants + getter boilerplate (`getChosenEffectKey()` etc.) with `public static final` fields in `EffectSmpMod`.
- Replaced manual last-element iteration over `LinkedHashSet` with `stream().reduce((a, b) -> b)` in `withdrawLatestSelectedEffect` and `removeLastSelectedEffects`.
- Removed `refreshEnabledEffectsFromConfig()` from the client mod; the enabled-effect list is now kept in sync exclusively via `UiStatePayload`.

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
