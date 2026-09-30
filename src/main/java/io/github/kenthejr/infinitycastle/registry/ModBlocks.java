package io.github.kenthejr.infinitycastle.registry;

import io.github.kenthejr.infinitycastle.InfinityCastle;
import io.github.kenthejr.infinitycastle.block.PaperLanternBlock;
import io.github.kenthejr.infinitycastle.block.TatamiBlock;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final Block TATAMI = register(
		"tatami",
		TatamiBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.8F).sound(SoundType.GRASS).ignitedByLava()
	);
	public static final Block SHOJI = register(
		"shoji",
		Block::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.SAND)
			.strength(0.6F)
			.sound(SoundType.BAMBOO_WOOD)
			.lightLevel(state -> 11)
			.ignitedByLava()
	);
	public static final Block SHOJI_SCREEN = register(
		"shoji_screen",
		IronBarsBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.SAND)
			.strength(0.4F)
			.sound(SoundType.BAMBOO_WOOD)
			.noOcclusion()
			.ignitedByLava()
	);
	public static final Block LACQUERED_PLANKS = register(
		"lacquered_planks",
		Block::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.ignitedByLava()
	);
	public static final Block PAPER_LANTERN = register(
		"paper_lantern",
		PaperLanternBlock::new,
		BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).mapColor(MapColor.COLOR_RED).sound(SoundType.WOOL).strength(0.5F)
	);

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, InfinityCastle.id(name));
		return Blocks.register(key, factory, properties);
	}

	public static void init() {
		// Classloading registers the blocks.
	}
}
