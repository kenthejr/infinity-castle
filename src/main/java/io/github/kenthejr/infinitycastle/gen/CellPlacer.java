package io.github.kenthejr.infinitycastle.gen;

/** Places a cell's canvas into the world, flipping upper-half cells upside down and tipping sideways chambers over. */
public final class CellPlacer {
	/** How far above the deck a sideways chamber floats. */
	public static final int SIDEWAYS_LIFT = 4;

	@FunctionalInterface
	public interface BlockSink {
		/**
		 * @param localX x within the cell (and chunk), 0..15
		 * @param worldY absolute y
		 * @param localZ z within the cell (and chunk), 0..15
		 */
		void set(int localX, int worldY, int localZ, Piece piece);
	}

	private CellPlacer() {
	}

	public static void place(CellPlan plan, Canvas canvas, int floor, Half half, BlockSink sink) {
		boolean sideways = plan.type() == ModuleType.SIDEWAYS_CHAMBER;
		canvas.forEach((x, y, z, piece) -> {
			int px = x;
			int py = y;
			int pz = z;
			Piece p = piece;
			if (sideways) {
				// Tip the structure over so its floor becomes the cell's south wall and its "up" points north.
				py = z + SIDEWAYS_LIFT;
				pz = Canvas.SIZE_Z - 1 - y;
				if (pz < 0 || py >= Canvas.SIZE_Y) {
					return;
				}
				p = p.tipOverX();
			}
			if (half.inverted()) {
				p = p.flipVertical();
			}
			sink.set(px, CastleGeometry.worldY(floor, half, py), pz, p);
		});
	}

	/** Builds and places every cell of one chunk column. */
	public static void placeColumn(CastleLayout layout, int cx, int cz, BlockSink sink) {
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			for (Half half : Half.values()) {
				CellPlan plan = layout.plan(floor, half, cx, cz);
				if (plan.type() == ModuleType.VOID) {
					continue;
				}
				place(plan, Modules.build(plan), floor, half, sink);
			}
		}
	}
}
