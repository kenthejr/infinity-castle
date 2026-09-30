package io.github.kenthejr.infinitycastle.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RollAnimatorTest {
	@Test
	void smoothFlipSettlesWithinASecond() {
		RollAnimator animator = new RollAnimator();
		int ticks = 0;
		while (animator.roll(1.0F) != 180.0F && ticks < 100) {
			animator.tick(180.0F, true);
			ticks++;
		}
		assertEquals(180.0F, animator.roll(1.0F));
		assertTrue(ticks > 3 && ticks <= 20, "took " + ticks + " ticks");
	}

	@Test
	void rollNeverOvershoots() {
		RollAnimator animator = new RollAnimator();
		for (int i = 0; i < 40; i++) {
			animator.tick(180.0F, true);
			assertTrue(animator.roll(1.0F) <= 180.0F);
		}
		for (int i = 0; i < 40; i++) {
			animator.tick(8.0F, true);
			assertTrue(animator.roll(1.0F) >= 8.0F);
		}
	}

	@Test
	void instantFlipWhenSmoothingIsOff() {
		RollAnimator animator = new RollAnimator();
		animator.tick(180.0F, false);
		assertEquals(180.0F, animator.roll(1.0F));
	}

	@Test
	void interpolatesBetweenTicks() {
		RollAnimator animator = new RollAnimator();
		animator.tick(180.0F, true);
		float previous = animator.roll(0.0F);
		float current = animator.roll(1.0F);
		assertEquals((previous + current) / 2.0F, animator.roll(0.5F), 1e-4);
	}

	@Test
	void snapSkipsAnimation() {
		RollAnimator animator = new RollAnimator();
		animator.snap(180.0F);
		assertEquals(180.0F, animator.roll(0.0F));
		assertEquals(180.0F, animator.roll(1.0F));
	}

	@Test
	void upsideDownPastNinetyDegrees() {
		assertFalse(RollAnimator.isUpsideDown(0.0F));
		assertFalse(RollAnimator.isUpsideDown(89.0F));
		assertTrue(RollAnimator.isUpsideDown(91.0F));
		assertTrue(RollAnimator.isUpsideDown(188.0F));
		assertFalse(RollAnimator.isUpsideDown(-8.0F));
	}

	@Test
	void targetCombinesGravityAndTilt() {
		assertEquals(0.0F, RollAnimator.target(false, 0.0F));
		assertEquals(188.0F, RollAnimator.target(true, 8.0F));
		assertEquals(-8.0F, RollAnimator.target(false, -8.0F));
	}
}
