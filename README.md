# HEV Suit

### This mod adds multiple HEV suit sound effects from Half-Life (and Black Mesa) and a simple HUD to Minecraft.

## Sound Effects

- **Fractures** – Plays a sound when you fall and lose a certain amount of health.
- **Blood Loss** – Triggered by damage from arrows or fireballs.
- **Health Monitoring** – Alerts for different health conditions, including critical health, near death, and seek medical attention.
- **Heat Damage** – Triggers from fire or high temperatures.
- **Electrical Damage** – Plays when struck by lightning.
- **Hazardous Chemical** – Alerts for poison or wither effects.
- **Power Level Tracking** – Based on your armor durability and type of armor (elytras are excluded from durability calculation).
- **Armor Durability Tracking** – Alerts when your armor is low; elytras play a unique sound.
- **Morphine SFX** – Plays a morphine sound (purely cosmetic) only if you take 6 damage points (3 hearts) or more at once.
- **HEV Logon** – Plays when you equip any chestplate with a name starting with "HEV".
- **Black Mesa HEV Suit Sounds** – Optional alternate sound effects. Some sound effects and features are disabled when toggled on.
- **and more! check the changelog for up the up to date features**
## Features

### **HUD & Display**
- **Toggle individual HUD elements** (`/hev toggle hud <element>`)
  - `all`, `health`, `armor`, `ammo`, `damageindicators`, `mobindicators`
- **Change HUD color** (`/hev edit hud color <hexcolor>`)
  - Reset with `/hev edit hud color reset`

### **Sound & System Controls**
- **Toggle voice alerts** (`/hev toggle voice <alert>`)
  - Includes: `morphine`, `armordurability`, `fractures`, `heatdamage`, `bloodloss`, `shockdamage`, `chemicaldamage`, `health_critical2`, `seek_medical`, `health_critical`, `near_death`
- **Toggle certain features that affect the entire suit** (`/hev toggle systems <option>`)
  - Options: `pvp`, `blackmesasfx`, `all`
- **List current sound queue** (`/hev listqueue`)
- **Clear sound queue** (`/hev clearqueue`)
- **Manually queue a sound** (`/hev queuesound <sound>`)

### **PvP Mode**
- `/hev toggle systems pvp` – Disables all HEV suit systems except health monitoring and the HUD. This is so the HEV suit only tells you the needed alerts to survive (e.g., you won’t be told you’re on fire when someone uses a fire aspect sword).

## Building for Multiple Versions

This project now builds against both Minecraft 1.21.7 and the backported 1.21.4.

- Default target comes from `gradle.properties` (`minecraft_version=1.21.7`).
- Override per build using the `mcVersion` property:
  - Build for 1.21.7:
    `./gradlew build -PmcVersion=1.21.7`
  - Build for 1.21.4:
    `./gradlew build -PmcVersion=1.21.4`

The Gradle script selects compatible Yarn mappings, Fabric Loader and Fabric API versions for each target.
You can update the mapping in `build.gradle` (see the `supported` map near the top) when bumping versions.
