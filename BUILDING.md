# Building and Running Sky's Cobblemon Utils

## Prerequisites

- Java 21 JDK (https://adoptium.net/)
- Gradle (included via wrapper)

## Building the Mod

### Windows

```batch
gradlew.bat build
```

### Linux/Mac

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/skys-cobblemon-utils-1.0.0.jar`

## Running in Development

### Client (with GUI)

```batch
gradlew.bat runClient
```

### Server (no GUI)

```batch
gradlew.bat runServer
```

## Installing the Mod

1. Build the mod (see above)
2. Copy `build/libs/skys-cobblemon-utils-1.0.0.jar` to your Minecraft mods folder
3. Make sure you have NeoForge 21.1.72+ and Cobblemon 1.6.0+ installed
4. Launch Minecraft

## Troubleshooting

### "Could not find cobblemon"

Make sure the Cobblemon maven repository is accessible. If you're behind a firewall or proxy, you may need to configure Gradle to use it.

### "Java version mismatch"

This mod requires Java 21. Check your Java version with:
```
java -version
```

Download Java 21 from https://adoptium.net/ if needed.

### "Missing texture" in game

Add PNG texture files to `src/main/resources/assets/skyscobblemonutilsmod/textures/item/`

See SPRITE_GUIDE.md for details on creating textures.

## Development Setup

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open → Select the `skys-cobblemon-utils` folder
3. Wait for Gradle sync to complete
4. Run configurations will be auto-generated

### Eclipse

1. Run `gradlew.bat eclipse` to generate Eclipse project files
2. Import project in Eclipse
3. Build will happen automatically

### VS Code

1. Install Java Extension Pack
2. Open folder
3. Gradle tasks will be available in the sidebar

## Clean Build

To clean all build artifacts:

```batch
gradlew.bat clean
```

Then rebuild:

```batch
gradlew.bat build
```
