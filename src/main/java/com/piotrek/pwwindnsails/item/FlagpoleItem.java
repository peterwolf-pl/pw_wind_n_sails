package com.piotrek.pwwindnsails.item;

import com.piotrek.pwwindnsails.block.FlagpoleBlock;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FlagpoleItem extends BlockItem {
	public FlagpoleItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clickedPos = context.getClickedPos();
		Direction clickedFace = context.getClickedFace();

		BlockPos basePos = clickedFace == Direction.UP ? clickedPos.above() : clickedPos.relative(clickedFace);

		// Must have a sturdy block underneath
		BlockPos belowPos = basePos.below();
		if (!level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP)) {
			return InteractionResult.FAIL;
		}

		// Check that all 7 vertical blocks are clear and replaceable
		for (int i = 0; i < FlagpoleBlock.TOTAL_PARTS; i++) {
			BlockPos checkPos = basePos.above(i);
			BlockState state = level.getBlockState(checkPos);
			if (!state.canBeReplaced()) {
				return InteractionResult.FAIL;
			}
		}

		// Place all 7 parts of the tall mast
		BlockState baseState = this.getBlock().defaultBlockState();
		for (int i = 0; i < FlagpoleBlock.TOTAL_PARTS; i++) {
			level.setBlock(basePos.above(i), baseState.setValue(FlagpoleBlock.PART, i), Block.UPDATE_ALL);
		}

		level.playSound(
			null, basePos.getX() + 0.5, basePos.getY() + 0.5, basePos.getZ() + 0.5,
			SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F
		);

		if (!level.isClientSide() && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.pw_wind_n_sails.flagpole.desc"));
	}
}
