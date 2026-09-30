package io.github.kenthejr.infinitycastle.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kenthejr.infinitycastle.gen.CastleGeometry;
import io.github.kenthejr.infinitycastle.gen.Half;
import org.junit.jupiter.api.Test;

class GravityRulesTest {
	private static final int FLOOR = 5;
	private static final double BASE = CastleGeometry.floorBase(FLOOR);
	private static final double EQUATOR = CastleGeometry.equatorY(FLOOR);

	@Test
	void bandsStartAtEachEquator() {
		assertEquals(FLOOR, GravityRules.equatorBand(EQUATOR));
		assertEquals(FLOOR - 1, GravityRules.equatorBand(EQUATOR - 0.001));
		assertEquals(FLOOR, GravityRules.equatorBand(EQUATOR + CastleGeometry.FLOOR_HEIGHT - 0.001));
	}

	@Test
	void crossingTheEquatorFlipsGravity() {
		assertTrue(GravityRules.nextInverted(false, EQUATOR - 0.1, EQUATOR + 0.1));
		assertFalse(GravityRules.nextInverted(true, EQUATOR + 0.1, EQUATOR - 0.1));
	}

	@Test
	void movingWithinABandKeepsGravity() {
		assertFalse(GravityRules.nextInverted(false, BASE + 2, BASE + 10));
		assertTrue(GravityRules.nextInverted(true, BASE + 2, BASE + 10));
	}

	/** Falling through a gap in the slab between floors must not flip gravity, or a player would bounce forever. */
	@Test
	void crossingTheSlabBetweenFloorsKeepsGravity() {
		assertFalse(GravityRules.nextInverted(false, BASE + 0.5, BASE - 0.5));
		assertTrue(GravityRules.nextInverted(true, BASE - 0.5, BASE + 0.5));
	}

	@Test
	void initialGravityFollowsTheHalf() {
		assertFalse(GravityRules.initialInverted(BASE + 3));
		assertTrue(GravityRules.initialInverted(BASE + CastleGeometry.FLOOR_HEIGHT - 3));
		assertEquals(Half.UPPER, CastleGeometry.halfOf(EQUATOR));
	}

	@Test
	void teleportingFarResetsGravity() {
		assertTrue(GravityRules.nextInverted(false, BASE + 3, BASE + 3 * CastleGeometry.FLOOR_HEIGHT + 40));
		assertFalse(GravityRules.nextInverted(true, BASE + 3 * CastleGeometry.FLOOR_HEIGHT + 40, BASE + 3));
	}

	@Test
	void wrappingAroundTheCastleKeepsGravityAndMomentum() {
		double below = CastleGeometry.MIN_Y - GravityRules.WRAP_MARGIN - 1;
		assertEquals(CastleGeometry.HEIGHT, GravityRules.wrapOffset(below));
		double above = CastleGeometry.MAX_Y + GravityRules.WRAP_MARGIN + 1;
		assertEquals(-CastleGeometry.HEIGHT, GravityRules.wrapOffset(above));
		assertEquals(0.0, GravityRules.wrapOffset(0.0));

		assertFalse(GravityRules.nextInverted(false, below, below + GravityRules.wrapOffset(below)));
		assertTrue(GravityRules.nextInverted(true, above, above + GravityRules.wrapOffset(above)));
	}

	// ------------------------------------------------------------ physics simulation

	/** Vanilla player constants. */
	private static final double JUMP = 0.42;
	private static final double GRAVITY = 0.08;
	private static final double DRAG = 0.98;
	private static final double HEIGHT = 1.8;

	private static final double LOWER_LANDING_TOP = EQUATOR - 2;
	private static final double UPPER_LANDING_BOTTOM = EQUATOR + 2;

	/** Player on the stairwell landing, between the two gate landings. */
	private static final class Body {
		double feet;
		double vy;
		boolean inverted;
		double lastCenter;

		Body(double feet, boolean inverted) {
			this.feet = feet;
			this.inverted = inverted;
			this.lastCenter = feet + HEIGHT / 2;
		}

		void jump() {
			this.vy = this.inverted ? -JUMP : JUMP;
		}

		/** One tick in vanilla order: gravity decided in Player.tick, then movement, then gravity and drag. */
		void tick(double gravityScale) {
			double center = this.feet + HEIGHT / 2;
			this.inverted = GravityRules.nextInverted(this.inverted, this.lastCenter, center);
			this.lastCenter = center;

			this.feet += this.vy;
			if (this.feet < LOWER_LANDING_TOP) {
				this.feet = LOWER_LANDING_TOP;
				this.vy = 0;
			}
			if (this.feet + HEIGHT > UPPER_LANDING_BOTTOM) {
				this.feet = UPPER_LANDING_BOTTOM - HEIGHT;
				this.vy = 0;
			}
			double g = GRAVITY * gravityScale * (this.inverted ? -1 : 1);
			this.vy = (this.vy - g) * DRAG;
		}

		boolean onLowerLanding() {
			return Math.abs(this.feet - LOWER_LANDING_TOP) < 1e-9;
		}

		boolean onUpperLanding() {
			return Math.abs(this.feet + HEIGHT - UPPER_LANDING_BOTTOM) < 1e-9;
		}
	}

	@Test
	void jumpingFromTheLandingFlipsYouOntoTheLandingAbove() {
		for (double scale : new double[] {1.0, FloorProfile.DRIFTING_GRAVITY}) {
			Body body = new Body(LOWER_LANDING_TOP, false);
			body.jump();
			for (int t = 0; t < 100; t++) {
				body.tick(scale);
			}
			assertTrue(body.inverted, "gravity did not flip (scale " + scale + ")");
			assertTrue(body.onUpperLanding(), "not resting on the upper landing: feet at " + body.feet);

			// And back again.
			body.jump();
			for (int t = 0; t < 100; t++) {
				body.tick(scale);
			}
			assertFalse(body.inverted, "gravity did not flip back (scale " + scale + ")");
			assertTrue(body.onLowerLanding(), "not resting on the lower landing: feet at " + body.feet);
		}
	}

	@Test
	void standingOnTheLandingDoesNotFlip() {
		Body lower = new Body(LOWER_LANDING_TOP, false);
		Body upper = new Body(UPPER_LANDING_BOTTOM - HEIGHT, true);
		for (int t = 0; t < 100; t++) {
			lower.tick(1.0);
			upper.tick(1.0);
		}
		assertFalse(lower.inverted);
		assertTrue(lower.onLowerLanding());
		assertTrue(upper.inverted);
		assertTrue(upper.onUpperLanding());
	}
}
