# SoulSpeedHarness

A Paper plugin that enables Soul Speed enchantment on Happy Ghast harnesses, significantly increasing their flying speed.

## Features

- Soul Speed enchantment support for Happy Ghast harnesses
- Increases flying speed based on Soul Speed level
- Supports all 16 harness color variants
- Event-based performance optimization
- Compatible with Paper 1.21.10

## Speed Boosts

- **Soul Speed I**: 40% faster flying speed
- **Soul Speed II**: 80% faster flying speed
- **Soul Speed III**: 120% faster flying speed

## Installation

1. Copy the JAR file to your server's `plugins/` folder
2. Restart the server or use `/reload`
3. The plugin will activate automatically

## Usage

1. Obtain a Soul Speed enchanted book (e.g., through bartering with Piglins)
2. Use an anvil to enchant a harness with Soul Speed
3. Place the enchanted harness on a Happy Ghast
4. Ride the Happy Ghast - it will fly significantly faster!

## Building

```bash
cd plugins/SoulSpeedHarness
./compile.sh
```

The compiled JAR will be in `build/libs/SoulSpeedHarness-1.0.0.jar`

## How It Works

- On mount: Plugin checks if the Happy Ghast has a harness with Soul Speed
- Speed modifier: Automatically applied to the `FLYING_SPEED` attribute
- Automatic cleanup: Modifier is removed when player dismounts

## Performance

The plugin uses:

- Event-based logic (`EntityMountEvent`, `EntityDismountEvent`)
- Periodic updates (every 5 ticks = 0.25 seconds) for active riders
- No tick loops for all players

This is significantly more efficient than datapack solutions with tick functions.

## Compatibility

- **Server**: Paper 1.21.10+
- **Entities**: Happy Ghasts and regular Ghasts
- **Harnesses**: All 16 color variants supported

## License

MIT License - see [LICENSE](LICENSE) file for details
