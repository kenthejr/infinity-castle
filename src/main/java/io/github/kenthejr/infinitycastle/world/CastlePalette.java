package io.github.kenthejr.infinitycastle.world;

import io.github.kenthejr.infinitycastle.block.FusumaBlock;
import io.github.kenthejr.infinitycastle.gen.Material;
import io.github.kenthejr.infinitycastle.gen.Piece;
import io.github.kenthejr.infinitycastle.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
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
			case SPRUCE_PLANKS -> Blocks.SPRUCE_PLANKS;
			case DARK_OAK_PLANKS -> Blocks.DARK_OAK_PLANKS;
			case OAK_PLANKS -> Blocks.OAK_PLANKS;
			case OAK_SLAB -> Blocks.OAK_SLAB;
			case SPRUCE_SLAB -> Blocks.SPRUCE_SLAB;
			case SPRUCE_STAIRS -> Blocks.SPRUCE_STAIRS;
			case DARK_OAK_STAIRS -> Blocks.DARK_OAK_STAIRS;
			case DARK_OAK_TRAPDOOR -> Blocks.DARK_OAK_TRAPDOOR;
			case OAK_FENCE -> Blocks.OAK_FENCE;
			case SPRUCE_FENCE -> Blocks.SPRUCE_FENCE;
			case SPRUCE_FENCE_GATE -> Blocks.SPRUCE_FENCE_GATE;
			case STRIPPED_BIRCH_LOG -> Blocks.STRIPPED_BIRCH_LOG;
			case LANTERN -> Blocks.LANTERN;
			case FUSUMA -> ModBlocks.FUSUMA;
		};
	}

	public static BlockState state(Piece piece) {
		BlockState state = block(piece.material()).defaultBlockState();
		return switch (piece.material().shape()) {
			case STAIRS -> state
				.setValue(StairBlock.FACING, direction(piece))
				.setValue(StairBlock.HALF, half(piece));
			case SLAB -> state.setValue(SlabBlock.TYPE, piece.top() ? SlabType.TOP : SlabType.BOTTOM);
			case TRAPDOOR -> state
				.setValue(TrapDoorBlock.FACING, direction(piece))
				.setValue(TrapDoorBlock.HALF, half(piece))
				.setValue(TrapDoorBlock.OPEN, true);
			case GATE -> state.setValue(FenceGateBlock.FACING, direction(piece));
			case PANEL -> state.setValue(FusumaBlock.FACING, direction(piece));
			case LANTERN -> state.setValue(LanternBlock.HANGING, piece.top());
			case AXIS -> state.setValue(RotatedPillarBlock.AXIS, axis(piece.axis()));
			default -> state;
		};
	}

	/** Fences need their connections, and stairs their corner shapes, computed once their neighbours exist. */
	public static boolean needsPostProcessing(Piece piece) {
		Material.Shape shape = piece.material().shape();
		return shape == Material.Shape.FENCE || shape == Material.Shape.STAIRS;
	}

	private static Half half(Piece piece) {
		return piece.top() ? Half.TOP : Half.BOTTOM;
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
