# AuraCraft

AuraCraft is a Fabric mod for Minecraft that adds persistent, token-based effect selection and upgrades with server-side PvP and config controls.

## Requirements

- Minecraft `26.1.2`
- Fabric Loader `0.19.1+`
- Fabric API `0.145.4+26.1.2`
- Java `25+`

## Gameplay

- Players use the picker UI (`Y` by default) to choose effects.
- Choosing an effect consumes a token.
- Choosing the same effect again upgrades its amplifier until duplicate cap.
- `Aura Plus` grants extra token capacity.
- `Aura Reset` clears selected effects and returns repick tokens.
- Token budget is capped by combined load: `selected effects + available tokens <= maxEffects`.

## PvP Rules

- PvP deaths remove effects based on config (`PvPEffectsLostOnDeath`).
- If victim has exactly 1 selected effect and extra tokens, death removes 1 token instead of the effect.
- If victim had no selected effects, a drop can still occur (when enabled).
- Killers can receive `Aura Plus` drops when configured.

## Commands

Root command: `/aura`

- `/aura` toggles AuraCraft UI enabled/disabled (GameMaster)
- `/aura status [player]` shows tokens/effects state (GameMaster)
- `/aura reset [player]` resets selections for repick (GameMaster)
- `/aura plus [player] [amount]` grants tokens (GameMaster)
- `/aura remove [player] [amount]` removes tokens (GameMaster)
- `/aura withdraw` lets a player voluntarily remove 1 token

## Config

- File: `config/auracraft.json`
- In-game config via Mod Menu:
  - `General`: `PvPEffectsLostOnDeath`, `maxDuplicateAmplifierBonus`
  - `Effects`: per-effect enable/disable toggles

Disabled effects are hidden from selection UI.

## Compatibility Gate

Server requires compatible AuraCraft client during join handshake.

## Items

- `auracraft:aura_plus` (`Aura Plus`)
- `auracraft:aura_reset` (`Aura Reset`)

## License

This project is licensed under the MIT License. See `LICENSE`.
