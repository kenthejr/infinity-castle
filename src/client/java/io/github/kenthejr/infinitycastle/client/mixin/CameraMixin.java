package io.github.kenthejr.infinitycastle.client.mixin;

import io.github.kenthejr.infinitycastle.client.CastleCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
	/**
	 * Vanilla builds the camera orientation with a roll of zero. Supplying our roll here turns the view, the frustum and
	 * the sound listener together, so everything stays consistent.
	 */
	@ModifyArg(
		method = "setRotation",
		at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", remap = false),
		index = 2
	)
	private float infinitycastle$roll(float roll) {
		return roll + CastleCamera.roll() * (float) (Math.PI / 180.0);
	}
}
