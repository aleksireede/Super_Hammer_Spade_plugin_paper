# Hammer

A hammer is a tool that breaks blocks in a 3x3 plane. It is a special type of pickaxe, so it is used on pickaxe-breakable blocks.

A spade is the shovel equivalent and breaks shovel-mineable blocks in the same 3x3 plane.

There is a hammer and a spade for every tool tier: wooden, stone, iron, golden, diamond, and netherite.

## Features
- Unlockable recipes
- Craftable, repairable, enchantable, and smith-able
- Respects Unbreaking, Fortune, and Silk Touch enchantments
- Displays breaking animation for all affected blocks
- Configurable gameplay values (e.g. hunger rate, block hardness range)
- Toggleable system to limit Efficiency level on custom area tools for game balance (disabled by default)
- No permissions management required*
- Admin command to give players hammers or spades (`/givehammer`)
- Can be easily retextured using CustomModelData -- values are editable in the config to prevent conflicts

## Recipe
![Recipe for crafting a hammer tool.](https://www.dropbox.com/scl/fi/cfvaqb8j2h8aaa9tjkto7/hammer-crafting.png?rlkey=evirio5wkibkj9xr79eks4f7j&st=q02hx9ul&dl=1)

The recipe shown above is for the diamond hammer, but the pattern is the same for every tier of hammers except Netherite.

Spades use a different recipe shape and are based on the matching shovel tier.

![Recipe for crafting a spade tool.](https://www.dropbox.com/scl/fi/ler3ot7b2b2abify5s4sx/spade_crafting.png?rlkey=07ia8ja5ftkmqlrcbhmysi4fa&st=sfty70rr&dl=1)

Netherite hammers and spades are upgraded from their diamond versions, just like Vanilla tools.

## Published To
- ~~[Spigot](https://www.spigotmc.org/resources/hammer.119392/)~~
- ~~[Paper](https://hangar.papermc.io/FullPotato/Hammer)~~
- ~~[Modrinth](https://modrinth.com/plugin/craftablehammers)~~


## Installation
1. Simply drop the JAR file into your plugins folder, and you should be good to go! There are no dependencies you have to install.

2. Verify that the plugin was added properly with /plugins (optional)

## Build
This project uses Maven, targets Paper, and requires Java 21.

Install the shared library first:

```bash
mvn -f hammer-shared-lib/pom.xml install
```

Then build the plugin jar:

```bash
mvn package
```

The packaged jar will be created under `target/Hammer.jar`.

## Permissions
To use the `/givehammer` command, the player must have the permission node: `hammer.givehammer`

## Footnotes

This plugin is inspired by but in no way affiliated with the Tinker's Construct mod or its creator(s).

*All players can craft and use hammers and spades with no permissions required. Only the give command requires permission.# Super_Hammer_Spade_plugin_paper
