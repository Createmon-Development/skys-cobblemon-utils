# Item Registration Guide

This guide walks you through adding a new custom item (like a gym badge) to the mod.

## Overview

Adding a new item requires 4 simple steps:
1. Register the item in Java code
2. Add a translation (display name)
3. Create an item model JSON
4. Add the texture PNG file

## Step-by-Step: Adding a New Gym Badge

Let's add a "Rainbow Badge" as an example.

### Step 1: Register the Item

Open `src/main/java/com/skys/cobblemonutilsmod/items/ModItems.java`

Add this line with the other badge registrations:

```java
public static final DeferredItem<Item> RAINBOW_BADGE = ITEMS.register("rainbow_badge",
    () -> new GymBadgeItem(new Item.Properties()));
```

**Important**:
- The string `"rainbow_badge"` is your item ID
- Use lowercase and underscores only
- This ID will be used everywhere else

### Step 2: Add Translation

Open `src/main/resources/assets/skyscobblemonutilsmod/lang/en_us.json`

Add this line (don't forget the comma on the previous line):

```json
{
  "item.skyscobblemonutilsmod.boulder_badge": "Boulder Badge",
  "item.skyscobblemonutilsmod.cascade_badge": "Cascade Badge",
  "item.skyscobblemonutilsmod.thunder_badge": "Thunder Badge",
  "item.skyscobblemonutilsmod.rainbow_badge": "Rainbow Badge",
  "item.skyscobblemonutilsmod.gym_badge.tooltip": "A prestigious gym badge"
}
```

**Format**: `"item.skyscobblemonutilsmod.YOUR_ITEM_ID": "Display Name"`

### Step 3: Create Item Model

Create a new file at:
`src/main/resources/assets/skyscobblemonutilsmod/models/item/rainbow_badge.json`

With this content:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "skyscobblemonutilsmod:item/rainbow_badge"
  }
}
```

**Important**:
- File name must match item ID
- The texture path points to your PNG file

### Step 4: Add Texture

Create your 16x16 pixel PNG texture and save it to:
`src/main/resources/assets/skyscobblemonutilsmod/textures/item/rainbow_badge.png`

See `SPRITE_GUIDE.md` for detailed texture creation instructions.

## Testing Your Item

1. Build the mod:
   ```
   gradlew.bat build
   ```

2. Copy the JAR from `build/libs/` to your Minecraft mods folder

3. Launch Minecraft

4. Give yourself the item:
   ```
   /give @s skyscobblemonutilsmod:rainbow_badge
   ```

## Adding Different Types of Items

### Custom Item Class

If you want different behavior than a gym badge, create a new item class:

```java
public class MyCustomItem extends Item {
    public MyCustomItem(Properties properties) {
        super(properties);
    }

    // Add custom functionality here
}
```

Then register it:

```java
public static final DeferredItem<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new MyCustomItem(new Item.Properties()));
```

### Item Properties

You can customize item properties:

```java
public static final DeferredItem<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new Item(new Item.Properties()
        .stacksTo(64)                    // Stack size (default 64)
        .rarity(Rarity.RARE)             // COMMON, UNCOMMON, RARE, EPIC
        .fireResistant()                 // Doesn't burn in lava/fire
    ));
```

## Complete Example Checklist

When adding "rainbow_badge":

- [x] Added to `ModItems.java`: `RAINBOW_BADGE = ITEMS.register("rainbow_badge", ...)`
- [x] Added to `en_us.json`: `"item.skyscobblemonutilsmod.rainbow_badge": "Rainbow Badge"`
- [x] Created `models/item/rainbow_badge.json`
- [x] Created `textures/item/rainbow_badge.png` (16x16 PNG)
- [x] Built mod: `gradlew.bat build`
- [x] Tested in-game: `/give @s skyscobblemonutilsmod:rainbow_badge`

## Common Mistakes

### Item doesn't appear in game
- Check you registered it in `ModItems.java`
- Make sure the mod compiled successfully
- Verify the JAR is in your mods folder

### Item has wrong name
- Check the translation key in `en_us.json`
- Format must be: `"item.skyscobblemonutilsmod.ITEM_ID": "Name"`

### Purple/black checkerboard texture
- Texture file is missing or named incorrectly
- Check file path: `textures/item/ITEM_ID.png`
- Verify PNG is exactly 16x16 pixels

### Item shows as "item.skyscobblemonutilsmod.item_id"
- Translation is missing from `en_us.json`
- Check for JSON syntax errors (missing comma, etc.)

## Need Help?

1. Check the example badges (boulder, cascade, thunder)
2. Read `SPRITE_GUIDE.md` for texture help
3. Read `BUILDING.md` for build troubleshooting
4. Make sure all file names match exactly (case-sensitive!)
