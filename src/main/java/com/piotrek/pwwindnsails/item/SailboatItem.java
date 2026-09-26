package com.piotrek.pwwindnsails.item;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SailboatItem extends Item {
	public SailboatItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

		if (hit.getType() == HitResult.Type.MISS) {
			return InteractionResult.PASS;
		}

		if (hit.getType() == HitResult.Type.BLOCK) {
			if (!level.isClientSide()) {
				Vec3 hitPos = hit.getLocation();
				SailboatEntity boat = new SailboatEntity(WindAndSailsMod.SAILBOAT_ENTITY, level);
				boat.setPos(hitPos.x, hitPos.y + 0.1, hitPos.z);
				boat.absSnapRotationTo(player.getYRot(), 0.0F);
				level.addFreshEntity(boat);
				level.playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.BLOCKS, 1.0F, 1.0F);

				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.pw_wind_n_sails.sailboat.desc"));
	}
}
