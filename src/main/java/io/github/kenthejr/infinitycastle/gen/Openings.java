package io.github.kenthejr.infinitycastle.gen;

/** Which of a cell's four edges have a doorway into the neighbouring cell. */
public record Openings(boolean north, boolean east, boolean south, boolean west) {
	public static final Openings NONE = new Openings(false, false, false, false);
	public static final Openings ALL = new Openings(true, true, true, true);

	/**
	 * Doorways are three blocks wide and centred on the cell's edge, so every module lines up with its neighbours. The
	 * bridges that lead to them are two blocks wider, for their rails.
	 */
	public static final int DOOR_MIN = CastleGeometry.CELL_SIZE / 2 - 1;
	public static final int DOOR_MAX = CastleGeometry.CELL_SIZE / 2 + 1;

	public boolean has(Dir dir) {
		return switch (dir) {
			case NORTH -> this.north;
			case EAST -> this.east;
			case SOUTH -> this.south;
			case WEST -> this.west;
		};
	}

	public int count() {
		return (this.north ? 1 : 0) + (this.east ? 1 : 0) + (this.south ? 1 : 0) + (this.west ? 1 : 0);
	}

	/** Two openings directly across from each other. */
	public boolean isStraight() {
		return this.count() == 2 && (this.north && this.south || this.east && this.west);
	}

	/** The only opening, for cells with exactly one. */
	public Dir single() {
		if (this.count() != 1) {
			throw new IllegalStateException("Not a single opening: " + this);
		}
		for (Dir dir : Dir.values()) {
			if (this.has(dir)) {
				return dir;
			}
		}
		throw new AssertionError();
	}
}
