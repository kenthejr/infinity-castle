package io.github.kenthejr.infinitycastle.gen;

/** Horizontal direction, independent of Minecraft's {@code Direction} so the generator can be unit-tested. */
public enum Dir {
	NORTH(0, -1),
	EAST(1, 0),
	SOUTH(0, 1),
	WEST(-1, 0);

	private final int dx;
	private final int dz;

	Dir(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public int dx() {
		return this.dx;
	}

	public int dz() {
		return this.dz;
	}

	/** Rotates clockwise when viewed from above. */
	public Dir rotateCw(int quarterTurns) {
		return values()[Math.floorMod(this.ordinal() + quarterTurns, 4)];
	}

	public Dir opposite() {
		return this.rotateCw(2);
	}

	public Axis axis() {
		return this.dx != 0 ? Axis.X : Axis.Z;
	}
}
