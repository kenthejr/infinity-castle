package io.github.kenthejr.infinitycastle.gravity;

/** Per-player gravity bookkeeping, mixed into {@code Player}. Transient: it is rebuilt from position after a relog. */
public interface GravityState {
	boolean infinitycastle$isTracking();

	boolean infinitycastle$isInverted();

	double infinitycastle$lastCenterY();

	int infinitycastle$lastFloor();

	void infinitycastle$update(boolean inverted, double centerY, int floor);

	/** Whether eye height was last computed for inverted gravity. */
	boolean infinitycastle$dimensionsInverted();

	void infinitycastle$setDimensionsInverted(boolean inverted);

	void infinitycastle$reset();
}
