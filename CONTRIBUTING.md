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
| `gen` | The procedural generator. Pure Java, with no Minecraft imports allowed, so it stays unit-testable. |
| `gravity` | Gravity rules, floor profiles, camera easing, and the mirrored collision logic |
| `world` | Chunk generator, block palette, teleporting |
| `mixin`, `client.mixin` | Hooks into vanilla. Keep these thin and put logic in the classes they call. |
| `tools/TextureGen.java` | All textures are generated from code. Edit and rerun it rather than editing PNGs by hand. |

### Adding a new module

1. Add a value to `ModuleType` and decide when `CastleLayout.plan` picks it.
2. Draw it in `Modules`, upright, with its doorways on `Openings.DOOR_MIN..DOOR_MAX` and at least `Modules.DOOR_HEIGHT`
   blocks of headroom. Draw it in a canonical orientation and use `Canvas.frame(turns)` for the rest.
3. Stick to `Material`s. If you need a new block, add a `Material` and map it in `CastlePalette`.
4. Run the tests. `ModulesTest` and `CellPlacerTest` check doorways, lantern support and the upside-down flip for
   every module automatically.

## Before opening a pull request

- `./gradlew build` passes (unit tests and server game tests).
- If you changed anything visual or camera-related, run `./gradlew runClientGameTest` and look at the screenshots in
  `build/run/clientGameTest/screenshots`.
- Match the surrounding style: tabs, and short comments that explain *why*.
- Keep pull requests focused. One feature or fix per PR is easiest to review.

## Reporting bugs

Please use the bug report template and include your Minecraft, Fabric Loader, Fabric API and mod versions, plus
`logs/latest.log` if anything crashed.
