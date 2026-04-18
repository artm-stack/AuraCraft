# AuraCraft — Complete Wiki

Welcome to the AuraCraft wiki. This guide covers what players and server operators need to know, based on the current mod behavior.

---

## Table of Contents

1. [Core Concepts](#1-core-concepts)
2. [Getting Started (Players)](#2-getting-started-players)
3. [Aura Picker UI](#3-aura-picker-ui)
4. [Token Economy](#4-token-economy)
5. [Effect Upgrades](#5-effect-upgrades)
6. [Special Items](#6-special-items)
7. [PvP & Death Mechanics](#7-pvp--death-mechanics)
8. [Commands Reference](#8-commands-reference)
9. [Server Setup Guide](#9-server-setup-guide)
10. [Configuration Reference](#10-configuration-reference)
11. [Compatibility & Multiplayer](#11-compatibility--multiplayer)
12. [FAQ](#12-faq)


---

## 1) Core Concepts

AuraCraft is built around:

- **Auras**: persistent status effects selected by players.
- **Tokens**: spent to select effects or upgrade duplicates.
- **Load**: the combined budget rule:

```
selected_effects + available_tokens <= maxEffects
```

Tokens and selected effects share the same budget.

---

## 2) Getting Started (Players)

1. Join a server with AuraCraft installed.
2. Press **Y** (default) to open the picker.
3. Select an enabled effect to spend 1 token.
4. Your selected effects persist and are reapplied.

![Aura Picker UI](docs/images/ui-picker.png)

Notes:
- If the server disables the UI globally, the picker cannot be used.
- If your client mod is missing/outdated versus server protocol, join is blocked.

---

## 3) Aura Picker UI

![Aura Picker UI](docs/images/ui-picker.png)

The picker opens with **Y** by default (rebindable in Controls).

Behavior:
- Shows enabled effects from server config.
- Selecting a new effect consumes 1 token.
- Selecting an owned effect upgrades it (if below cap) and consumes 1 token.
- If an effect is at duplicate cap, it cannot be selected again.

---

## 4) Token Economy

### Core Limits

- `MAX_SELECTION_TOKENS` hard cap: **3**
- Combined load cap: `selected_effects + available_tokens <= maxEffects`

### How tokens are gained

- Using `Aura Plus` (item ID: `auracraft:aura_plus`), when allowed by caps.
- Admin command: `/aura plus [player] [amount]`
- In some cases, `Aura Plus` restores your first lost effect instead of adding a token.

### How tokens are spent/lost

- Selecting or upgrading effects.
- PvP death logic (see section 7).
- Admin command: `/aura remove [player] [amount]`
- Voluntary command: `/aura withdraw`

---

## 5) Effect Upgrades

Selecting the same effect again increases amplifier bonus (duplicate upgrade) up to `maxDuplicateAmplifierBonus`.

Example with default bonus cap `1`:
- First selection: base amplifier
- Second selection: +1 bonus amplifier
- Further selections on that same effect are blocked at cap

---

## 6) Special Items

### Aura Plus

- **ID**: `auracraft:aura_plus`
- **Stack size**: 16
- **Use behavior**:
  - Fails if token cap (3) is already reached.
  - Fails if combined load cap (`maxEffects`) is full.
  - If first selected effect was lost and conditions match, it may restore that effect.
  - Otherwise grants +1 token.

### Aura Reset

- **ID**: `auracraft:aura_reset`
- **Stack size**: 16
- **Use behavior**:
  - Removes currently selected effects.
  - Returns repick tokens equal to number of selected effect IDs cleared.
  - Opens picker prompt when UI is enabled.

---

## 7) PvP & Death Mechanics

AuraCraft applies PvP consequences when killed by another player.

![Aura Token Lost](docs/images/token-lost.png)
![Aura Effect Lost](docs/images/effect-lost.png)

### Config knob

- `pvpEffectsLostOnDeath` controls how many effects are removed (default `1`).

### Current death logic

- If victim has exactly **1 selected effect** and **at least 1 token**, death removes **1 token** instead of removing that effect.
- Otherwise, effects are removed according to `pvpEffectsLostOnDeath`.
- If `dropPlusItemOnPvpKill` is enabled, killer-side `Aura Plus` drop can occur when:
  - victim had no selected effects, or
  - victim lost an effect, or
  - victim lost a token via the 1-effect protection rule.

---

## 8) Commands Reference

Root command: `/aura`

### Player command

- `/aura withdraw`
  - Removes 1 token from self.

### GameMaster commands (OP level 2+)

- `/aura`
  - Toggles AuraCraft UI for the world.
- `/aura status [player]`
  - Shows tokens, first effect, selected effects, UI state.
- `/aura reset [player]`
  - Clears effects and enables repick flow.
- `/aura plus [amount]`
- `/aura plus <player> [amount]`
  - Adds tokens with cap checks.
- `/aura remove [amount]`
- `/aura remove <player> [amount]`
  - Removes tokens.

---

## 9) Server Setup Guide

1. Put `auracraft-<version>.jar` and `fabric-api-<version>.jar` in server `mods/`.
2. Start once to generate `config/auracraft.json`.
3. Edit config or use Mod Menu (where applicable).
4. Restart server.
5. Ensure clients use compatible AuraCraft version.

Recommended first checks:
- Set `maxEffects` for your balance target.
- Set `pvpEffectsLostOnDeath` (0 disables PvP loss).
- Review `enabledEffects` toggles.
- Decide whether `dropPlusItemOnPvpKill` should be on.

---

## 10) Configuration Reference

Config file: `config/auracraft.json`

Current keys:

| Key | Type | Default | Description |
|---|---|---:|---|
| `maxEffects` | integer | `3` | Combined load cap (`selected + tokens`). |
| `pvpEffectsLostOnDeath` | integer | `1` | Effects removed on PvP death. |
| `maxDuplicateAmplifierBonus` | integer | `1` | Max duplicate upgrade bonus per effect. |
| `dropPlusItemOnPvpKill` | boolean | `true` | Allows Aura Plus drop on qualifying PvP kills. |
| `enableResetRecipe` | boolean | `true` | Enables Aura Reset recipe. |
| `enablePlusRecipe` | boolean | `true` | Enables Aura Plus recipe. |
| `enabledEffects` | object(map) | generated | Per-effect enable/disable table. |

`enabledEffects` example:

```json
"enabledEffects": {
  "minecraft:speed": true,
  "minecraft:haste": true,
  "minecraft:strength": true,
  "minecraft:blindness": false
}
```

Disabled effects do not appear in picker selection.

---

## 11) Compatibility & Multiplayer

AuraCraft enforces a client/server handshake protocol on join:

- Missing client mod: join is denied.
- Outdated/incompatible client protocol: join is denied.

This prevents desync between client UI/packets and server effect logic.

---

## 12) FAQ

Q. Why can’t I open the picker?
A. Possible reasons: UI is disabled by admin, you have no available tokens, or your client/server version handshake failed.

Q. Can I select disabled effects?
A. No. Disabled effects are hidden from picker selection.

Q. Can I exceed 3 tokens?
A. No. Token count is hard-capped at 3.

Q. Why did I lose a token instead of my last effect in PvP?
A. If you had exactly one selected effect and spare tokens, the mod removes one token first.

Q. What does `/aura status` show?
A. Tokens, first effect, selected effect IDs, and UI-disabled state.

Q. Is AuraCraft required on both server and client?
A. Yes. The join handshake rejects missing or outdated client installs.
