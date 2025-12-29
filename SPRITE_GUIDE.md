# Custom Sprite Creation Guide

This guide explains how to create and add custom sprites (textures) for items in this mod.

## Quick Start

1. Create a 16x16 pixel PNG image
2. Save it to `src/main/resources/assets/skyscobblemonutilsmod/textures/item/your_item_name.png`
3. Register the item in `ModItems.java`
4. Create the model JSON file
5. Add translation in language file
6. Build and test!

## Detailed Texture Requirements

### Technical Specifications

| Property | Requirement |
|----------|-------------|
| File Format | PNG (.png) |
| Dimensions | 16×16 pixels |
| Color Mode | RGBA (32-bit with alpha channel) |
| DPI | 72 or higher |
| Compression | PNG standard (lossless) |

### File Location

All item textures must be placed in:
```
src/main/resources/assets/skyscobblemonutilsmod/textures/item/
```

### Naming Convention

- **Lowercase only**: `boulder_badge.png` ✓, `Boulder_Badge.png` ✗
- **Underscores for spaces**: `thunder_badge.png` ✓, `thunder badge.png` ✗
- **No special characters**: Only letters, numbers, and underscores
- **Match your item ID**: If registered as "mega_badge", file must be "mega_badge.png"

## Recommended Tools

### Free Tools

1. **Piskel** (https://www.piskelapp.com/)
   - Web-based, no installation needed
   - Specifically designed for pixel art
   - Export directly as PNG

2. **GIMP** (https://www.gimp.org/)
   - Full-featured image editor
   - Free and open-source
   - Available on Windows, Mac, Linux

3. **Paint.NET** (https://www.getpaint.net/)
   - Windows only
   - Simple and user-friendly
   - Good for beginners

### Paid Tools

1. **Aseprite** ($19.99)
   - Industry-standard for pixel art
   - Animation support
   - Highly recommended for serious sprite work

2. **Photoshop**
   - Professional tool
   - Good for complex designs
   - Subscription-based

## Design Tips

### For Gym Badges

1. **Keep it simple**: At 16x16, details are hard to see
2. **Use bold colors**: Helps visibility in inventory
3. **Consider a border**: 1-2 pixel border helps definition
4. **Use the whole space**: Don't make sprites too small
5. **Think about color theory**: Contrasting colors stand out

### Minecraft Style Guidelines

Minecraft has a distinctive art style. To match it:

- Use a limited color palette (not too many colors)
- Avoid gradients (use distinct color bands instead)
- Use dithering sparingly for texture
- Keep shapes geometric and clear
- Use muted/desaturated colors rather than bright neon

### Transparency

- Use transparency (alpha channel) for non-square shapes
- Fully transparent pixels (alpha = 0) for empty space
- Avoid semi-transparent pixels (causes issues in-game)
- Most badge designs work well as opaque squares or circles

## Step-by-Step Creation Process

### Using Piskel (Web-based)

1. Go to https://www.piskelapp.com/
2. Click "Create Sprite"
3. Set size to 16×16 (in the resize tool)
4. Design your sprite using the drawing tools
5. Export as PNG:
   - Click "Export"
   - Choose "Download PNG"
   - Save to the textures/item folder

### Using GIMP

1. Open GIMP
2. Create new image: File → New
3. Set dimensions to 16×16 pixels
4. Set "Fill with: Transparency"
5. Use Pencil tool (not Paintbrush) for pixel-perfect drawing
6. Zoom in (400%+) for easier editing
7. Export as PNG:
   - File → Export As
   - Choose PNG
   - Save to textures/item folder

### Using Paint.NET

1. Open Paint.NET
2. New: 16×16 pixels
3. Set Grid: View → Grid → Set to 1×1 pixels
4. Use Pencil tool for drawing
5. Zoom in for better visibility
6. Save as PNG to textures/item folder

## Testing Your Sprites

1. **Build the mod**: Run `./gradlew build`
2. **Copy to mods folder**: Copy the JAR from `build/libs/` to your Minecraft mods folder
3. **Launch Minecraft** with NeoForge
4. **Check in-game**:
   - Use `/give @s skyscobblemonutilsmod:your_item_id`
   - Check inventory to see your sprite
   - If you see purple/black checkerboard: texture not found (check file name/location)

## Common Issues

### Purple/Black Checkerboard

**Problem**: Missing texture
**Solutions**:
- Check file name matches item ID exactly
- Verify file is in correct folder
- Ensure file extension is .png (not .PNG or .jpg)
- Rebuild mod after adding texture

### Sprite Looks Blurry

**Problem**: Image is not exactly 16×16 pixels
**Solutions**:
- Recreate at exact 16×16 size
- Don't upscale smaller images
- Use "Nearest Neighbor" resampling if resizing

### Sprite Doesn't Appear

**Problem**: Model JSON not set up correctly
**Solutions**:
- Verify model JSON exists and is named correctly
- Check JSON syntax is valid
- Ensure texture path in model matches file location

## Example Sprites

The mod includes three example badges. Check these files for reference:
- `textures/item/boulder_badge.png` (you'll need to create this)
- `textures/item/cascade_badge.png` (you'll need to create this)
- `textures/item/thunder_badge.png` (you'll need to create this)

## Next Steps

After creating your sprites:

1. Register the item in `ModItems.java`
2. Create the model JSON file
3. Add translation in `lang/en_us.json`
4. Build and test
5. Share your creations!

## Resources

- Minecraft Wiki: https://minecraft.wiki/w/Resource_Pack
- Color Palette Generator: https://lospec.com/palette-list
- Pixel Art Community: https://www.reddit.com/r/PixelArt/
