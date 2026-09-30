package io.github.kenthejr.infinitycastle.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A chōchin: a tall, ribbed paper lantern that stands on a surface or hangs from a cord. */
public class PaperLanternBlock extends LanternBlock {
	private static final VoxelShape SHAPE_STANDING = Shapes.or(Block.column(8.0, 1.0, 13.0), Block.column(6.0, 0.0, 14.0));
	private static final VoxelShape SHAPE_HANGING = Shapes.or(Block.column(8.0, 3.0, 15.0), Block.column(6.0, 2.0, 16.0));

	public PaperLanternBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HANGING) ? SHAPE_HANGING : SHAPE_STANDING;
	}
}
