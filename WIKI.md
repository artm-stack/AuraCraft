# AuraCraft Wiki

Welcome to the AuraCraft wiki. This guide covers what players and server operators need to know, based on the current mod behavior.

---

## Table of Contents

1. [Overview](#overview)
2. [How the Point System Works](#how-the-point-system-works)
3. [Items](#items)
4. [Aura Picker UI](#aura-picker-ui)
5. [PvP Rules](#pvp-rules)
6. [Restoration Queue](#restoration-queue)
7. [Commands](#commands)
8. [GameRules](#gamerules)
9. [Config File](#config-file)
10. [Server Setup](#server-setup)
11. [FAQ](#faq)

---

## Overview

AuraCraft gives each player a persistent **load** of status effects called **auras**. Players spend points to choose and upgrade auras through an in-game UI. Points and aura selections are saved per-player and survive restarts, world reloads, and server shutdowns.

---

## How the Point System Works

Every player's current state is represented by their **load**:

```
load = selected auras + unspent points
```

Both invested auras and banked points count toward the same capacity cap (`max_auras`, default 3). A player who has selected 2 auras and holds 1 unspent point is at maximum load for the default cap.

Players begin their first session with 1 point. Picking an aura converts 1 point into an active aura slot. Picking the same aura again upgrades it by one level (e.g. Speed I → Speed II), consuming an additional point.

### Load vs Capacity

| State | Auras | Points | Load | Cap | Can gain more? |
|---|---|---|---|---|---|
| Fresh player | 0 | 1 | 1 | 3 | Yes |
| Picked 1 aura | 1 | 0 | 1 | 3 | Yes (needs point) |
| Full (points) | 0 | 3 | 3 | 3 | No |
| Full (auras) | 3 | 0 | 3 | 3 | No |

---

## Items

### Aura Plus

- **Stack size:** 1
- **Source:** Drops from killed players (if `drop_plus_on_kill` is on), crafted with a Heart of the Sea (if `enable_heart_recipe` is on), or granted by admins via `/aura plus`
- **Not consumed in creative mode**

**Right-click behavior:**

1. If the player has auras in their **restoration queue**, the top entry is popped off and that aura is automatically re-selected — no picker, no point spend. A confirmation message is shown.
2. If the restoration queue is empty, the player gains +1 point. If they are not at full capacity, the picker prompt appears.
3. Does nothing if the player is already at full load.

---

### Aura Reset

- **Stack size:** 8
- **Source:** Crafted or admin-given
- **Not consumed in creative mode**
- **Requires at least one previously selected aura** — if a player has never picked an aura, the item refuses to activate (prevents multi-use abuse before any points are spent).

**Right-click behavior:**

1. Checks the player has at least 1 aura in their history. If not, shows a warning and does nothing.
2. Removes all selected auras and refunds all points (invested + unspent), capped at `max_auras`.
3. Clears all active aura effects.
4. Consumes 1 item from the stack.
5. Plays a sound and confirms how many points were refunded.

---

## Aura Picker UI

**Default keybind:** `Y`

The picker can always be opened, even with 0 unspent points. Browsing is always available.

### Left Panel — Aura List

Shows all auras enabled in `auracraft.json`. Each row displays:
- Aura name and icon
- Current level / max upgrade level
- Whether it is already selected

### Right Panel — Detail View

Click any aura to see:
- Full description
- Whether it is chosen and at what level
- A tooltip hint: "Choose this Aura to activate it" or "Choose again to upgrade"

### Choose Button

The Choose button activates or upgrades the selected aura. It is **grayed out** if:
- No aura is selected in the list
- The selected aura is already at max upgrade level
- The player has 0 unspent points

### Points Display

The picker always shows your current unspent points in the top corner.

---

## PvP Rules

When a player is killed by another player:

1. **If the victim has auras:** The most recently added aura entry is removed. The removed aura ID is pushed onto the victim's **restoration queue**.
2. **If the victim has no auras but has points:** 1 point is deducted.
3. **If the victim has nothing:** No penalty.

If `drop_plus_on_kill` is enabled, the killer receives one **Aura Plus** item.

The victim receives a colored message describing what was lost.

---

## Restoration Queue

The restoration queue is a per-player, NBT-persisted LIFO (last-in, first-out) list of aura IDs. It tracks auras lost in PvP or withdrawn via `/aura withdraw`.

### How auras enter the queue

- **PvP death:** The victim's most recent aura entry is pushed to the front of the queue.
- **`/aura withdraw [amount]`:** Each aura removed during the withdraw is pushed to the queue. Unspent points that are withdrawn do **not** enter the queue (they had no aura attached).

### How auras leave the queue

Using an **Aura Plus** item checks the queue first. If the queue is non-empty and the top entry is a valid, enabled aura, it is popped off and automatically re-applied. The player does not need to open the picker.

### Clearing the queue

The restoration queue is cleared when:
- `/aura restart [player]` is used (hard reset)

The queue is **not** cleared by `/aura reset` or by using the Aura Reset item. If you want a player to start completely fresh with no restoration memory, use `/aura restart`.

---

## Commands

All commands require **op level 2** (GameMaster).

### `/aura`

Toggles the AuraCraft UI on or off server-wide. When disabled, the picker prompt does not appear and the keybind cannot open the screen. Persists across restarts via `SavedData`.

---

### `/aura status [player]`

Prints a formatted summary:
```
Status
Player: [Steve]
Points: [2]
Aura/s: [minecraft:speed, minecraft:strength]
```

Defaults to the command sender if no player is specified.

---

### `/aura reset [player]`

Refunds all invested aura points back as unspent points. The total refunded is capped at `max_auras`. Clears all active aura effects.

This mirrors what the **Aura Reset item** does. Intended as the admin-level version of the same action.

---

### `/aura restart [player]`

Hard reset. Sets the player back to exactly 1 point, clears all aura selections, clears all aura effects, and **clears the restoration queue**. Use when you want the player to start fresh as if they just joined for the first time.

---

### `/aura plus [player] [amount]`

Grants the specified number of extra points to the player. Subject to the `max_auras` cap — will not push the player over capacity.

---

### `/aura remove [player] [amount]`

Removes points or auras from a player. Processes unspent points first, then removes aura entries if points run out.

---

### `/aura withdraw [amount]`

Self-only command. Converts a player's own points or auras into **Aura Plus** items placed in their inventory. Processes unspent points first, then removes aura entries.

Each removed aura is pushed to the restoration queue, so using the resulting Aura Plus items restores them in reverse order automatically.

If `amount` is omitted, withdraws 1.

---

## GameRules

Set in-game with `/gamerule <rule> <value>`. Listed in the **AuraCraft** category in the game rules screen.

| Rule | Type | Default | Description |
|---|---|---|---|
| `max_auras` | Integer | `3` | Maximum load per player (selected auras + unspent points combined) |
| `drop_plus_on_kill` | Boolean | `true` | Whether a killed player drops an Aura Plus for the killer |
| `enable_heart_recipe` | Boolean | `true` | Whether the Heart of the Sea → Aura Plus crafting recipe is available |

---

## Config File

Located at `config/auracraft.json`. Generated on first run with all vanilla mob effects pre-populated.

### `enabledAuras`

A map of mob effect IDs to booleans. Only effects listed as `true` appear in the picker UI.

```json
"enabledAuras": {
  "minecraft:speed": true,
  "minecraft:strength": true,
  "minecraft:blindness": false
}
```

If an effect is not listed, it defaults to `true`.

### `auraUpgradeLevels`

A map of mob effect IDs to their maximum amplifier level. `1` means no upgrades (only base level available). `2` means one upgrade step (e.g. Speed I → Speed II), and so on.

```json
"auraUpgradeLevels": {
  "minecraft:speed": 2,
  "minecraft:strength": 1
}
```

If an effect is not listed, it defaults to `1`.

---

## Server Setup

1. Install AuraCraft on the server **and on every client that connects**.
2. Players connecting without AuraCraft, or with a mismatched protocol version, are kicked with a descriptive message.
3. Adjust `max_auras`, `drop_plus_on_kill`, and `enable_heart_recipe` with `/gamerule` as needed.
4. Edit `config/auracraft.json` to enable/disable specific auras and set upgrade caps. Restart the server after changes.
5. Use `/aura` to toggle the UI globally (e.g. during setup or events).

---

## FAQ

**Q: A player died to another player but didn't lose anything. Why?**
A: PvP must be enabled on the level. The victim must also have at least 1 point or 1 aura — if they have nothing, no penalty is applied.

**Q: Aura Plus isn't restoring the expected aura.**
A: The restoration queue is LIFO — the most recently lost or withdrawn aura is restored first. Use `/aura status` to check current auras.

**Q: Can the picker be opened if the client doesn't have the mod installed?**
A: No. AuraCraft requires the client mod. The server validates the protocol version on join and disconnects clients that are missing or out of date.

**Q: Can I disable the Aura Plus recipe but still give the item?**
A: Yes. Set `enable_heart_recipe false` and use `/aura plus [player] [amount]` or `/give`.

**Q: What happens if `max_auras` is reduced while players are already at the old cap?**
A: Existing players keep their current load. They simply cannot gain new points or auras until their load drops below the new cap naturally (PvP loss, withdraw, reset, etc.).

**Q: Does `/aura reset` clear the restoration queue?**
A: No. Only `/aura restart` clears it. Reset only refunds points.

**Q: Can a player open the picker with 0 points?**
A: Yes. The UI always opens, allowing players to browse and read descriptions. The Choose button is disabled until they have a point to spend.

**Q: What is the difference between `/aura reset` and `/aura restart`?**
A: `reset` refunds all points (same as the Aura Reset item). `restart` is a hard wipe — player returns to 1 point with no auras, no effects, and no restoration queue.
