package io.github.kenthejr.infinitycastle.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CellPlacerTest {
	private static final int CELL = CastleGeometry.CELL_SIZE;

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
				int ox = cx * CELL;
				int oz = cz * CELL;
				CellPlacer.placeCell(layout, cx, cz, (x, y, z, piece) -> world.set(ox + x, y, oz + z, piece));
			}
		}
		int[] lanterns = {0};
		world.blocks.forEach((key, piece) -> {
			if (piece.material() != Material.LANTERN) {
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
				CellPlacer.placeCell(layout, cx, cz, (x, y, z, piece) -> {
					assertTrue(x >= 0 && x < CELL && z >= 0 && z < CELL);
					assertTrue(y >= CastleGeometry.MIN_Y && y < CastleGeometry.MAX_Y, "y out of range: " + y);
					assertNull(world.blocks.get(World.key(x, y, z)), "two cells wrote to " + x + "," + y + "," + z);
					world.set(x, y, z, piece);
				});
			}
		}
	}

	/** The four chunks of a cell together hold exactly the cell, each in its own quarter. */
	@Test
	void chunksAreQuartersOfTheirCell() {
		CastleLayout layout = new CastleLayout(13L);
		int cx = -1;
		int cz = 2;
		World cell = new World();
		CellPlacer.placeCell(layout, cx, cz, cell::set);
		World chunks = new World();
		for (int dx = 0; dx < CastleGeometry.CHUNKS_PER_CELL; dx++) {
			for (int dz = 0; dz < CastleGeometry.CHUNKS_PER_CELL; dz++) {
				int ox = dx * 16;
				int oz = dz * 16;
				CellPlacer.placeChunk(layout, cx * CastleGeometry.CHUNKS_PER_CELL + dx, cz * CastleGeometry.CHUNKS_PER_CELL + dz, (x, y, z, piece) -> {
					assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16, "chunk-local coordinate out of range: " + x + "," + z);
					chunks.set(ox + x, y, oz + z, piece);
				});
			}
		}
		assertFalse(cell.blocks.isEmpty());
		assertEquals(cell.blocks, chunks.blocks);
	}

	@Test
	void sidewaysChambersOnlyUseBlocksThatCanLieSideways() {
		CellPlan plan = new CellPlan(ModuleType.SIDEWAYS_CHAMBER, Openings.NONE, 3L);
		World world = new World();
		CellPlacer.place(plan, Modules.build(plan), 5, Half.LOWER, world::set);
		assertFalse(world.blocks.isEmpty());
		int base = CastleGeometry.floorBase(5);
		Set<Material.Shape> sideways = Set.of(Material.Shape.CUBE, Material.Shape.AXIS, Material.Shape.PANEL);
		world.blocks.forEach((key, piece) -> {
			assertTrue(sideways.contains(piece.material().shape()), piece.toString());
			int y = (int) (key >>> 40) - 4096;
			assertTrue(y >= base + CastleGeometry.DECK_Y && y < base + CastleGeometry.HALF_HEIGHT, "sideways chamber outside its half at y=" + y);
		});
	}

	/** A sideways chamber keeps its whole floor: nothing of the design is clipped away by the tipping. */
	@Test
	void sidewaysChambersFitInsideTheHalf() {
		for (long variant = 0; variant < 4; variant++) {
			CellPlan plan = new CellPlan(ModuleType.SIDEWAYS_CHAMBER, Openings.NONE, variant);
			Canvas canvas = Modules.build(plan);
			int[] drawn = {0};
			canvas.forEach((x, y, z, piece) -> drawn[0]++);
			int[] placed = {0};
			CellPlacer.place(plan, canvas, 5, Half.LOWER, (x, y, z, piece) -> placed[0]++);
			int[] dropped = {0};
			canvas.forEach((x, y, z, piece) -> dropped[0] += piece.tipOverX().isAir() ? 1 : 0);
			assertEquals(drawn[0] - dropped[0], placed[0], "variant " + variant + " was clipped");
		}
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
		int x = Modules.LANDING_CENTER_X;
		int z = Modules.LANDING_CENTER_Z;
		assertEquals(Material.SPRUCE_PLANKS, world.get(x, lowerLandingTop - 1, z).material());
		assertEquals(Material.SPRUCE_PLANKS, world.get(x, upperLandingBottom, z).material());
		for (int y = lowerLandingTop; y < upperLandingBottom; y++) {
			assertTrue(world.get(x, y, z).isAir());
		}
	}
}
