# AuraCraft

> A Fabric mod for Minecraft that gives players a persistent, point-based aura system — choose your status effects, upgrade them over time, and defend them in PvP.

---

## What is AuraCraft?

AuraCraft lets players permanently equip status effects as **auras** using a point economy. Instead of brewing potions, players spend points to lock in the effects they want and can upgrade those effects by reinvesting in the same one. Points and selections persist across sessions, making your aura a meaningful part of your character build.

On PvP servers, auras become a stake: dying to another player can cost you an aura or a point, and killers are rewarded with an Aura Plus drop.

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | `26.1.2` |
| Fabric Loader | `0.19.1+` |
| Fabric API | `0.145.4+26.1.2` |
| Java | `25+` |

> **Note:** AuraCraft must be installed on both the server and the client. Mismatched or missing client installs are rejected on join.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft `26.1.2`.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your `mods/` folder.
3. Download the latest AuraCraft release and place it in your `mods/` folder on both server and client.
4. Launch the game. A `config/auracraft.json` file will be generated on first run.

---

## Gameplay

- Players use the picker UI (**Y** by default) to choose auras.
- Press **Y** at any time to browse available auras, even with no points to spend.

### The Point Economy

Every player starts with 1 point. The total **load** is always:

```
selected auras + unspent points ≤ max_auras
```

Holding unspent points and spending them on auras count against the same cap.

### Choosing and Upgrading Auras

- Press **Y** to open the **Aura Picker**.
- Click an aura to select it. This consumes 1 point and activates the effect permanently.
- Click the **same aura again** to upgrade its amplifier (e.g. Speed I → Speed II). Each upgrade costs 1 additional point, up to the configured max.
- The Choose button is grayed out when you have no points.

### Special Items

| Item | Effect |
|---|---|
| `Aura Plus` | Restores your most recently lost/withdrawn aura first; otherwise grants +1 point |
| `Aura Reset` | Refunds all points (invested + unspent), capped at max capacity |

---

## PvP Rules

When you die to another player:

- If you have selected auras, your most recent aura entry is removed.
- If you have no auras but have unspent points, you lose 1 point instead.
- The removed aura is remembered. Using an **Aura Plus** item restores it automatically.
- If `drop_plus_on_kill` is enabled, the killer receives an **Aura Plus** drop.

---

## Commands

All commands require **GameMaster** (op level 2).

| Command | Description |
|---|---|
| `/aura` | Toggles the AuraCraft UI on/off for all players |
| `/aura status [player]` | Shows a player's current points and selected auras |
| `/aura reset [player]` | Refunds all points (invested + unspent), capped at max |
| `/aura restart [player]` | Hard resets a player to 1 point, clearing all auras |
| `/aura plus [player] [amount]` | Grants the player extra points |
| `/aura remove [player] [amount]` | Removes points or auras from a player |
| `/aura withdraw [amount]` | Converts points/auras into Aura Plus items |

---

## Configuration

The config file is generated at `config/auracraft.json` on first launch.

### GameRules

Configured in-game via `/gamerule` under the **AuraCraft** category.

| GameRule | Default | Description |
|---|---|---|
| `max_auras` | `3` | Combined load cap (selected auras + unspent points) |
| `drop_plus_on_kill` | `true` | Whether killing a player drops an Aura Plus |
| `enable_heart_recipe` | `true` | Whether the Heart of the Sea crafting recipe is active |

### Config File (`auracraft.json`)

| Key | Description |
|---|---|
| `enabledAuras` | Per-aura enable/disable toggle |
| `auraUpgradeLevels` | Max upgrade level per aura |

Example:
```json
{
  "enabledAuras": {
    "minecraft:speed": true,
    "minecraft:strength": true,
    "minecraft:blindness": false
  },
  "auraUpgradeLevels": {
    "minecraft:speed": 2,
    "minecraft:strength": 1
  }
}
```

---

## License

MIT - see [LICENSE](LICENSE).
