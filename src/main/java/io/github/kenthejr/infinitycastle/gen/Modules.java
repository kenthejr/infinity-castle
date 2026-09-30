package io.github.kenthejr.infinitycastle.gen;

import static io.github.kenthejr.infinitycastle.gen.Openings.DOOR_MAX;
import static io.github.kenthejr.infinitycastle.gen.Openings.DOOR_MIN;

/**
 * Builds the architecture for each {@link ModuleType} into a {@link Canvas}.
 *
 * <p>All modules are drawn upright (deck at y = 0). Upper-half cells are later flipped by {@link CellPlacer}, which is
 * what makes rooms hang from the ceiling. Doorways always span {@link Openings#DOOR_MIN}..{@link Openings#DOOR_MAX} on
 * an edge and are at least {@link #DOOR_HEIGHT} blocks tall, so any two modules can sit side by side.
 */
public final class Modules {
	public static final int DOOR_HEIGHT = 4;

	/** Height of the stairwell landing block. Its top face is where the jump across the equator starts. */
	public static final int LANDING_Y = 21;

	/** A spot in the middle of the stairwell landing, in cell coordinates, clear of railings and posts. */
	public static final int LANDING_CENTER_X = 8;
	public static final int LANDING_CENTER_Z = 12;

	private static final int LAST = Canvas.SIZE_X - 1;

	private Modules() {
	}

	public static Canvas build(CellPlan plan) {
		Canvas canvas = new Canvas();
		switch (plan.type()) {
			case VOID -> {
			}
			case ENTRANCE -> hall(canvas, plan.openings(), plan.variant(), true);
			case HALL -> hall(canvas, plan.openings(), plan.variant(), false);
			case ROOM -> room(canvas.frame(plan.openings().single().ordinal()), plan.variant());
			case CORRIDOR -> corridor(canvas.frame(plan.openings().north() ? 0 : 1), plan.variant());
			case BRIDGE -> bridge(canvas.frame(plan.openings().north() ? 0 : 1), plan.variant());
			case PAVILION -> pavilion(canvas.frame(CastleRandom.below(plan.variant(), 4)));
			case STAIRWELL -> stairwell(canvas.frame(), plan.variant());
			case SIDEWAYS_CHAMBER -> room(canvas.frame(CastleRandom.below(plan.variant(), 4)), plan.variant());
		}
		return canvas;
	}

	private static boolean inDoorSpan(int i) {
		return i >= DOOR_MIN && i <= DOOR_MAX;
	}

	// ---------------------------------------------------------------- halls and rooms

	/**
	 * A 14 × 14 hall with shoji walls on the cell's inner ring, interior columns, tatami floor, hanging lanterns and a
	 * hipped tile roof.
	 */
	static void hall(Canvas canvas, Openings openings, long variant, boolean grand) {
		Canvas.Frame f = canvas.frame();
		Material column = grand ? Material.LACQUER : Material.TIMBER;

		deckWithTrim(f);
		tatami(f, 2, 13);

		for (Dir side : Dir.values()) {
			wall(canvas.frame(side.ordinal()), openings.has(side), column);
		}

		for (int x : new int[] {5, 10}) {
			for (int z : new int[] {5, 10}) {
				f.fill(x, 1, z, x, 6, z, column == Material.TIMBER ? Piece.axis(Material.TIMBER, Axis.Y) : Piece.of(column));
			}
		}

		f.fill(1, 7, 1, 14, 7, 14, Material.CEILING);
		for (int x : new int[] {3, 12}) {
			for (int z : new int[] {3, 12}) {
				f.set(x, 6, z, Piece.lantern(true));
			}
		}
		if (grand || CastleRandom.below(variant, 2) == 0) {
			f.set(7, 6, 7, Piece.lantern(true));
			f.set(8, 6, 8, Piece.lantern(true));
		}

		hipRoof(f, 0, 7);
	}

