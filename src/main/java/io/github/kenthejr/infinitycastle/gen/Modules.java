package io.github.kenthejr.infinitycastle.gen;

import static io.github.kenthejr.infinitycastle.gen.CastleGeometry.DECK_Y;

/**
 * Builds the architecture for each {@link ModuleType} into a {@link Canvas} from the hand-built {@link Designs}.
 *
 * <p>All modules are drawn upright, standing on the deck plane at {@link CastleGeometry#DECK_Y}. Upper-half cells are
 * later flipped by {@link CellPlacer}, which is what makes rooms hang from the ceiling. A building is centred in its
 * cell, so the doorway in the middle of each of its sides lines up with {@link Openings#DOOR_MIN}..{@link
 * Openings#DOOR_MAX} on the cell's edge, and a bridge runs from each open door to the edge.
 *
 * <p>The bridges and the long staircase are drawn here from the repeating pattern they were designed with, so they
 * can be any length. Both are double-sided: the rails and lanterns are mirrored under the walkway, so they look right
 * from the inverted half across the equator and from the floor below.
 */
public final class Modules {
	/** Doorways in the designs are two blocks tall. */
	public static final int DOOR_HEIGHT = 2;

	public static final int CENTER = CastleGeometry.CELL_SIZE / 2;

	/** Bridges are five blocks wide: a three-block walkway between two rails. */
	public static final int BRIDGE_MIN = CENTER - 2;
	public static final int BRIDGE_MAX = CENTER + 2;

	/** Height of the stairwell landing block. Its top face is where the jump across the equator starts. */
	public static final int LANDING_Y = 21;

	/** A spot in the middle of the stairwell landing, in cell coordinates, clear of rails. */
	public static final int LANDING_CENTER_X = CENTER;
	public static final int LANDING_CENTER_Z = 30;

	/** Where players arrive in the entrance hall: on the room floor, clear of the spiral stair at the centre. */
	public static final int ARRIVAL_X = CENTER + 4;
	public static final int ARRIVAL_Z = CENTER;

	/** The first step of the roof stair, just south of the hatch the spiral stair comes up through. */
	static final int ROOF_STAIR_START_Z = CENTER + 3;

	/**
	 * Where a sideways chamber is drawn before the cell is tipped over: far enough north that, once its depth has
	 * become height, it floats above the deck and below the top of the half.
	 */
	static final int SIDEWAYS_Z = 4;

	private static final int LAST = CastleGeometry.CELL_SIZE - 1;

	private Modules() {
	}

	public static Canvas build(CellPlan plan) {
		Canvas canvas = new Canvas();
		Openings openings = plan.openings();
		long design = CastleRandom.fork(plan.variant(), 1);
		int turns = CastleRandom.below(CastleRandom.fork(plan.variant(), 2), 4);
		switch (plan.type()) {
			case VOID -> {
			}
			case ENTRANCE -> building(canvas, Designs.get(Designs.ENTRANCE), openings);
			case HALL -> building(canvas, Designs.pick(Designs.LARGE, design).rotated(turns), openings);
			case ROOM -> building(canvas, Designs.pick(CastleRandom.below(design, 3) == 0 ? Designs.LONG : Designs.SMALL, design).rotated(turns), openings);
			case PAVILION -> building(canvas, Designs.pick(Designs.SMALL, design).rotated(turns), Openings.NONE);
			case CORRIDOR -> building(canvas, Designs.pick(Designs.LONG, design).rotated(openings.north() ? 1 : 0), openings);
			case BRIDGE -> bridgeAcross(canvas, openings.north() ? Axis.Z : Axis.X);
			case STAIRWELL -> {
				Template hall = Designs.pick(Designs.TALL, design);
				building(canvas, hall, openings);
				roofStair(canvas, DECK_Y + hall.sizeY());
			}
			case SIDEWAYS_CHAMBER -> sideways(canvas, design);
		}
		return canvas;
	}

	// ---------------------------------------------------------------- buildings

	/** Stamps a design centred on the deck, slides open the doors that lead somewhere and bridges each to the edge. */
	static void building(Canvas canvas, Template design, Openings openings) {
		int ox = CENTER - (design.sizeX() - 1) / 2;
		int oz = CENTER - (design.sizeZ() - 1) / 2;
		design.stamp(canvas, ox, DECK_Y, oz, openings);
		for (Dir side : Dir.values()) {
			if (!openings.has(side)) {
				continue;
			}
			// Blocks between the cell's edge and the building on this side. Even-sized cells leave one fewer to the
			// south and east of an odd-sized, centred building.
			int gap = switch (side) {
				case NORTH -> oz;
				case WEST -> ox;
				case SOUTH -> LAST - (oz + design.sizeZ() - 1);
				case EAST -> LAST - (ox + design.sizeX() - 1);
			};
			bridge(canvas, side, gap);
		}
	}

	/**
	 * A small room drawn upright at the north of the cell. {@link CellPlacer} tips the whole cell over so that its
	 * depth becomes height, which leaves the room floating on its side against the cell's south edge.
	 */
	static void sideways(Canvas canvas, long design) {
		Template room = Designs.pick(Designs.SIDEWAYS, design);
		room.stamp(canvas, CENTER - (room.sizeX() - 1) / 2, 0, SIDEWAYS_Z, Openings.NONE);
	}

	// ---------------------------------------------------------------- bridges

