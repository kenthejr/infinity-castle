package io.github.kenthejr.infinitycastle.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kenthejr.infinitycastle.client.CastleCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	/**
	 * A camera rolled 180° mirrors the screen on both axes, so dragging right would turn you left. Mirror the input so
	 * the view follows the mouse.
	 */
	@WrapOperation(
		method = "turnPlayer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
	)
	private void infinitycastle$mirrorTurn(LocalPlayer player, double yRot, double xRot, Operation<Void> original) {
		if (CastleCamera.shouldMirrorControls()) {
			original.call(player, -yRot, -xRot);
		} else {
			original.call(player, yRot, xRot);
		}
	}
}
