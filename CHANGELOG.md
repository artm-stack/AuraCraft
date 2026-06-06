# Changelog

> All notable changes to this project are documented in this file.

### 1.1.0

**Terminology "tokens/effects" -> "points/auras"**
- Renamed all player-facing and internal references from "tokens" to "points" and from "effects" to "auras" for consistency.

**Restoration Queue**
- Added a per-player, NBT-persisted LIFO restoration queue.
- PvP deaths now push the removed aura ID onto the victim's queue instead of discarding it.
- `/aura withdraw` now pushes each removed aura onto the queue.
- `Aura Plus` checks the queue first; if non-empty, the top aura is automatically re-applied rather than granting a bare point.
- `/aura restart` clears the restoration queue. `/aura reset` and the Aura Reset item do not.
- Removed the old `withdrawnAura` single-slot tracking in favor of the full queue.

**`/aura withdraw [amount]`**
- Command now accepts an optional integer argument to withdraw multiple points/auras at once.
- Processes unspent points first, then aura entries.

**Item changes**
- `Aura Plus` stack size changed from 16 to **1** (each item is a discrete restoration token).
- `Aura Plus` is no longer consumed when used in creative mode.
- `Aura Reset` stack size changed from 16 to **8**.
- `Aura Reset` is now gated: it refuses to activate if the player has never selected any aura, preventing pointless multi-use before any points are invested.
- `Aura Reset` returns the count of refunded points in its confirmation message.

**SelectionUI with 0 points**
- The Aura Picker (`Y`) now opens even when the player has no unspent points.
- The Choose button is disabled when points = 0, but browsing and reading descriptions always works.

**GameRules replace numeric config values**
- `max_auras`, `drop_plus_on_kill`, and `enable_heart_recipe` are now proper Minecraft GameRules, configurable in-game via `/gamerule`.
- Config file (`auracraft.json`) now only controls per-aura enable/disable and max upgrade level.

**Command improvements**
- Added `/aura restart [player]` - hard reset to 1 point, clears all auras, effects, and restoration queue.
- `/aura status` now labels the player name in its output.
- Version mismatch disconnect message is now translatable instead of hardcoded English.

**Code quality**
- `resetPlayer()` now returns the number of points refunded; callers handle their own feedback messages.
- Removed duplicate inline reset logic from `Items.java`; item now delegates directly to `AuraCraft.resetPlayer()`.
- Upgrade message now correctly shows the new level (e.g. "+1 -> Level 2") instead of the pre-upgrade amplifier index.
- Removed dead `withdrawnAura` field and all associated infrastructure from `PlayerMixin` and `PlayerAuraData`.
- Removed orphaned lang key `screen.auracraft.col_max_upg`.
- `"AuraCraft"` title literal in the prompt handler replaced with `Component.translatable("message.auracraft.mod_name")`.
- Removed redundant null-check in `Config.applyDefaults()`.

**New lang keys**
- `message.auracraft.mod_name`
- `message.auracraft.version_mismatch`
- `message.auracraft.aura_restored`
- `message.auracraft.reset_no_history`
- `command.auracraft.status.player`

---

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
