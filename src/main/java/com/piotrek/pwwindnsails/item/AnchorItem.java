package com.piotrek.pwwindnsails.item;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.AnchorEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AnchorItem extends Item {
	public AnchorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			Vec3 look = player.getLookAngle();
			Vec3 eyePos = player.getEyePosition();
			Vec3 spawnPos = eyePos.add(look.scale(0.8));

			AnchorEntity anchor = new AnchorEntity(WindAndSailsMod.ANCHOR_ENTITY, level);
			anchor.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

			// Ballistic launch velocity: 45° upward gives max throw range of ~10 blocks
			double throwSpeed = 0.62;
			anchor.setDeltaMovement(look.x * throwSpeed, look.y * throwSpeed, look.z * throwSpeed);
			level.addFreshEntity(anchor);

			// Connect leash from anchor to player
			anchor.setLeashedTo(player, true);

			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}

			// Place lead into hand if the hand was emptied
			if (player.getItemInHand(hand).isEmpty()) {
				player.setItemInHand(hand, new ItemStack(Items.LEAD));
			}

			player.swing(hand, SwingAnimation.DEFAULT, true);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.7F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);

			player.sendOverlayMessage(
				Component.translatable("hud.pw_wind_n_sails.anchor.thrown")
			);
		}
		return InteractionResult.SUCCESS;
	}
}
