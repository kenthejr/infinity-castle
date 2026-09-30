package io.github.kenthejr.infinitycastle.gen;

/** Which side of a floor's equator a position is on. */
public enum Half {
	/** Rooms stand upright on the deck at the bottom of the floor. */
	LOWER,
	/** Rooms hang upside down from the deck at the top of the floor. */
	UPPER;

	public boolean inverted() {
		return this == UPPER;
	}
}
