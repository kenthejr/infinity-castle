package io.github.kenthejr.infinitycastle.world;

import io.github.kenthejr.infinitycastle.block.TatamiBlock;
import io.github.kenthejr.infinitycastle.gen.Material;
import io.github.kenthejr.infinitycastle.gen.Piece;
import io.github.kenthejr.infinitycastle.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Maps the generator's abstract {@link Piece}s to concrete block states. */
public final class CastlePalette {
	private CastlePalette() {
	}

	public static Block block(Material material) {
		return switch (material) {
			case AIR -> Blocks.AIR;
			case DECK -> Blocks.SPRUCE_PLANKS;
			case FLOOR_TRIM -> Blocks.DARK_OAK_PLANKS;
			case CEILING -> Blocks.DARK_OAK_PLANKS;
			case TATAMI -> ModBlocks.TATAMI;
			case TIMBER -> Blocks.STRIPPED_DARK_OAK_LOG;
			case LACQUER -> ModBlocks.LACQUERED_PLANKS;
			case SHOJI_WALL -> ModBlocks.SHOJI;
			case SHOJI_SCREEN -> ModBlocks.SHOJI_SCREEN;
			case RAIL -> Blocks.MANGROVE_FENCE;
			case ROOF_TILE -> Blocks.DEEPSLATE_TILES;
			case ROOF_STAIRS -> Blocks.DEEPSLATE_TILE_STAIRS;
			case ROOF_SLAB -> Blocks.DEEPSLATE_TILE_SLAB;
			case WOOD_STAIRS -> Blocks.DARK_OAK_STAIRS;
			case WOOD_SLAB -> Blocks.DARK_OAK_SLAB;
			case PAPER_LANTERN -> ModBlocks.PAPER_LANTERN;
		};
	}

	public static BlockState state(Piece piece) {
		BlockState state = block(piece.material()).defaultBlockState();
		return switch (piece.material().shape()) {
			case STAIRS -> state
				.setValue(StairBlock.FACING, direction(piece))
				.setValue(StairBlock.HALF, piece.top() ? Half.TOP : Half.BOTTOM);
			case SLAB -> state.setValue(SlabBlock.TYPE, piece.top() ? SlabType.TOP : SlabType.BOTTOM);
			case LANTERN -> state.setValue(LanternBlock.HANGING, piece.top());
			case AXIS -> state.setValue(RotatedPillarBlock.AXIS, axis(piece.axis()));
			case HORIZONTAL_AXIS -> state.setValue(TatamiBlock.AXIS, axis(piece.axis()));
			default -> state;
		};
	}

	/** Fences and panes need their connections computed once neighbours exist. */
	public static boolean needsPostProcessing(Piece piece) {
		return piece.material().shape() == Material.Shape.CONNECTING;
	}

	private static Direction direction(Piece piece) {
		return switch (piece.facing()) {
			case NORTH -> Direction.NORTH;
			case EAST -> Direction.EAST;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
		};
	}

	private static Direction.Axis axis(io.github.kenthejr.infinitycastle.gen.Axis axis) {
		return switch (axis) {
			case X -> Direction.Axis.X;
			case Y -> Direction.Axis.Y;
			case Z -> Direction.Axis.Z;
		};
	}
}
