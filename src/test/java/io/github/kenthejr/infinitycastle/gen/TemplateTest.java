package io.github.kenthejr.infinitycastle.gen;

import static io.github.kenthejr.infinitycastle.gen.CastleGeometry.DECK_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TemplateTest {
	static List<String> designs() {
		return Designs.ALL;
	}

	@Test
	void parsesBlockStates() {
		assertEquals(Piece.stairs(Material.SPRUCE_STAIRS, Dir.EAST, true), Template.parsePiece("spruce_stairs[facing=east,half=top]"));
		assertEquals(Piece.slab(Material.OAK_SLAB, false), Template.parsePiece("oak_slab[type=bottom]"));
		assertEquals(Piece.lantern(true), Template.parsePiece("lantern[hanging=true]"));
		assertEquals(Piece.axis(Material.STRIPPED_BIRCH_LOG, Axis.Y), Template.parsePiece("stripped_birch_log[axis=y]"));
		assertEquals(Piece.fusuma(Dir.WEST), Template.parsePiece("fusuma[facing=west]"));
		assertEquals(Piece.of(Material.SPRUCE_PLANKS), Template.parsePiece("spruce_planks"));
	}

	@ParameterizedTest
	@MethodSource("designs")
	void everyDesignFitsACellWithRoomForBridges(String name) {
		Template design = Designs.get(name);
		// Odd sizes have a centre block, so the door in the middle of each side lands on the cell's centre line.
		assertEquals(1, design.sizeX() % 2, name + " is not an odd width");
		assertEquals(1, design.sizeZ() % 2, name + " is not an odd depth");
		assertTrue(design.sizeX() <= CastleGeometry.CELL_SIZE - 4 && design.sizeZ() <= CastleGeometry.CELL_SIZE - 4, name + " leaves no room for bridges");
		assertTrue(DECK_Y + design.sizeY() < Modules.LANDING_Y, name + " is too tall");
	}

	/** Nobody should fall out of a room into the void: every design's floor is complete. */
	@ParameterizedTest
	@MethodSource("designs")
	void everyDesignHasAFloorAndARoof(String name) {
		Template design = Designs.get(name);
		int roof = design.sizeY() - 1;
		for (int x = 0; x < design.sizeX(); x++) {
			for (int z = 0; z < design.sizeZ(); z++) {
				assertTrue(design.get(x, 0, z).material().isSolid(), name + ": hole in the floor at " + x + "," + z);
			}
		}
		assertTrue(design.get(1, roof, 1).material().isSolid(), name + " has no roof");
	}

	@ParameterizedTest
	@MethodSource("designs")
	void turningADesignMovesItsNorthDoorEastAndFourTurnsIsIdentity(String name) {
		Template design = Designs.get(name);
		Template once = design.rotated(1);
		assertEquals(design.sizeX(), once.sizeZ());
		assertEquals(design.sizeZ(), once.sizeX());
		assertEquals(design.door(Dir.NORTH).size(), once.door(Dir.EAST).size());
		assertEquals(design.door(Dir.WEST).size(), once.door(Dir.NORTH).size());
		// The middle of the north wall ends up in the middle of the east wall, turned with it.
		int cx = (design.sizeX() - 1) / 2;
		assertEquals(design.get(cx, 1, 0).rotateY(1), once.get(once.sizeX() - 1, 1, (once.sizeZ() - 1) / 2));
		Template full = design.rotated(4);
		Template around = once.rotated(3);
		for (int y = 0; y < design.sizeY(); y++) {
			for (int z = 0; z < design.sizeZ(); z++) {
				for (int x = 0; x < design.sizeX(); x++) {
					assertEquals(design.get(x, y, z), full.get(x, y, z));
					assertEquals(design.get(x, y, z), around.get(x, y, z), name + " differs after four turns at " + x + "," + y + "," + z);
				}
			}
		}
	}

	@ParameterizedTest
	@MethodSource("designs")
	void slidingADoorOpenClearsAWalkwayToTheRoom(String name) {
		Template design = Designs.get(name);
		int cx = (design.sizeX() - 1) / 2;
		int cz = (design.sizeZ() - 1) / 2;
		for (Dir side : Dir.values()) {
			Canvas closed = new Canvas();
			design.stamp(closed, 0, 0, 0, Openings.NONE);
			Canvas open = new Canvas();
			design.stamp(open, 0, 0, 0, new Openings(side == Dir.NORTH, side == Dir.EAST, side == Dir.SOUTH, side == Dir.WEST));
			assertFalse(design.door(side).isEmpty(), name + " has no door patch on " + side);
			// Walk in from the edge along the centre line until well inside the room: floor underfoot, headroom above.
			boolean hitWall = false;
			for (int depth = 0; depth < cx - 1 && depth < cz - 1; depth++) {
				int x = side.axis() == Axis.X ? (side == Dir.WEST ? depth : design.sizeX() - 1 - depth) : cx;
				int z = side.axis() == Axis.Z ? (side == Dir.NORTH ? depth : design.sizeZ() - 1 - depth) : cz;
				assertTrue(open.get(x, 0, z).material().isSolid(), name + " " + side + ": no floor at depth " + depth);
				for (int y = 1; y <= Modules.DOOR_HEIGHT; y++) {
					assertTrue(open.get(x, y, z).isAir(), name + " " + side + ": blocked at depth " + depth + ", y=" + y + " by " + open.get(x, y, z));
					hitWall |= !closed.get(x, y, z).isAir();
				}
			}
			assertTrue(hitWall, name + " " + side + ": the closed door is not a wall");
		}
	}

	@ParameterizedTest
	@MethodSource("designs")
	void everyDesignHasASpiralStairToItsRoofHatch(String name) {
		Template design = Designs.get(name);
		int cx = (design.sizeX() - 1) / 2;
		int cz = (design.sizeZ() - 1) / 2;
		int roof = design.sizeY() - 1;
		assertEquals(Material.STRIPPED_BIRCH_LOG, design.get(cx, 1, cz).material(), name + ": no newel post");
		boolean hatch = false;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				hatch |= design.get(cx + dx, roof, cz + dz).isAir();
			}
		}
		assertTrue(hatch, name + ": no hatch in the roof");
	}
}
