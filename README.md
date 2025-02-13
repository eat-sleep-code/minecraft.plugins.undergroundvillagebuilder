# UndergroundVillageBuilder Minecraft (Nukkit) Plugin

Build a large underground village complete with apartments, fields, crops, and a crafting area.   A large open lower level is included.

## Prerequisites
- [Nukkit Minecraft Server](https://github.com/PetteriM1/NukkitPetteriM1Edition/releases)

## Installation 
- Place the `UndergroundVillageBuilder.jar` file in the `<Nukkit Installation Folder>/plugins/` folder.

## Usage

- Create a underground village directly in front of the player:

  `/undergroundvillage`

## Known Issues

- When gravel, sand, water, or lava is found within the build area it can sometimes overwhelm the creation of parts of the village.   This will result in some debris that needs to be cleaned up. 

## Building Project

Run `mvn clean package`.   The output will be saved to `/target/UndergroundVillageBuilder.jar` 