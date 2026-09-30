package io.github.kenthejr.infinitycastle.gravity;

import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.Half;

/**
 * Decides which way gravity points for an entity inside the castle.
 *
 * <p>Gravity only flips when an entity's centre crosses a floor's <em>equator</em> (the plane halfway between its
 * upright and inverted decks): crossing upward makes gravity point up, crossing downward makes it point down. Since
 * gravity then pulls away from the equator on both sides, crossing is self-reinforcing and never oscillates.
 *
 * <p>Passing through the slab between two floors (for example by falling through a gap) does <em>not</em> flip
 * gravity. That boundary has upright rooms on one side and inverted rooms on the other, and flipping there would trap
 * a falling player bouncing between the two.
 */
public final class GravityRules {
	/** How far past the top or bottom of the castle a falling entity travels before being wrapped to the other end. */
	public static final double WRAP_MARGIN = 8.0;

	private GravityRules() {
	}

	/**
	 * Index of the band between two consecutive equators that contains {@code y}. Band {@code k} starts at the equator
	 * of floor {@code k}.
	 */
	public static int equatorBand(double y) {
		return (int) Math.floor((y - CastleGeometry.MIN_Y - CastleGeometry.HALF_HEIGHT) / CastleGeometry.FLOOR_HEIGHT);
	}

	/** Gravity for an entity that appears at {@code centerY} with no history, such as after logging in. */
	public static boolean initialInverted(double centerY) {
		return CastleGeometry.halfOf(centerY) == Half.UPPER;
	}

	/**
	 * Gravity after an entity's centre moved from {@code previousCenterY} to {@code centerY}.
	 *
	 * @param wasInverted gravity before the move
	 */
	public static boolean nextInverted(boolean wasInverted, double previousCenterY, double centerY) {
		int before = equatorBand(previousCenterY);
		int after = equatorBand(centerY);
		int delta = after - before;
		if (delta == 0) {
			return wasInverted;
		}
		if (Math.abs(delta) == CastleGeometry.FLOOR_COUNT) {
			// Wrapped from one end of the castle to the other (see wrapOffset): momentum and gravity carry over.
			return wasInverted;
		}
		if (Math.abs(delta) > 1) {
			// Teleported across several floors: there is no meaningful crossing, so start fresh.
			return initialInverted(centerY);
		}
		return delta > 0;
	}

	/**
	 * Returns the vertical offset needed to wrap an entity that fell out of the castle back in at the opposite end, or 0
	 * if it is still inside. The castle's floors repeat seamlessly, so falling off the bottom lands you in the top.
	 */
	public static double wrapOffset(double y) {
		if (y < CastleGeometry.MIN_Y - WRAP_MARGIN) {
			return CastleGeometry.HEIGHT;
		}
		if (y > CastleGeometry.MAX_Y + WRAP_MARGIN) {
			return -CastleGeometry.HEIGHT;
		}
		return 0.0;
	}
}
