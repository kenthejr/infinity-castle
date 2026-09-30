package io.github.kenthejr.infinitycastle.client;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import io.github.kenthejr.infinitycastle.gravity.FloorProfile;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.gravity.RollAnimator;
import io.github.kenthejr.infinitycastle.world.CastleDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

/** Client-side camera state: the roll that turns the world over when gravity flips, and the castle colour grade. */
public final class CastleCamera {
	public static final Identifier COLOR_GRADE = InfinityCastle.id("castle_grade");

	private static final RollAnimator ROLL = new RollAnimator();
	private static boolean wasInCastle;

	private CastleCamera() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		boolean inCastle = player != null && CastleDimension.is(player.level());
		float target = inCastle ? targetRoll(player) : 0.0F;
		if (inCastle != wasInCastle) {
			// Arriving or leaving is a teleport; don't animate across it.
			ROLL.snap(target);
			wasInCastle = inCastle;
		} else {
			ROLL.tick(target, ClientConfig.get().smoothFlip);
		}
	}

	private static float targetRoll(LocalPlayer player) {
		ClientConfig.Values config = ClientConfig.get();
		boolean inverted = config.rollCameraWhenInverted && GravityController.isInverted(player);
		float tilt = config.floorTilt ? FloorProfile.at(GravityController.centerY(player)).cameraTilt() : 0.0F;
		return RollAnimator.target(inverted, tilt);
	}

	/** Current roll in degrees, interpolated for this frame. */
	public static float roll() {
		if (!wasInCastle) {
			return 0.0F;
		}
		return ROLL.roll(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
	}

	/** Whether the view is currently turned more than halfway over. */
	public static boolean isViewUpsideDown() {
		return wasInCastle && RollAnimator.isUpsideDown(roll());
	}

	public static boolean shouldMirrorControls() {
		return ClientConfig.get().mirrorControlsWhenInverted && isViewUpsideDown();
	}

	public static boolean shouldApplyColorGrade() {
		Minecraft minecraft = Minecraft.getInstance();
		return ClientConfig.get().colorGrade && minecraft.level != null && CastleDimension.is(minecraft.level);
	}
}
