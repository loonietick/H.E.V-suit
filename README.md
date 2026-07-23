# HEV Suit Sound System

Adds Half-Life 1 hud and full hev suit sound effects to minecraft with a bunch of additional features

## Features

### Health & damage alerts
Voiced alerts as your health drops, with distinct lines at each threshold (near-death, critical, seek-medical-attention, and an earlier "vital signs dropping" warning), plus a flatline sound.
- **Fractures** — fall damage, minor or major depending on how hard you land
- **Blood loss** — arrow, fireball, or other projectile hits
- **Internal bleeding** — explosion damage
- **Lacerations** — melee hits from hostile mobs (creepers excluded)
- **Heat damage** — fire, lava, or other heat sources
- **Shock damage** — lightning strikes
- **Chemical damage** — poison or wither effects
- **Morphine** — cosmetic alert on a single large hit
- **Insufficient medical supplies** — warns when your health is low and you're not carrying food or potions
- **HEV logon** — plays when you equip a chestplate named starting with "HEV"

### Environmental
- **Radiation** — Basalt Deltas biome is treated as a radiation zone: a Geiger-counter loop plays while you're in one, with a one-time "radiation detected" alert on entry
- **Totem of Undying** — detected via its status effect signature and treated as emergency medical intervention (an "administering medical" alert, followed by the morphine sound)

### Flashlight (requires [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights))
A real dynamic-lighting flashlight with battery drain and recharge, not just a HUD icon. Defaults to a Half-Life 1–style point light at your crosshair target; an experimental Half-Life 2–style cone floodlight attached to the player is available as a toggle. Default keybind: **G**.

### HUD
Health, armor (with power percentage), ammo, directional damage indicators, and hostile-mob threat indicators, each independently toggleable. Colors are fully customizable. A "chatty suit" mode reverts alert cooldowns to older, more frequent timing if you prefer that over the more accurate one.

### Config
Open the config screen with the **H** key, or through [Mod Menu](https://modrinth.com/mod/modmenu) if installed.

## Dependencies
- **[Fabric API](https://modrinth.com/mod/fabric-api)** — required
- **[Mod Menu](https://modrinth.com/mod/modmenu)** — optional, adds an in-game config screen
- **[LambDynamicLights](https://modrinth.com/mod/lambdynamiclights)** — optional, enables the flashlight functionality.

## Build from source
- Compile like any other Gradle project
- Select a target version with `-PmcVersion=<version>`, e.g. `./gradlew build -PmcVersion=1.21.11`
- Supported versions: `1.20.1`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`
- Output jar lands in `./build/libs/`
