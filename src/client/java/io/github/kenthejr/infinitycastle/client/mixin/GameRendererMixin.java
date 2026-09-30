package io.github.kenthejr.infinitycastle.client.mixin;

import io.github.kenthejr.infinitycastle.client.CastleCamera;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;

	/** Request the castle colour grade each frame while inside, alongside any vanilla effects. */
	@Inject(method = "update", at = @At("TAIL"))
	private void infinitycastle$colorGrade(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (CastleCamera.shouldApplyColorGrade()) {
			this.requestedPostEffects.add(CastleCamera.COLOR_GRADE);
		}
	}
}
