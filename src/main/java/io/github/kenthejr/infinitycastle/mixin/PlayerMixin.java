package io.github.kenthejr.infinitycastle.mixin;

import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.gravity.GravityState;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin implements GravityState {
	@Unique
	private boolean infinitycastle$tracking;
	@Unique
	private boolean infinitycastle$inverted;
	@Unique
	private double infinitycastle$lastCenterY;
	@Unique
	private int infinitycastle$lastFloor;
	@Unique
	private boolean infinitycastle$dimensionsInverted;

	@Shadow
	public abstract boolean isLocalPlayer();

	@Inject(method = "tick", at = @At("HEAD"))
	private void infinitycastle$tickGravity(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		// Only the side that simulates this player's movement decides its gravity.
		if (!self.level().isClientSide() || this.isLocalPlayer()) {
			GravityController.tick(self);
		}
	}

	/** Sneaking keeps you from walking off the edge of what's below you, which is nothing when you stand on a ceiling. */
	@Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
	private void infinitycastle$noEdgeCheckWhenInverted(Vec3 delta, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
		if (GravityController.isInverted((Player) (Object) this)) {
			cir.setReturnValue(delta);
		}
	}

	@Override
	public boolean infinitycastle$isTracking() {
		return this.infinitycastle$tracking;
	}

	@Override
	public boolean infinitycastle$isInverted() {
		return this.infinitycastle$inverted;
	}

	@Override
	public double infinitycastle$lastCenterY() {
		return this.infinitycastle$lastCenterY;
	}

	@Override
	public int infinitycastle$lastFloor() {
		return this.infinitycastle$lastFloor;
	}

	@Override
	public void infinitycastle$update(boolean inverted, double centerY, int floor) {
		this.infinitycastle$tracking = true;
		this.infinitycastle$inverted = inverted;
		this.infinitycastle$lastCenterY = centerY;
		this.infinitycastle$lastFloor = floor;
	}

	@Override
	public boolean infinitycastle$dimensionsInverted() {
		return this.infinitycastle$dimensionsInverted;
	}

	@Override
	public void infinitycastle$setDimensionsInverted(boolean inverted) {
		this.infinitycastle$dimensionsInverted = inverted;
	}

	@Override
	public void infinitycastle$reset() {
		this.infinitycastle$tracking = false;
		this.infinitycastle$inverted = false;
	}
}
