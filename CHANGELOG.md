# Changelog

All notable changes to this project are documented in this file.

## [1.2.23] - 2026-04-18

### Added
- Mod Menu config screen with `General` and `Effects` tabs.
- Join handshake gate for missing/outdated client mod versions.
- `/aura withdraw` command for voluntary token withdrawal.
- Optional numeric amount support for `/aura plus` and `/aura remove`.
- `icon.png` support in mod assets/metadata.

### Changed
- Rebrand from `EffectSMP` to `AuraCraft`:
  - Mod ID and namespace changed to `auracraft`.
  - Root command changed to `/aura`.
  - Item IDs renamed:
    - `auracraft:effect_plus` -> `auracraft:aura_plus`
    - `auracraft:effect_reset` -> `auracraft:aura_reset`
  - Item names renamed to `Aura Plus` and `Aura Reset`.
- PvP behavior updated:
  - `Aura Plus` can drop even when victim has no selected effects.
  - If victim has 1 selected effect and extra tokens, death removes 1 token instead of effect.
- Token capacity behavior updated:
  - `tokens + selected effects` is now enforced against `maxEffects`.
- Audio behavior updated:
  - Token gain now uses level-up sound.
  - Effect selection/upgrade now uses beacon power select sound.

### Fixed
- Effect selection/config UI layout overlap and scroll issues.
- Runtime effect availability refresh after config toggles.
- Multiplayer `Aura Plus` drop condition reliability.
- Token gain sound propagation to recipient/nearby players.

## [1.2.1] - 2026-04-16

### Added
- Early stable token/effect flow and item crafting integration.

## [1.1.8] - 2026-04-16

### Added
- Initial EffectSMP prototype and base gameplay loop.
