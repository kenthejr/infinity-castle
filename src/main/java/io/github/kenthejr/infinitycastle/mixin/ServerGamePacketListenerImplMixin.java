package io.github.kenthejr.infinitycastle.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	/** The server forgets a fall whenever the player moves up, but with inverted gravity moving up <em>is</em> falling. */
	@WrapOperation(
		method = "handlePlayerPositionChange",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetFallDistance()V")
	)
	private void infinitycastle$keepFallingUp(ServerPlayer player, Operation<Void> original) {
		if (!GravityController.isInverted(player)) {
			original.call(player);
		}
	}
}