	/** A bridge from the cell's edge on {@code side}, running {@code length} blocks in toward the centre. */
	static void bridge(Canvas canvas, Dir side, int length) {
		for (int i = 0; i < length; i++) {
			bridgeSlice(canvas, side.axis(), side == Dir.NORTH || side == Dir.WEST ? i : LAST - i);
		}
	}

	/** A bridge across the whole cell along {@code axis}. */
	static void bridgeAcross(Canvas canvas, Axis axis) {
		for (int along = 0; along <= LAST; along++) {
			bridgeSlice(canvas, axis, along);
		}
	}

	/**
	 * One block's length of bridge at coordinate {@code along} on {@code axis}: a spruce walkway with dark-oak kerbs,
	 * a rail of alternating fences and gates on each side, and a lantern on every fourth post. The rail and lanterns
	 * are repeated upside down under the deck. The pattern is phased on the coordinate, so it continues seamlessly
	 * from one cell's bridge into the next.
	 */
	private static void bridgeSlice(Canvas canvas, Axis axis, int along) {
		for (int across = BRIDGE_MIN; across <= BRIDGE_MAX; across++) {
			int x = axis == Axis.Z ? across : along;
			int z = axis == Axis.Z ? along : across;
			boolean kerb = across == BRIDGE_MIN || across == BRIDGE_MAX;
			canvas.set(x, DECK_Y, z, kerb ? Material.DARK_OAK_PLANKS : Material.SPRUCE_PLANKS);
			if (!kerb) {
				continue;
			}
			Piece rail = along % 2 == 0 ? Piece.of(Material.SPRUCE_FENCE) : Piece.gate(axis == Axis.Z ? Dir.EAST : Dir.NORTH);
			canvas.set(x, DECK_Y + 1, z, rail);
			canvas.set(x, DECK_Y - 1, z, rail);
			if (along % 4 == 2) {
				canvas.set(x, DECK_Y + 2, z, Piece.lantern(false));
				canvas.set(x, DECK_Y - 2, z, Piece.lantern(true));
			}
		}
	}

	// ---------------------------------------------------------------- stairwell

	/**
	 * The long staircase: from the roof of the hall, a flight climbs south at forty-five degrees to a landing just
	 * below the equator. Each step is a stair block over an upside-down stair, so the underside is a smooth slope.
	 * Rails of dark-oak planks run level with the steps, with two fences and a lantern above and, mirrored, below.
	 *
	 * <p>The same module is built in the upper half and flipped, so directly above the landing hangs its mirror image,
	 * four blocks away across the equator. Jumping from the landing carries the player over the equator, gravity
	 * flips, and they fall "up" onto the mirrored landing.
	 *
	 * @param roofY the first free y above the roof, where the climb starts
	 */
	static void roofStair(Canvas canvas, int roofY) {
		int steps = LANDING_Y - roofY + 1;
		if (ROOF_STAIR_START_Z + steps + 1 > LAST) {
			throw new AssertionError("Roof at " + roofY + " is too low for the stair to reach the landing inside the cell");
		}
		int z = ROOF_STAIR_START_Z;
		int column = 0;
		for (int i = 0; i < steps; i++, z++, column++) {
			int y = roofY + i;
			canvas.fill(BRIDGE_MIN + 1, y, z, BRIDGE_MAX - 1, y, z, Piece.stairs(Material.SPRUCE_STAIRS, Dir.SOUTH, false));
			for (int x = BRIDGE_MIN + 1; x < BRIDGE_MAX; x++) {
				canvas.setIfAir(x, y - 1, z, Piece.stairs(Material.SPRUCE_STAIRS, Dir.NORTH, true));
			}
			stairRail(canvas, z, y, column % 2 == 0);
		}
		// Landing, level with the last step, out to the cell's edge.
		for (; z <= LAST; z++, column++) {
			canvas.set(BRIDGE_MIN, LANDING_Y, z, Material.DARK_OAK_PLANKS);
			canvas.set(BRIDGE_MAX, LANDING_Y, z, Material.DARK_OAK_PLANKS);
			canvas.fill(BRIDGE_MIN + 1, LANDING_Y, z, BRIDGE_MAX - 1, LANDING_Y, z, Material.SPRUCE_PLANKS);
			stairRail(canvas, z, LANDING_Y, column % 2 == 0);
		}
		canvas.fill(BRIDGE_MIN + 1, LANDING_Y + 1, LAST, BRIDGE_MAX - 1, LANDING_Y + 1, LAST, Material.SPRUCE_FENCE);
	}

	/**
	 * One column of the staircase rail on both sides of step {@code y}. Nothing is drawn where the hall already stands,
	 * so the rail grows out of the roof where the stair starts.
	 */
	private static void stairRail(Canvas canvas, int z, int y, boolean lantern) {
		for (int x : new int[] {BRIDGE_MIN, BRIDGE_MAX}) {
			canvas.setIfAir(x, y - 1, z, Piece.of(Material.DARK_OAK_PLANKS));
			canvas.setIfAir(x, y, z, Piece.of(Material.DARK_OAK_PLANKS));
			for (int dy = 1; dy <= 2; dy++) {
				canvas.setIfAir(x, y + dy, z, Piece.of(Material.SPRUCE_FENCE));
				canvas.setIfAir(x, y - 1 - dy, z, Piece.of(Material.SPRUCE_FENCE));
			}
			if (lantern) {
				canvas.setIfAir(x, y + 3, z, Piece.lantern(false));
				if (canvas.get(x, y - 3, z).material() == Material.SPRUCE_FENCE) {
					canvas.setIfAir(x, y - 4, z, Piece.lantern(true));
				}
			}
		}
	}
}
