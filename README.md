# Infinity Castle

An endless castle of sliding shoji screens, vermilion bridges and upside-down rooms, for Minecraft 26.3 on Fabric.
Gravity and the camera flip as you move between floors.

Inspired by the **Infinity Castle (無限城, *Mugen-jō*)** from *Demon Slayer: Kimetsu no Yaiba*. This is an unofficial
fan project and isn't affiliated with or endorsed by the creators or rights holders.

![A view across the castle's void](docs/images/vista.png)

## Features

- **A procedurally generated castle.** Every world seed produces a different castle. Halls, dead-end screen rooms,
  corridors, bridges, lone pavilions and stairwells are laid out on a grid and connected by doorways that always line
  up. Gaps between them open onto the void.
- **Floors with two faces.** Each floor has upright rooms on its lower deck and a second set of rooms hanging upside
  down from the deck above. Sixteen floors are stacked, and they wrap: fall off the bottom and you tumble back in at the
  top.
- **Gravity that flips.** Halfway between the two decks of each floor is an *equator*. Cross it and gravity reverses,
  so you fall "up" onto the ceiling and walk around the hanging rooms. Every floor has **stairwells** that climb to a
  landing just below the equator. Jump from the landing and you flip onto its mirror image above.
- **A camera that follows.** When gravity flips, the view rolls over smoothly, and mouse and strafe controls mirror so
  they still match what you see. Some floors hold the camera at a slight uneasy tilt, and on "drifting" floors gravity
  is lighter.
- **No sky, no horizon.** The dimension has no skybox, sun, stars or clouds. Everything fades into ember-red fog, so
  the castle seems to go on forever in every direction.
- **The castle's red–amber glow.** Warm lantern-coloured block light, plus a colour-grading post effect that
  split-tones shadows toward crimson and highlights toward amber, and darkens the corners of the screen.
- **Japanese architecture.** Tatami, back-lit shoji walls and screens, paper lanterns (chōchin), lacquered vermilion
  timber, dark-wood columns and tiled hipped roofs. Floors are named in Japanese as you enter them (第九層, "ninth floor").

| Entrance hall | Stairwell landing | After the flip |
| --- | --- | --- |
| ![Entrance hall](docs/images/entrance.png) | ![Below the gravity gate](docs/images/gate-below.png) | ![Standing on the ceiling](docs/images/gate-inverted.png) |

## Getting in

Craft a **biwa** and use it to be drawn into the castle; use it again inside to be released back to where you came
from.

```
  S      S = string
 ES      E = ender pearl
PP       P = dark oak planks
```

Operators can use commands instead:

| Command | What it does |
| --- | --- |
| `/infinitycastle enter [players]` | Draws players into the castle's entrance hall |
| `/infinitycastle leave [players]` | Returns players to where they entered from |
| `/infinitycastle where` | Names your floor, half, gravity direction and the room you're in (anyone can use this) |
| `/infinitycastle stairwell` | Takes you to the nearest gravity gate on your floor |

The building blocks (tatami, shoji wall, shoji screen, lacquered planks, paper lantern) are craftable and appear in
their own creative tab.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5 or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and the Infinity Castle jar in your `mods` folder.
3. The mod is needed on both the client and the server.

## Client settings

The camera effects can be disorienting, so each one can be turned off in `config/infinitycastle-client.json`:

| Setting | Default | Effect |
| --- | --- | --- |
| `smoothFlip` | `true` | Animate the camera roll when gravity flips instead of snapping |
| `rollCameraWhenInverted` | `true` | Turn the view upside down while standing on a ceiling |
| `mirrorControlsWhenInverted` | `true` | Mirror mouse and strafe input while the view is upside down |
| `floorTilt` | `true` | Allow the constant tilt on some floors |
| `colorGrade` | `true` | The red–amber colour grade and vignette |

## How it works

The generator is pure Java with no Minecraft dependencies, so it can be unit-tested directly:

```
gen/        CastleLayout     decides each cell's module from the seed and shared-edge hashes
            Modules          draws each module upright into a 16×24×16 Canvas
            CellPlacer       places canvases into the world, flipping upper-half cells upside down
gravity/    GravityRules     equator-crossing rules, slab stickiness and vertical wrapping
            FloorProfile     per-floor gravity scale, camera tilt and Japanese floor names
            RollAnimator     easing for the camera roll
world/      CastleChunkGenerator, CastlePalette (materials → block states), CastleTeleporter
mixin/      collision, stepping, fall damage, jumping and eye height for inverted gravity
client/     camera roll, mirrored controls, upside-down player rendering, colour grade
```

Gravity uses the vanilla `minecraft:gravity` attribute. A negative value makes an entity fall upward, and vanilla
already handles the movement, syncing and anti-flight checks for that. The mixins then mirror the rest of the physics:
landing on a ceiling counts as being on the ground, jumping pushes away from it, stepping onto a slab works in reverse,
and falling upward still does fall damage. Eye height is mirrored too, so the camera and every raycast sit where your
head actually is.

Gravity flips only when your centre crosses an equator. It does *not* flip when you fall through the slab between two
floors: that boundary has upright rooms on one side and inverted rooms on the other, and flipping there would trap you
bouncing between them.

## Building from source

You need JDK 25.

```sh
./gradlew build              # compile, unit tests and server game tests → build/libs/
./gradlew runClient          # play in a development client
./gradlew runClientGameTest  # end-to-end client test; screenshots land in build/run/clientGameTest/screenshots
java tools/TextureGen.java   # regenerate the textures from code
```

### Tests

- **Unit tests** (`src/test`) cover the layout, modules and gravity rules. For example, they check that neighbouring
  cells agree on every shared edge, every doorway has a floor and headroom, and every lantern is still supported after
  the upside-down flip. The stairwell is walked with a climbing search to prove it reaches the landing. There is also a
  tick-by-tick simulation with vanilla's jump and gravity constants showing a jump from the landing carries you through
  the gravity gate and back, and the mirrored step logic is tested against real Minecraft collision shapes.
- **Server game tests** (`src/gametest`) run inside a real server. They check that generated chunks match the pure
  layout block for block, the entrance is safe, entering and leaving round-trips a player, gravity flips at the equator
  with mirrored eye height, and an inverted player lands on a ceiling and jumps away from it.
- **Client game test** runs a real client. It enters the castle, stands on a stairwell landing, presses jump, and
  asserts the player ends up inverted on the landing above with the camera rolled over, then jumps back.

## Known limitations

- Gravity only flips up and down. The sideways rooms are scenery; true wall-walking would need a full gravity-changer
  rewrite of player physics.
- Only players are affected by the castle's gravity. Dropped items and mobs fall normally, so items dropped in the
  upper half fall away into the void.
- Sneaking doesn't stop you walking off a ceiling edge.
- The castle has no inhabitants yet.

See [CONTRIBUTING.md](CONTRIBUTING.md) if you'd like to help with any of these.

## License

[MIT](LICENSE). *Demon Slayer*, *Kimetsu no Yaiba* and related names belong to their respective owners.
