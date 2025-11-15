# Changelog

All notable changes to the SoulSpeedHarness plugin will be documented in this file.

## [1.0.0] - 2024

### Added

- Initial release of SoulSpeedHarness plugin
- Soul Speed enchantment support for Happy Ghast harnesses
- Speed boost system with three levels:
  - Level 1: 40% flying speed increase
  - Level 2: 80% flying speed increase
  - Level 3: 120% flying speed increase
- Support for all 16 harness color variants (Black, Blue, Brown, Cyan, Gray, Green, Light Blue, Light Gray, Lime, Magenta, Orange, Pink, Purple, Red, White, Yellow)
- Event-based performance optimization (EntityMountEvent, EntityDismountEvent)
- Periodic speed updates every 5 ticks for active riders
- Automatic modifier cleanup on dismount and player quit

### Fixed

- Happy Ghast entity recognition (now uses EntityType.HAPPY_GHAST instead of instanceof Ghast)
- Harness detection for all color types
- Compatibility with both Happy Ghasts and regular Ghasts

### Technical

- Modern Paper API usage (EntityType, NamespacedKey for AttributeModifier)
- Deprecated API replacements (Registry.ENCHANTMENT, NamespacedKey-based AttributeModifier)
- Clean code structure with helper methods (isHappyGhast, isHarness)
