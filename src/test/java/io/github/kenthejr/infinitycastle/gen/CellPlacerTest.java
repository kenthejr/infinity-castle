package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CellPlacerTest {
	/** A sparse world: (x, y, z) packed into a long. */
	private static final class World {
		final Map<Long, Piece> blocks = new HashMap<>();

		static long key(int x, int y, int z) {
			return ((long) (y + 4096) << 40) | ((long) (x & 0xFFFFF) << 20) | (z & 0xFFFFF);
		}

		void set(int x, int y, int z, Piece piece) {
			this.blocks.put(key(x, y, z), piece);
		}

		Piece get(int x, int y, int z) {
			return this.blocks.getOrDefault(key(x, y, z), Piece.AIR);
		}
	}

	@Test
	void lowerHalfIsPlacedAsDrawn() {
		CellPlan plan = new CellPlan(ModuleType.HALL, Openings.ALL, 0L);
		Canvas canvas = Modules.build(plan);
		World world = new World();
		CellPlacer.place(plan, canvas, 3, Half.LOWER, world::set);
		int base = CastleGeometry.floorBase(3);
		canvas.forEach((x, y, z, piece) -> assertEquals(piece, world.get(x, base + y, z)));
	}

	@Test
	void upperHalfIsMirroredAndFlipped() {
		CellPlan plan = new CellPlan(ModuleType.STAIRWELL, Openings.ALL, 0L);
		Canvas canvas = Modules.build(plan);
		World world = new World();
		CellPlacer.place(plan, canvas, 3, Half.UPPER, world::set);
		int top = CastleGeometry.floorBase(3) + CastleGeometry.FLOOR_HEIGHT - 1;
		canvas.forEach((x, y, z, piece) -> assertEquals(piece.flipVertical(), world.get(x, top - y, z)));
	}

	/** The key invariant of the flip: after placement, every lantern hangs from or stands on a real block. */
	@Test
	void lanternsStaySupportedInBothHalves() {
		CastleLayout layout = new CastleLayout(8L);
		World world = new World();
		for (int cx = -3; cx <= 3; cx++) {
			for (int cz = -3; cz <= 3; cz++) {
				int ox = cx * 16;
				int oz = cz * 16;
				CellPlacer.placeColumn(layout, cx, cz, (x, y, z, piece) -> world.set(ox + x, y, oz + z, piece));
			}
		}
		int[] lanterns = {0};
		world.blocks.forEach((key, piece) -> {
			if (piece.material() != Material.PAPER_LANTERN) {
				return;
			}
			int y = (int) (key >>> 40) - 4096;
			int x = (int) ((key >>> 20) & 0xFFFFF);
			int z = (int) (key & 0xFFFFF);
			x = x >= 0x80000 ? x - 0x100000 : x;
			z = z >= 0x80000 ? z - 0x100000 : z;
			Piece support = world.get(x, piece.hanging() ? y + 1 : y - 1, z);
			assertTrue(support.material().isSolid(), "unsupported lantern at " + x + "," + y + "," + z + " hanging=" + piece.hanging());
			lanterns[0]++;
		});
		assertTrue(lanterns[0] > 50);
	}

	@Test
	void cellsNeverOverlapAndStayInsideTheCastle() {
		CastleLayout layout = new CastleLayout(21L);
		for (int cx = -2; cx <= 2; cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				World world = new World();
				CellPlacer.placeColumn(layout, cx, cz, (x, y, z, piece) -> {
					assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16);
					assertTrue(y >= CastleGeometry.MIN_Y && y < CastleGeometry.MAX_Y, "y out of range: " + y);
					assertNull(world.blocks.get(World.key(x, y, z)), "two cells wrote to " + x + "," + y + "," + z);
					world.set(x, y, z, piece);
				});
			}
		}
	}

	@Test
	void sidewaysChambersOnlyUseBlocksThatCanLieSideways() {
		CellPlan plan = new CellPlan(ModuleType.SIDEWAYS_CHAMBER, Openings.NONE, 3L);
		World world = new World();
		CellPlacer.place(plan, Modules.build(plan), 5, Half.LOWER, world::set);
		assertFalse(world.blocks.isEmpty());
		int base = CastleGeometry.floorBase(5);
		world.blocks.forEach((key, piece) -> {
			Material.Shape shape = piece.material().shape();
			assertFalse(shape == Material.Shape.STAIRS || shape == Material.Shape.SLAB || shape == Material.Shape.LANTERN || shape == Material.Shape.CONNECTING, piece.toString());
			int y = (int) (key >>> 40) - 4096;
			assertTrue(y >= base && y < base + CastleGeometry.HALF_HEIGHT);
		});
	}

	/**
	 * Each stairwell's landing faces its mirror image across the equator with a four-block gap: tall enough for a
	 * player to stand, short enough that a jump carries their centre over the equator.
	 */
	@Test
	void gravityGateGeometry() {
		CellPlan plan = new CellPlan(ModuleType.STAIRWELL, Openings.NONE, 0L);
		World world = new World();
		int floor = 4;
		CellPlacer.place(plan, Modules.build(plan), floor, Half.LOWER, world::set);
		CellPlacer.place(plan, Modules.build(plan), floor, Half.UPPER, world::set);
		int equator = (int) CastleGeometry.equatorY(floor);
		int lowerLandingTop = equator - 2;
		int upperLandingBottom = equator + 2;
		assertEquals(Material.DECK, world.get(8, lowerLandingTop - 1, 12).material());
		assertEquals(Material.DECK, world.get(8, upperLandingBottom, 12).material());
		for (int y = lowerLandingTop; y < upperLandingBottom; y++) {
			assertTrue(world.get(8, y, 12).isAir());
		}
	}
}
