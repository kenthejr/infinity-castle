# Contributing

Thanks for wanting to add a room to the castle. Bug reports, ideas and pull requests are all welcome.

## Getting set up

1. Install JDK 25 (for example [Temurin](https://adoptium.net/)).
2. Clone the repository and run `./gradlew build`. The first build downloads and sets up Minecraft, which takes a few
   minutes.
3. Open the folder in IntelliJ IDEA (recommended) or any editor with Gradle support. `./gradlew genSources` gives you
   readable Minecraft sources to browse.
4. `./gradlew runClient` starts a development client with the mod loaded.

## Where things live

| Package | Contents |
| --- | --- |
| `gen` | The generator. Pure Java, with no Minecraft imports allowed, so it stays unit-testable. |
| `gen/Designs`, `gen/Template` | The hand-built designs: which modules use which, and how a design is read and turned |
| `gravity` | Gravity rules, floor profiles, camera easing, and the mirrored collision logic |
| `world` | Chunk generator, block palette, teleporting |
| `mixin`, `client.mixin` | Hooks into vanilla. Keep these thin and put logic in the classes they call. |
| `tools/TextureGen.java` | All textures are generated from code. Edit and rerun it rather than editing PNGs by hand. |
| `tools/DesignImport.java` | Reads the design world and writes the templates in `src/main/resources/data/infinitycastle/designs`. |

### Adding or changing a design

The buildings are built in Minecraft, not in code. The design world (`Infinity castle _ Designs.zip`, kept outside
git) is a flat creative world; open it with any launcher that can load a 1.20.1 world.

1. Build the room on the grass. Give it an odd width and depth of at most 27, so it has a centre block and leaves room
   for bridges in a 32-block cell, and keep it under 19 blocks tall. Put the doorway in the middle of each side: the
   bridges are five blocks wide and arrive on the cell's centre line, and the deck they arrive on is the room's bottom
   layer. The generator relies on a spiral stair at the centre and a hatch in the roof for stairwells, so include them.
2. Place a structure block one block below the room's north-west corner, set to the room's size, exactly like the
   existing ones. The importer reads that region.
3. Copy the room thirty blocks straight up and slide the centre panels of each side open in the copy. The importer
   diffs the two copies and records, for each side, what changes when its door opens.
4. Add the room to `DESIGNS` in `tools/DesignImport.java` with the structure block's position and size, then run
   `java tools/DesignImport.java "Infinity castle _ Designs.zip"`.
5. Add its name to the lists in `Designs` that should use it (`SMALL`, `LARGE`, `LONG`, `TALL`, `SIDEWAYS`). Designs in
   `TALL` must be 9 blocks high for the roof staircase to reach the landing inside the half.
6. Run the tests. `TemplateTest` checks every design has a complete floor, a roof hatch, a door on each side and a
   clear walkway through it; `ModulesTest` and `CellPlacerTest` check the bridges meet the doors, lanterns stay
   supported after the upside-down flip, and the stairwell can be climbed.

If a design needs a block the palette doesn't have, add a `Material` (named after the block), map it in
`CastlePalette`, teach `DesignImport.State.vocabulary()` any renaming it needs, and give `Piece` the orientation rules
for it (flipping, turning and tipping over).

### Changing the bridges or staircases

Those are drawn in `Modules` from the repeating pattern they were designed with, so they can be any length. Keep them
double-sided: everything above the walkway is mirrored below it.

## Before opening a pull request

- `./gradlew build` passes (unit tests and server game tests).
- If you changed anything visual or camera-related, run `./gradlew runClientGameTest` and look at the screenshots in
  `build/run/clientGameTest/screenshots`.
- Match the surrounding style: tabs, and short comments that explain *why*.
- Keep pull requests focused. One feature or fix per PR is easiest to review.

## Reporting bugs

Please use the bug report template and include your Minecraft, Fabric Loader, Fabric API and mod versions, plus
`logs/latest.log` if anything crashed.