	/**
	 * Dead-end room, drawn with its door facing north: nested sliding screens, an alcove at the back and a hipped roof.
	 */
	static void room(Canvas.Frame f, long variant) {
		deckWithTrim(f);
		tatami(f, 2, 13);

		wall(f, true, Material.TIMBER);
		for (int turns = 1; turns < 4; turns++) {
			Canvas.Frame side = f.canvas().frame(f.turns() + turns);
			wall(side, false, Material.TIMBER);
		}

		// Nested screens with a gap facing the door: the endless sliding doors of the castle.
		screenRing(f, 3, 12);
		if (CastleRandom.below(variant, 3) != 0) {
			screenRing(f, 5, 10);
		}

		// Alcove (tokonoma) with a standing lantern.
		f.fill(6, 1, 11, 9, 1, 11, Piece.slab(Material.WOOD_SLAB, false));
		f.set(7, 2, 11, Piece.lantern(false));
		f.set(8, 2, 11, Piece.lantern(false));

		f.fill(1, 7, 1, 14, 7, 14, Material.CEILING);
		f.set(3, 6, 3, Piece.lantern(true));
		f.set(12, 6, 3, Piece.lantern(true));
		f.set(3, 6, 12, Piece.lantern(true));
		f.set(12, 6, 12, Piece.lantern(true));

		hipRoof(f, 0, 7);
	}

	private static void deckWithTrim(Canvas.Frame f) {
		for (int x = 0; x <= LAST; x++) {
			for (int z = 0; z <= LAST; z++) {
				boolean edge = x == 0 || z == 0 || x == LAST || z == LAST;
				f.set(x, 0, z, edge ? Material.FLOOR_TRIM : Material.DECK);
			}
		}
	}

	/** Tatami laid in alternating 4 × 4 blocks, like mats in a real room. */
	private static void tatami(Canvas.Frame f, int min, int max) {
		for (int x = min; x <= max; x++) {
			for (int z = min; z <= max; z++) {
				boolean alongX = ((x - min) / 4 + (z - min) / 4) % 2 == 0;
				f.set(x, 0, z, Piece.axis(Material.TATAMI, alongX ? Axis.X : Axis.Z));
			}
		}
	}

	/**
	 * The north wall of a hall-sized room at z = 1, running x = 1..14: columns, back-lit shoji panels, a beam band and a
	 * screen transom. Rotate the frame for the other sides.
	 */
	private static void wall(Canvas.Frame f, boolean door, Material column) {
		int z = 1;
		for (int x = 1; x <= 14; x++) {
			boolean isColumn = x == 1 || x == 5 || x == 10 || x == 14;
			if (isColumn) {
				f.fill(x, 1, z, x, 6, z, column == Material.TIMBER ? Piece.axis(Material.TIMBER, Axis.Y) : Piece.of(column));
				continue;
			}
			if (door && inDoorSpan(x)) {
				f.fill(x, 5, z, x, 6, z, Piece.axis(Material.TIMBER, Axis.X));
				continue;
			}
			f.fill(x, 1, z, x, 3, z, Material.SHOJI_WALL);
			f.set(x, 4, z, Piece.axis(Material.TIMBER, Axis.X));
			f.fill(x, 5, z, x, 6, z, Material.SHOJI_SCREEN);
		}
	}

	/** A ring of sliding screens from {@code min} to {@code max} with a doorway gap on the north side. */
	private static void screenRing(Canvas.Frame f, int min, int max) {
		for (int i = min; i <= max; i++) {
			for (int y = 1; y <= 4; y++) {
				if (!inDoorSpan(i)) {
					f.set(i, y, min, Material.SHOJI_SCREEN);
				}
				f.set(i, y, max, Material.SHOJI_SCREEN);
				f.set(min, y, i, Material.SHOJI_SCREEN);
				f.set(max, y, i, Material.SHOJI_SCREEN);
			}
		}
	}

