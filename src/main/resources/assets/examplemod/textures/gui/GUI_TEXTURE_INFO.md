# GUI Texture Information

## Required GUI Textures

### mob_spawner.png
- Size: 176x202 pixels
- Contains:
  - Main background panel
  - Spawn egg slot (80, 35)
  - 6 module slots in 3x2 grid starting at (8, 60)
  - Player inventory slots starting at (8, 120)
  - Player hotbar slots at (8, 178)

### spawn_egg_mob_spawner.png  
- Size: 176x166 pixels
- Contains:
  - Main background panel
  - Spawn egg slot
  - Player inventory slots

## Temporary Solution
Until proper PNG textures are created, the game will use default slot backgrounds.
The slots are positioned correctly and functional even without custom textures.

## Module Slot Layout
```
[M1] [M2] [M3]
[M4] [M5] [M6]
```
Where M1-M6 are the 6 module slots positioned at:
- M1: (8, 60)   M2: (26, 60)   M3: (44, 60)
- M4: (8, 78)   M5: (26, 78)   M6: (44, 78)
