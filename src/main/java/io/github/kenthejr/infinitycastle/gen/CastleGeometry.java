package io.github.kenthejr.infinitycastle.gen;

/**
 * Fixed vertical geometry of the castle.
 *
 * <p>The dimension is split into {@link #FLOOR_COUNT} stacked floors. Every floor is {@link #FLOOR_HEIGHT} blocks tall
 * and consists of two halves:
 *
 * <pre>
 *   base + 47  ┌──────────────────────┐  inverted deck (rooms hang downward from here)
 *              │   UPPER half         │  gravity points UP
 *   base + 24  ├ ─ ─ ─ equator ─ ─ ─ ─┤  crossing this plane flips gravity
 *              │   LOWER half         │  gravity points DOWN
 *   base + 0   └──────────────────────┘  upright deck (rooms stand on it)
 * </pre>
 *
 * The inverted deck of floor {@code n} sits directly on top of the upright deck of floor {@code n + 1}, so the castle
 * reads as an endless stack of double-sided floors with rooms growing out of both faces.
 */
public final class CastleGeometry {
	public static final int MIN_Y = -384;
	public static final int HEIGHT = 768;
	public static final int MAX_Y = MIN_Y + HEIGHT;

	public static final int FLOOR_HEIGHT = 48;
	public static final int HALF_HEIGHT = FLOOR_HEIGHT / 2;
	public static final int FLOOR_COUNT = HEIGHT / FLOOR_HEIGHT;

	/** Cells are exactly one chunk wide so each chunk can be generated independently. */
	public static final int CELL_SIZE = 16;

	/** The floor players arrive on. Its upright deck is at y = 0. */
	public static final int ENTRANCE_FLOOR = 8;

	private CastleGeometry() {
	}

	public static int floorBase(int floor) {
		return MIN_Y + floor * FLOOR_HEIGHT;
	}

	/** Floor index for a world y. May be outside {@code [0, FLOOR_COUNT)} above or below the castle. */
	public static int floorOf(double y) {
		return Math.floorDiv((int) Math.floor(y) - MIN_Y, FLOOR_HEIGHT);
	}

	public static double equatorY(int floor) {
		return floorBase(floor) + HALF_HEIGHT;
	}

	public static Half halfOf(double y) {
		double offset = y - floorBase(floorOf(y));
		return offset < HALF_HEIGHT ? Half.LOWER : Half.UPPER;
	}

	/** World y for a y coordinate local to a half, where local 0 is always the deck the half's rooms stand on. */
	public static int worldY(int floor, Half half, int localY) {
		int base = floorBase(floor);
		return half == Half.LOWER ? base + localY : base + FLOOR_HEIGHT - 1 - localY;
	}

	/** World y a player's feet rest at when standing on the entrance deck. */
	public static int entranceStandY() {
		return worldY(ENTRANCE_FLOOR, Half.LOWER, 0) + 1;
	}

	public static boolean isValidFloor(int floor) {
		return floor >= 0 && floor < FLOOR_COUNT;
	}
}