	/**
	 * Hipped roof made of concentric rings of tile stairs rising one block per ring, starting at {@code inset} on
	 * layer {@code y}, until the rings meet.
	 */
	private static void hipRoof(Canvas.Frame f, int inset, int y) {
		for (int ring = 0; ; ring++) {
			int min = inset + ring;
			int max = LAST - inset - ring;
			int layer = y + ring;
			if (min > max || layer >= Canvas.SIZE_Y) {
				return;
			}
			if (max - min <= 1) {
				f.fill(min, layer, min, max, layer, max, Material.ROOF_TILE);
				return;
			}
			for (int i = min; i <= max; i++) {
				f.set(i, layer, min, Piece.stairs(Material.ROOF_STAIRS, Dir.SOUTH, false));
				f.set(i, layer, max, Piece.stairs(Material.ROOF_STAIRS, Dir.NORTH, false));
			}
			for (int i = min + 1; i < max; i++) {
				f.set(min, layer, i, Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, false));
				f.set(max, layer, i, Piece.stairs(Material.ROOF_STAIRS, Dir.WEST, false));
			}
		}
	}

	// ---------------------------------------------------------------- corridors and bridges

	/** Enclosed corridor running north–south. One side is sometimes an open veranda with a railing. */
	static void corridor(Canvas.Frame f, long variant) {
		int veranda = CastleRandom.below(CastleRandom.fork(variant, 1), 4); // 0 = west open, 1 = east open, else closed
		for (int z = 0; z <= LAST; z++) {
			f.set(5, 0, z, Material.FLOOR_TRIM);
			f.set(10, 0, z, Material.FLOOR_TRIM);
			f.fill(DOOR_MIN, 0, z, DOOR_MAX, 0, z, Material.DECK);

			boolean isColumn = z % 5 == 2;
			for (int x : new int[] {5, 10}) {
				boolean open = x == 5 ? veranda == 0 : veranda == 1;
				if (isColumn) {
					f.fill(x, 1, z, x, 4, z, Piece.axis(Material.TIMBER, Axis.Y));
				} else if (open) {
					f.set(x, 1, z, Material.RAIL);
				} else {
					f.fill(x, 1, z, x, 3, z, Material.SHOJI_WALL);
					f.set(x, 4, z, Piece.axis(Material.TIMBER, Axis.Z));
				}
			}

			f.fill(5, 5, z, 10, 5, z, Material.CEILING);
			if (isColumn) {
				f.set(z % 2 == 0 ? 7 : 8, 4, z, Piece.lantern(true));
			}

			// Gable roof.
			f.set(4, 5, z, Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, false));
			f.set(11, 5, z, Piece.stairs(Material.ROOF_STAIRS, Dir.WEST, false));
			f.set(5, 6, z, Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, false));
			f.set(10, 6, z, Piece.stairs(Material.ROOF_STAIRS, Dir.WEST, false));
			f.set(6, 7, z, Piece.stairs(Material.ROOF_STAIRS, Dir.EAST, false));
			f.set(9, 7, z, Piece.stairs(Material.ROOF_STAIRS, Dir.WEST, false));
			f.fill(7, 8, z, 8, 8, z, Piece.slab(Material.ROOF_SLAB, false));
		}
		// Close the attic at both gable ends.
		for (int z : new int[] {0, LAST}) {
			f.fill(6, 6, z, 9, 6, z, Material.CEILING);
			f.fill(7, 7, z, 8, 7, z, Material.CEILING);
		}
	}

	/** Open bridge running north–south with vermilion posts carrying lanterns. */
	static void bridge(Canvas.Frame f, long variant) {
		boolean lanterns = CastleRandom.below(CastleRandom.fork(variant, 2), 3) != 0;
		for (int z = 0; z <= LAST; z++) {
			f.fill(DOOR_MIN, 0, z, DOOR_MAX, 0, z, Material.DECK);
			f.set(5, 0, z, Material.LACQUER);
			f.set(10, 0, z, Material.LACQUER);
			boolean isPost = z % 5 == 2;
			for (int x : new int[] {5, 10}) {
				if (isPost) {
					f.fill(x, 1, z, x, 2, z, Material.LACQUER);
					if (lanterns) {
						f.set(x, 3, z, Piece.lantern(false));
					}
				} else {
					f.set(x, 1, z, Material.RAIL);
				}
			}
		}
	}

	// ---------------------------------------------------------------- pavilion

	/** A small roofed platform floating alone in the void. */
	static void pavilion(Canvas.Frame f) {
		f.fill(5, 0, 5, 10, 0, 10, Material.DECK);
		for (int x : new int[] {5, 10}) {
			for (int z : new int[] {5, 10}) {
				f.fill(x, 1, z, x, 3, z, Material.LACQUER);
			}
		}
		f.fill(5, 4, 5, 10, 4, 10, Material.CEILING);
		f.set(7, 3, 7, Piece.lantern(true));
		f.set(8, 3, 8, Piece.lantern(true));
		hipRoof(f, 4, 4);
	}

	// ---------------------------------------------------------------- stairwell

	/**
	 * A deck with a two-flight stair climbing from the deck to a landing just below the equator.
	 *
	 * <p>The first flight rises eastward along the north edge, a corner step turns it south, and the second flight
	 * climbs along the east edge onto the landing in the south-east. The same module is built in the upper half and
	 * flipped, so directly above the landing hangs its mirror image, four blocks away across the equator. Jumping from
	 * the landing carries the player over the equator, gravity flips, and they fall "up" onto the mirrored landing.
	 */
	static void stairwell(Canvas.Frame f, long variant) {
		deckWithTrim(f);

		int y = 1;
		// First flight: along the north edge, climbing east.
		for (int x = 1; x <= 12; x++, y++) {
			stairStep(f, x, y, 1, Dir.EAST);
			stairStep(f, x, y, 2, Dir.EAST);
		}
		// Corner step.
		stairStep(f, 13, y, 1, Dir.EAST);
		stairStep(f, 14, y, 1, Dir.EAST);
		stairStep(f, 13, y, 2, Dir.EAST);
		stairStep(f, 14, y, 2, Dir.EAST);
		y++;
		// Second flight: along the east edge, climbing south.
		for (int z = 3; z <= 10; z++, y++) {
			stairStep(f, 13, y, z, Dir.SOUTH);
			stairStep(f, 14, y, z, Dir.SOUTH);
		}
		if (y - 1 != LANDING_Y) {
			throw new AssertionError("Stair does not reach the landing: " + (y - 1));
		}

		// Landing.
		f.fill(3, LANDING_Y, 11, 14, LANDING_Y, 14, Material.DECK);
		for (int x = 3; x <= 12; x++) {
			f.set(x, LANDING_Y + 1, 11, Material.RAIL);
		}
		for (int x = 3; x <= 14; x++) {
			f.set(x, LANDING_Y + 1, 14, Material.RAIL);
		}
		for (int z = 12; z <= 13; z++) {
			f.set(3, LANDING_Y + 1, z, Material.RAIL);
			f.set(14, LANDING_Y + 1, z, Material.RAIL);
		}

		// Supporting columns that continue past the landing as lacquered gate posts. Their mirror images reach down
		// from the landing above, so together they frame the gravity gate.
		for (int[] post : new int[][] {{3, 11}, {3, 14}, {14, 14}}) {
			f.fill(post[0], 1, post[1], post[0], LANDING_Y - 1, post[1], Piece.axis(Material.TIMBER, Axis.Y));
			f.fill(post[0], LANDING_Y + 1, post[1], post[0], LANDING_Y + 2, post[1], Material.LACQUER);
		}

		for (int x = 5; x <= 11; x += 3) {
			f.set(x, LANDING_Y - 1, 12 + CastleRandom.below(CastleRandom.fork(variant, x), 2), Piece.lantern(true));
		}
	}

	/** One stair block with a supporting board underneath. */
	private static void stairStep(Canvas.Frame f, int x, int y, int z, Dir climb) {
		f.set(x, y, z, Piece.stairs(Material.WOOD_STAIRS, climb, false));
		if (y > 1) {
			f.set(x, y - 1, z, Material.CEILING);
		}
	}
}
