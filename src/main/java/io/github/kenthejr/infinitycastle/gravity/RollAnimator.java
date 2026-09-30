package io.github.kenthejr.infinitycastle.gravity;

/**
 * Eases a camera roll angle toward a target, one step per game tick, and interpolates between ticks for rendering.
 * Pure math so it can be unit-tested; the client owns an instance.
 */
public final class RollAnimator {
	/** Fraction of the remaining angle covered each tick. */
	public static final float EASE = 0.28F;
	/** Smallest step in degrees, so the ease-out doesn't crawl at the end. */
	public static final float MIN_STEP = 3.0F;

	private float roll;
	private float previousRoll;

	public void tick(float target, boolean smooth) {
		this.previousRoll = this.roll;
		float remaining = target - this.roll;
		if (!smooth || Math.abs(remaining) <= MIN_STEP) {
			this.roll = target;
			return;
		}
		float step = Math.max(Math.abs(remaining) * EASE, MIN_STEP);
		this.roll += Math.signum(remaining) * step;
	}

	public void snap(float target) {
		this.roll = target;
		this.previousRoll = target;
	}

	public float roll(float partialTick) {
		return this.previousRoll + (this.roll - this.previousRoll) * partialTick;
	}

	/** True once the view has turned more than halfway over, which is when controls should mirror. */
	public static boolean isUpsideDown(float rollDegrees) {
		return Math.cos(Math.toRadians(rollDegrees)) < 0.0;
	}

	/** Target roll for a gravity direction and a floor's constant tilt. */
	public static float target(boolean inverted, float tilt) {
		return (inverted ? 180.0F : 0.0F) + tilt;
	}
}
