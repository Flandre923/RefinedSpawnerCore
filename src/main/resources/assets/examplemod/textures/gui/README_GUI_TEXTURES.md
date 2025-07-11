# GUI Textures

This directory contains GUI texture files for the mod.

## Required Textures

### spawn_egg_mob_spawner.png
- Size: 176x166 pixels
- Purpose: GUI background for the spawn egg mob spawner interface
- Layout:
  - Single slot at (80, 35) for spawn egg
  - Standard player inventory at bottom
  - Title area at top

### mob_spawner.png (if not already present)
- Size: 176x166 pixels  
- Purpose: GUI background for the debug mob spawner configuration interface

## How to Create Textures

1. **Quick Method**: Copy vanilla chest GUI texture from Minecraft assets
2. **Custom Method**: Create your own 176x166 PNG with appropriate slot layouts
3. **Temporary**: Use any existing GUI texture as placeholder

## Texture Sources

You can extract vanilla GUI textures from:
- Minecraft client jar file
- Online Minecraft asset repositories
- Texture pack resources

## Installation

1. Create or obtain the PNG files
2. Name them exactly as specified above
3. Place in this directory
4. Remove placeholder text files
5. Test in-game

The mod will work without custom textures but may show missing texture errors until proper PNG files are provided.
