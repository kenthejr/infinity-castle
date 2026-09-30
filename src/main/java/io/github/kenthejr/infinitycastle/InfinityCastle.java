package io.github.kenthejr.infinitycastle;

import io.github.kenthejr.infinitycastle.command.CastleCommands;
import io.github.kenthejr.infinitycastle.gravity.GravityController;
import io.github.kenthejr.infinitycastle.registry.ModBlocks;
import io.github.kenthejr.infinitycastle.registry.ModItems;
import io.github.kenthejr.infinitycastle.world.CastleChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InfinityCastle implements ModInitializer {
	public static final String MOD_ID = "infinitycastle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModItems.init();
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("castle"), CastleChunkGenerator.CODEC);
		CastleCommands.init();
		GravityController.init();
		LOGGER.info("The biwa sounds. The Infinity Castle unfolds.");
	}
}
