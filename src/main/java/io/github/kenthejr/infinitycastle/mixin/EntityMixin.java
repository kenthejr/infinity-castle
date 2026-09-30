package io.github.kenthejr.infinitycastle.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.gravity.InvertedCollision;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Teaches entity movement that "down" is up for entities with negative gravity. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	public boolean verticalCollision;

	@Shadow
	public boolean verticalCollisionBelow;

	@Shadow
	private EntityDimensions dimensions;

	@Shadow
	public abstract AABB getBoundingBox();

	@Shadow
	public abstract Level level();

	@Shadow
	public abstract float maxUpStep();

	@Shadow
	public abstract boolean onGround();

	@Shadow
	private static List<VoxelShape> collectCollidersIgnoringWorldBorder(Entity source, Level level, List<VoxelShape> entityColliders, AABB boundingBox) {
		throw new AssertionError();
	}

	@Shadow
	private static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
		throw new AssertionError();
	}

	private boolean infinitycastle$inverted() {
		return GravityController.isInverted((Entity) (Object) this);
	}

	/** Touching a ceiling while falling up counts as landing on the ground. */
	@WrapOperation(
		method = "move",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V")
	)
	private void infinitycastle$groundAbove(
		Entity self, boolean onGround, boolean horizontalCollision, Vec3 movement, Operation<Void> original, @Local(argsOnly = true) Vec3 delta
	) {
		if (this.infinitycastle$inverted()) {
			this.verticalCollisionBelow = this.verticalCollision && delta.y > 0.0;
			onGround = this.verticalCollisionBelow;
		}
		original.call(self, onGround, horizontalCollision, movement);
	}

	/** Step "up" onto blocks hanging from the ceiling, by running vanilla's step logic mirrored. */
	@Inject(method = "collide", at = @At("HEAD"), cancellable = true)
	private void infinitycastle$collideInverted(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
		if (!this.infinitycastle$inverted()) {
			return;
		}
		Entity self = (Entity) (Object) this;
		cir.setReturnValue(InvertedCollision.collide(
			movement,
			this.getBoundingBox(),
			this.maxUpStep(),
			this.onGround(),
			(box) -> {
				List<VoxelShape> entityColliders = this.level().getEntityCollisions(self, box);
				return collectCollidersIgnoringWorldBorder(self, this.level(), entityColliders, box);
			},
			EntityMixin::collideWithShapes
		));
	}

	/** Falling up accumulates fall distance just like falling down. */
	@ModifyVariable(method = "checkFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double infinitycastle$fallUp(double ya) {
		return this.infinitycastle$inverted() ? -ya : ya;
	}

	/**
	 * With gravity inverted the player's feet are pressed to the ceiling, so the eyes belong near the bottom of the
	 * bounding box, the same distance from the "floor" as usual. This moves both the camera and every raycast.
	 */
	@ModifyExpressionValue(
		method = {"refreshDimensions", "fixupDimensions"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityDimensions;eyeHeight()F")
	)
	private float infinitycastle$invertedEyeHeight(float eyeHeight) {
		if (!this.infinitycastle$inverted()) {
			return eyeHeight;
		}
		return this.dimensions.height() - eyeHeight;
	}
}
