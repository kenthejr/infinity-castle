package io.github.kenthejr.infinitycastle.gen;

/**
 * A 32 × 24 × 32 voxel grid holding one cell's worth of structure in upright local coordinates: y = 0 is the bottom of
 * the half and y grows away from it. Unset voxels are air.
 */
public final class Canvas {
	public static final int SIZE_X = CastleGeometry.CELL_SIZE;
	public static final int SIZE_Y = CastleGeometry.HALF_HEIGHT;
	public static final int SIZE_Z = CastleGeometry.CELL_SIZE;

	private final Piece[] pieces = new Piece[SIZE_X * SIZE_Y * SIZE_Z];

	public static boolean inBounds(int x, int y, int z) {
		return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
	}

	private static int index(int x, int y, int z) {
		return (y * SIZE_Z + z) * SIZE_X + x;
	}

	public Piece get(int x, int y, int z) {
		if (!inBounds(x, y, z)) {
			return Piece.AIR;
		}
		Piece piece = this.pieces[index(x, y, z)];
		return piece == null ? Piece.AIR : piece;
	}

	public void set(int x, int y, int z, Piece piece) {
		if (!inBounds(x, y, z)) {
			throw new IndexOutOfBoundsException("(" + x + ", " + y + ", " + z + ") is outside the canvas");
		}
		this.pieces[index(x, y, z)] = piece.isAir() ? null : piece;
	}

	public void set(int x, int y, int z, Material material) {
		this.set(x, y, z, Piece.of(material));
	}

	/** Sets a voxel only if it is inside the canvas and nothing has been drawn there yet. */
	public boolean setIfAir(int x, int y, int z, Piece piece) {
		if (!inBounds(x, y, z) || !this.get(x, y, z).isAir()) {
			return false;
		}
		this.set(x, y, z, piece);
		return true;
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Piece piece) {
		for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
			for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
				for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
					this.set(x, y, z, piece);
				}
			}
		}
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Material material) {
		this.fill(x0, y0, z0, x1, y1, z1, Piece.of(material));
	}

	/** Visits every non-air voxel. */
	public void forEach(VoxelVisitor visitor) {
		for (int y = 0; y < SIZE_Y; y++) {
			for (int z = 0; z < SIZE_Z; z++) {
				for (int x = 0; x < SIZE_X; x++) {
					Piece piece = this.pieces[index(x, y, z)];
					if (piece != null) {
						visitor.visit(x, y, z, piece);
					}
				}
			}
		}
	}

	public boolean isEmpty() {
		for (Piece piece : this.pieces) {
			if (piece != null) {
				return false;
			}
		}
		return true;
	}

	@FunctionalInterface
	public interface VoxelVisitor {
		void visit(int x, int y, int z, Piece piece);
	}
}
