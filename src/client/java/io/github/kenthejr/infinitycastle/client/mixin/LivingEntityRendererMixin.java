package io.github.kenthejr.infinitycastle.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** Entities walking on a ceiling are drawn upside down, the same way vanilla draws a mob named Dinnerbone. */
	@ModifyReturnValue(method = "isEntityUpsideDown", at = @At("RETURN"))
	private boolean infinitycastle$upsideDownWhenInverted(boolean original, LivingEntity entity) {
		return original || GravityController.isInverted(entity);
	}
}
