package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.entity.MooringAim;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.network.MooringClickPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Optional;

public final class SailboatCleatHelper {
	private SailboatCleatHelper() {}

	public static void extractCleatStates(SailboatEntity entity, SailboatRenderState state, float partialTick) {
		// 1. Cleat tied status
		for (int i = 0; i < SailboatEntity.CLEAT_COUNT; i++) {
			state.cleatTied[i] = entity.isCleatAttached(i);
		}

		// 2. Check if local player is holding a lead
		Minecraft mc = Minecraft.getInstance();
		Player localPlayer = mc.player;
		boolean holdingLead = false;
		int targeted = -1;

		if (localPlayer != null) {
			holdingLead = localPlayer.getMainHandItem().is(Items.LEAD) || localPlayer.getOffhandItem().is(Items.LEAD);
			if (holdingLead) {
				targeted = getTargetedCleat(entity, localPlayer, partialTick);
			}
		}

		state.holdingLead = holdingLead;
		state.targetedCleat = targeted;
		state.fenderPort = entity.hasPortDockFenders();
		state.fenderStarboard = entity.hasStarboardDockFenders();

		// 3. Extract 3D leash states for each tied cleat
		if (state.leashStates == null) {
			state.leashStates = new ArrayList<>();
		} else {
			state.leashStates.clear();
		}

		Vec3 boatPos = entity.getPosition(partialTick);
		float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

		for (int i = 0; i < SailboatEntity.CLEAT_COUNT; i++) {
			if (!state.cleatTied[i]) {
				continue;
			}

			Optional<BlockPos> knotPos = entity.getCleatKnotPos(i);
			Entity holder = entity.getCleatHolderEntity(i);

			if (knotPos.isPresent() || (holder != null && holder.isAlive())) {
				EntityRenderState.LeashState leashState = new EntityRenderState.LeashState();
				Vec3 cleatWorld = entity.getCleatWorldPos(i, boatPos, yaw);

				leashState.offset = cleatWorld.subtract(boatPos);
				leashState.start = cleatWorld;

				if (knotPos.isPresent()) {
					BlockPos kp = knotPos.get();
					leashState.end = new Vec3(kp.getX() + 0.5, kp.getY() + 0.5 + 0.2, kp.getZ() + 0.5);
				} else {
					leashState.end = holder.getRopeHoldPosition(partialTick);
				}

				BlockPos startPos = BlockPos.containing(cleatWorld);
				BlockPos endPos = BlockPos.containing(leashState.end);

				leashState.startBlockLight = entity.level().getBrightness(LightLayer.BLOCK, startPos);
				leashState.startSkyLight = entity.level().getBrightness(LightLayer.SKY, startPos);
				leashState.endBlockLight = entity.level().getBrightness(LightLayer.BLOCK, endPos);
				leashState.endSkyLight = holder != null
					? holder.level().getBrightness(LightLayer.SKY, endPos)
					: entity.level().getBrightness(LightLayer.SKY, endPos);

				double dist = cleatWorld.distanceTo(leashState.end);
				leashState.slack = dist < 3.5;

				state.leashStates.add(leashState);
			}
		}
	}

	public static int getTargetedCleat(SailboatEntity entity, Player player, float partialTick) {
		MooringAim.Target aim = MooringAim.find(player, entity.level(), partialTick);
		if (aim != null && aim.cleat() && aim.boatId() == entity.getId()) {
			return aim.cleatIndex();
		}
		return -1;
	}

	/** Same single target the black wireframe is drawing. Neighbors are not clickable. */
	public static MooringClickPayload findClickTarget(Player player, net.minecraft.world.level.Level level) {
		MooringAim.Target aim = MooringAim.find(player, level, 1.0F);
		if (aim == null || !SailboatEntity.shouldOwnMooringClick(player, aim)) {
			return null;
		}
		if (aim.cleat()) {
			return new MooringClickPayload(true, aim.boatId(), aim.cleatIndex(), BlockPos.ZERO);
		}
		return new MooringClickPayload(false, -1, -1, aim.fence());
	}
}
