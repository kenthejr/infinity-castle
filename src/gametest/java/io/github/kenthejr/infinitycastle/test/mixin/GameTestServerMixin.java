package io.github.kenthejr.infinitycastle.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The vanilla game test server only creates the built-in dimensions. Normal worlds also load dimensions from data
 * packs, which is how the castle is added, so do the same here to test it.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	@WrapOperation(
		method = "lambda$create$1",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"
		)
	)
	private static WorldDimensions.Complete infinitycastle$includeDatapackDimensions(
		WorldDimensions dimensions, Registry<LevelStem> ignored, Operation<WorldDimensions.Complete> original, @Local(argsOnly = true) WorldLoader.DataLoadContext context
	) {
		return original.call(dimensions, context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
	}
}
