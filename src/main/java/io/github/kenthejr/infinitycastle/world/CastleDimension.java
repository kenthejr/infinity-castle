package io.github.kenthejr.infinitycastle.world;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** The castle dimension. Its type, biome and generator are defined as data in {@code data/infinitycastle}. */
public final class CastleDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, InfinityCastle.id("infinity_castle"));

	private CastleDimension() {
	}

	public static boolean is(@Nullable Level level) {
		return level != null && level.dimension() == LEVEL;
	}
}
