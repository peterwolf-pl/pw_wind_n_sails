package com.piotrek.pwwindnsails.block;

import com.piotrek.pwwindnsails.block.entity.FlagpoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 4-block high mast/flagpole with an animated wind flag on top.
 * Part 0 = base on ground, Part 1 & 2 = middle pole segments, Part 3 = masthead finial with flag.
 */
public class FlagpoleBlock extends Block implements EntityBlock {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

	private static final VoxelShape POLE_SHAPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
	private static final VoxelShape BASE_SHAPE = Shapes.or(
		Block.box(4.0, 0.0, 4.0, 12.0, 3.0, 12.0),
		Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5)
	);
	private static final VoxelShape TOP_SHAPE = Shapes.or(
		Block.box(6.5, 0.0, 6.5, 9.5, 14.0, 9.5),
		Block.box(5.0, 14.0, 5.0, 11.0, 16.0, 11.0)
	);

	public FlagpoleBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int part = state.getValue(PART);
		if (part == 0) return BASE_SHAPE;
		if (part == 3) return TOP_SHAPE;
		return POLE_SHAPE;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		int part = state.getValue(PART);
		if (part == 0) {
			BlockPos below = pos.below();
			return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
		} else {
			BlockState belowState = level.getBlockState(pos.below());
			return belowState.is(this) && belowState.getValue(PART) == part - 1;
		}
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader levelReader,
		ScheduledTickAccess scheduledTickAccess,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		int part = state.getValue(PART);
		if (direction == Direction.DOWN && part > 0) {
			if (!neighborState.is(this) || neighborState.getValue(PART) != part - 1) {
				return Blocks.AIR.defaultBlockState();
			}
		}
		if (direction == Direction.UP && part < 3) {
			if (!neighborState.is(this) || neighborState.getValue(PART) != part + 1) {
				return Blocks.AIR.defaultBlockState();
			}
		}
		return super.updateShape(state, levelReader, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			int part = state.getValue(PART);
			BlockPos basePos = pos.below(part);
			// Clean up all 4 segments so no floating mast parts remain
			for (int i = 0; i <= 3; i++) {
				BlockPos checkPos = basePos.above(i);
				if (checkPos.equals(pos)) continue;
				BlockState s = level.getBlockState(checkPos);
				if (s.is(this)) {
					level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
					level.levelEvent(player, 2001, checkPos, Block.getId(s));
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		// Top finial segment (part 3) hosts the FlagpoleBlockEntity to render the dynamic wind flag
		if (state.getValue(PART) == 3) {
			return new FlagpoleBlockEntity(pos, state);
		}
		return null;
	}
}
