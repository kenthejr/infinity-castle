package io.github.kenthejr.infinitycastle.gen;

/** Places a cell's canvas into the world, flipping upper-half cells upside down and tipping sideways chambers over. */
public final class CellPlacer {
	/** How far above the bottom of the half a sideways chamber floats. */
	public static final int SIDEWAYS_LIFT = 4;

	@FunctionalInterface
	public interface BlockSink {
		/**
		 * @param localX x within the cell or chunk being placed
		 * @param worldY absolute y
		 * @param localZ z within the cell or chunk being placed
		 */
		void set(int localX, int worldY, int localZ, Piece piece);
	}

	private CellPlacer() {
	}

	/** Places one cell's canvas, in cell-local x and z. */
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
				if (p.isAir()) {
					return;
				}
			}
			if (half.inverted()) {
				p = p.flipVertical();
			}
			sink.set(px, CastleGeometry.worldY(floor, half, py), pz, p);
		});
	}

	/** Builds and places every half of every floor in one cell, in cell-local x and z. */
	public static void placeCell(CastleLayout layout, int cellX, int cellZ, BlockSink sink) {
		for (int floor = 0; floor < CastleGeometry.FLOOR_COUNT; floor++) {
			for (Half half : Half.values()) {
				CellPlan plan = layout.plan(floor, half, cellX, cellZ);
				if (plan.type() == ModuleType.VOID) {
					continue;
				}
				place(plan, Modules.build(plan), floor, half, sink);
			}
		}
	}

	/** Builds the cell a chunk belongs to and places only that chunk's part of it, in chunk-local x and z. */
	public static void placeChunk(CastleLayout layout, int chunkX, int chunkZ, BlockSink sink) {
		int offsetX = Math.floorMod(chunkX, CastleGeometry.CHUNKS_PER_CELL) * 16;
		int offsetZ = Math.floorMod(chunkZ, CastleGeometry.CHUNKS_PER_CELL) * 16;
		placeCell(layout, Math.floorDiv(chunkX, CastleGeometry.CHUNKS_PER_CELL), Math.floorDiv(chunkZ, CastleGeometry.CHUNKS_PER_CELL), (x, y, z, piece) -> {
			int cx = x - offsetX;
			int cz = z - offsetZ;
			if (cx >= 0 && cx < 16 && cz >= 0 && cz < 16) {
				sink.set(cx, y, cz, piece);
			}
		});
	}
}
