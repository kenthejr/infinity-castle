package io.github.kenthejr.infinitycastle.gravity;

import io.github.kenthejr.infinitycastle.gen.CastleGeometry;

/**
 * Per-floor physics and camera character. Profiles depend only on the floor index, so the client and server agree
 * without syncing anything.
 *
 * @param floor          floor index, 0 at the bottom of the castle
 * @param gravityScale   multiplier on vanilla gravity
 * @param cameraTilt     constant camera roll in degrees, for floors that feel subtly wrong
 */
public record FloorProfile(int floor, double gravityScale, float cameraTilt) {
	/** Gravity on drifting floors. Stays above the scale at which vanilla stops applying fall damage sensibly. */
	public static final double DRIFTING_GRAVITY = 0.6;
	public static final float TILT_DEGREES = 8.0F;

	public static FloorProfile of(int floor) {
		int f = Math.floorMod(floor, CastleGeometry.FLOOR_COUNT);
		if (f == CastleGeometry.ENTRANCE_FLOOR) {
			return new FloorProfile(f, 1.0, 0.0F);
		}
		double gravity = f % 4 == 1 ? DRIFTING_GRAVITY : 1.0;
		float tilt = f % 5 == 3 ? (f % 2 == 0 ? TILT_DEGREES : -TILT_DEGREES) : 0.0F;
		return new FloorProfile(f, gravity, tilt);
	}

	public static FloorProfile at(double y) {
		return of(CastleGeometry.floorOf(y));
	}

	public boolean drifting() {
		return this.gravityScale < 1.0;
	}

	/** Human-facing floor number, counted from 1 at the bottom. */
	public int number() {
		return this.floor + 1;
	}

	/** The floor's name in Japanese, e.g. 第九層 ("ninth floor"). */
	public String japaneseName() {
		return "第" + KanjiNumerals.of(this.number()) + "層";
	}
}
