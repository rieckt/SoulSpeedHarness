# SoulSpeedHarness

A Paper plugin that enables Soul Speed enchantment on Happy Ghast harnesses, significantly increasing their flying speed.

## Features

- Soul Speed enchantment support for Happy Ghast harnesses
- Increases flying speed based on Soul Speed level
- Supports all 16 harness color variants
- Event-based performance optimization
- Compatible with Paper 1.21.10+

## Speed Boosts

- **Soul Speed I**: 40% faster flying speed
- **Soul Speed II**: 80% faster flying speed
- **Soul Speed III**: 120% faster flying speed

## Installation

1. Download the latest JAR from [Releases](https://github.com/rieckt/SoulSpeedHarness/releases) or [Modrinth](https://modrinth.com/plugin/soulspeedharness)
2. Copy the JAR file to your server's `plugins/` folder
3. Restart the server or use `/reload`
4. The plugin will activate automatically

## Usage

1. Obtain a Soul Speed enchanted book (e.g., through bartering with Piglins)
2. Use an anvil to enchant a harness with Soul Speed
3. Place the enchanted harness on a Happy Ghast
4. Ride the Happy Ghast - it will fly significantly faster!

The speed boost is automatically applied when you mount the Happy Ghast and removed when you dismount.

## Building

### Using Gradle (Recommended)

```bash
cd plugins/SoulSpeedHarness
./gradlew build
```

The compiled JAR will be in `build/libs/SoulSpeedHarness-1.0.0.jar`

### Requirements

- Java 21 or higher
- Gradle (included via wrapper)

## How It Works

- On mount: Plugin checks if the Happy Ghast has a harness with Soul Speed
- Speed modifier: Automatically applied to the `FLYING_SPEED` attribute
- Automatic cleanup: Modifier is removed when player dismounts
- Periodic updates: Speed is recalculated every 5 ticks for active riders

## Performance

The plugin uses:

- Event-based logic (`EntityMountEvent`, `EntityDismountEvent`)
- Periodic updates (every 5 ticks = 0.25 seconds) for active riders only
- No tick loops for all players
- Efficient attribute modifier management

This is significantly more efficient than datapack solutions with tick functions.

## Compatibility

- **Server**: Paper 1.21.10+ (or compatible Spigot/Paper forks)
- **Entities**: Happy Ghasts (`EntityType.HAPPY_GHAST`) and regular Ghasts (`EntityType.GHAST`)
- **Harnesses**: All 16 color variants supported (Black, Blue, Brown, Cyan, Gray, Green, Light Blue, Light Gray, Lime, Magenta, Orange, Pink, Purple, Red, White, Yellow)

## Support

If you enjoy this plugin, consider supporting the development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/rieckt)

## Links

- [GitHub](https://github.com/rieckt/SoulSpeedHarness)
- [Modrinth](https://modrinth.com/plugin/soulspeedharness)
- [Issues](https://github.com/rieckt/SoulSpeedHarness/issues)
