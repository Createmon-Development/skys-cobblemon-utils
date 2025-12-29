# Sky's Cobblemon Utils

A quality of life utility mod for Cobblemon that adds useful features for server gameplay.

## Features

### 1. Battle Aggro Protection
Players in Pokemon battles will not be targeted by hostile mobs. This includes:
- Automatic aggro clearing when entering a battle
- Prevention of new aggro while in battle
- Works for all battle types (wild, trainer, PvP)

### 2. Custom Item System
Easy-to-use infrastructure for adding custom items with sprites, including example gym badges.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.72+
- Cobblemon 1.6.0+

## Building

Run `./gradlew build` to build the mod. The output JAR will be in `build/libs/`.

## Development

### Adding New Gym Badges

1. **Register the item** in `ModItems.java`:
```java
public static final DeferredItem<Item> YOUR_BADGE = ITEMS.register("your_badge_id",
    () -> new GymBadgeItem(new Item.Properties()));
```

2. **Add translation** in `assets/skyscobblemonutilsmod/lang/en_us.json`:
```json
"item.skyscobblemonutilsmod.your_badge_id": "Your Badge Name"
```

3. **Create item model** at `assets/skyscobblemonutilsmod/models/item/your_badge_id.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "skyscobblemonutilsmod:item/your_badge_id"
  }
}
```

4. **Add texture** at `assets/skyscobblemonutilsmod/textures/item/your_badge_id.png`

See the "Adding Custom Sprites" section below for texture requirements.

## Adding Custom Sprites

### Texture Requirements

**File Format:** PNG (Portable Network Graphics)
**Dimensions:** 16x16 pixels (standard Minecraft item texture size)
**Color Depth:** 32-bit RGBA (supports transparency)
**Location:** `src/main/resources/assets/skyscobblemonutilsmod/textures/item/`

### File Naming Convention

- Use lowercase letters only
- Use underscores for spaces (e.g., `boulder_badge.png`)
- File name must match the item ID registered in `ModItems.java`

### Creating Textures

1. **Use a pixel art editor** like:
   - Aseprite (paid)
   - Piskel (free, web-based)
   - GIMP (free)
   - Paint.NET (free, Windows)

2. **Design guidelines**:
   - Keep it simple and recognizable at small size
   - Use clear, bold colors
   - Minecraft uses a somewhat muted color palette
   - Consider using an outline or border for visibility
   - Use transparency for non-rectangular shapes

3. **Export settings**:
   - Format: PNG
   - Size: 16x16 pixels
   - Preserve alpha channel (transparency)
   - No compression artifacts

### Example Folder Structure

```
src/main/resources/assets/skyscobblemonutilsmod/
├── textures/
│   └── item/
│       ├── boulder_badge.png    (16x16 PNG)
│       ├── cascade_badge.png    (16x16 PNG)
│       └── thunder_badge.png    (16x16 PNG)
├── models/
│   └── item/
│       ├── boulder_badge.json
│       ├── cascade_badge.json
│       └── thunder_badge.json
└── lang/
    └── en_us.json
```

### Placeholder Textures

If you don't have textures ready, you can use placeholder images:
1. Create a 16x16 PNG with a solid color
2. Add text or simple shapes to distinguish badges
3. Replace with final artwork later

The mod will load with missing textures (showing the purple/black checkerboard pattern), but it's recommended to at least add placeholder images for testing.

## License

MIT License
