package com.piotrek.pwwindnsails.item;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.AnchorEntity;
import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BuoyItem extends Item {
	private final int buoyType;

	public BuoyItem(int buoyType, Properties properties) {
		super(properties);
		this.buoyType = buoyType;
	}

	public int getBuoyType() {
		return this.buoyType;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			Vec3 look = player.getLookAngle();
			Vec3 eyePos = player.getEyePosition();
			Vec3 spawnPos = eyePos.add(look.scale(0.8));

			// Spawn Buoy
			BuoyEntity buoy = new BuoyEntity(WindAndSailsMod.BUOY_ENTITY, level);
			buoy.setBuoyType(this.buoyType);
			buoy.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
			if (stack.has(DataComponents.CUSTOM_NAME)) {
				buoy.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
				buoy.setCustomNameVisible(true);
			}

			double throwSpeed = 0.58;
			buoy.setDeltaMovement(look.x * throwSpeed, look.y * throwSpeed, look.z * throwSpeed);

			// Spawn Anchor
			AnchorEntity anchor = new AnchorEntity(WindAndSailsMod.ANCHOR_ENTITY, level);
			anchor.setPos(spawnPos.x, spawnPos.y - 0.2, spawnPos.z);
			anchor.setDeltaMovement(look.x * throwSpeed * 0.7, Math.min(-0.15, look.y * throwSpeed), look.z * throwSpeed * 0.7);

			level.addFreshEntity(anchor);
			level.addFreshEntity(buoy);

			buoy.setAnchor(anchor);
			anchor.setLeashedTo(buoy, true);

			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}

			player.swing(hand, SwingAnimation.DEFAULT, true);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.7F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.9F, 1.0F);

			String key = this.buoyType == BuoyEntity.TYPE_BLUE_CYLINDRICAL
				? "hud.pw_wind_n_sails.buoy.cylindrical_thrown"
				: "hud.pw_wind_n_sails.buoy.thrown";
			player.sendOverlayMessage(Component.translatable(key));
		}
		return InteractionResult.SUCCESS;
	}
}
