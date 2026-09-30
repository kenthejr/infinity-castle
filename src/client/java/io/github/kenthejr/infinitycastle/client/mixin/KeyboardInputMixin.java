package io.github.kenthejr.infinitycastle.client.mixin;

import io.github.kenthejr.infinitycastle.client.CastleCamera;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	/** Strafe keys follow the screen: with the view upside down, the screen's left is the world's right. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void infinitycastle$mirrorStrafe(CallbackInfo ci) {
		if (CastleCamera.shouldMirrorControls()) {
			this.moveVector = new Vec2(-this.moveVector.x, this.moveVector.y);
		}
	}
}
