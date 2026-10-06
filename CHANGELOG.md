# Changelog

## 0.1.0 — unreleased

First release.

- Castle dimension built from twelve hand-designed rooms (plain, banded, veranda and veranda-banded styles in three
  footprints) and double-sided bridges and staircases, laid out procedurally on a grid of 32-block cells across sixteen
  double-sided floors that wrap vertically. Designs are imported from a Minecraft world with `tools/DesignImport.java`.
- Gravity flips when crossing a floor's equator. Players walk on ceilings, with mirrored stepping, jumping, fall damage
  and eye height. Each stairwell's spiral stair leads to a roof staircase up to the gravity gate.
- Camera rolls with gravity; mouse and strafe controls mirror while upside down; per-floor tilt and drifting gravity.
- Skyless dimension with ember-red fog, warm block light and a red–amber colour grade.
- Blocks: fusuma, tatami, shoji wall, shoji screen, lacquered planks, paper lantern. Item: biwa.
- Commands: `/infinitycastle enter|leave|where|stairwell`.
- English and Japanese translations.
