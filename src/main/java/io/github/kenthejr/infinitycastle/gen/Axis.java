package io.github.kenthejr.infinitycastle.gen;

public enum Axis {
	X,
	Y,
	Z;

	/** The axis after a quarter turn around the vertical axis. */
	public Axis rotateY(int quarterTurns) {
		if (this == Y || quarterTurns % 2 == 0) {
			return this;
		}
		return this == X ? Z : X;
	}
}
