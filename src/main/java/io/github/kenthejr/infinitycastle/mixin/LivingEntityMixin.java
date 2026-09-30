package io.github.kenthejr.infinitycastle.mixin;

import io.github.kenthejr.infinitycastle.gravity.GravityController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Jumping pushes away from the surface you stand on, which is the ceiling when gravity is inverted. */
	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	private void infinitycastle$jumpDown(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (GravityController.isInverted(self)) {
			Vec3 movement = self.getDeltaMovement();
			self.setDeltaMovement(movement.x, -movement.y, movement.z);
		}
	}
}
