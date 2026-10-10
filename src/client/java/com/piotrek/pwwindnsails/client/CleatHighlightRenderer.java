package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piotrek.pwwindnsails.entity.AnchorEntity;
import com.piotrek.pwwindnsails.entity.MooringAim;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Renders an authentic vanilla black wireframe bounding box ("czarny drut") around:
 * 1) The targeted boat mooring cleat
 * 2) The targeted dock fence post / knot
 * whenever the player is holding a lead or mooring line in hand.
 */
public final class CleatHighlightRenderer {
	private CleatHighlightRenderer() {}

	public static void render(LevelRenderContext context) {
		try {
			Minecraft client = Minecraft.getInstance();
			Player player = client.player;
			ClientLevel level = client.level;
			if (player == null || level == null || player.tickCount < 80) {
				return;
			}
			if (level.getChunkSource().getChunkNow(player.getBlockX() >> 4, player.getBlockZ() >> 4) == null) {
				return;
			}

			boolean holdsLead = player.getMainHandItem().is(Items.LEAD) || player.getOffhandItem().is(Items.LEAD);
			boolean holdsThrownAnchor = AnchorEntity.findHeldBy(player) != null;
			AABB searchBox = player.getBoundingBox().inflate(12.0);
			List<SailboatEntity> boats = level.getEntitiesOfClass(SailboatEntity.class, searchBox);

			boolean hasLineFromBoat = false;
			for (SailboatEntity b : boats) {
				if (b.isCleatAttachedTo(player)) {
					hasLineFromBoat = true;
					break;
				}
			}

			if (!holdsLead && !hasLineFromBoat && !holdsThrownAnchor) {
				return;
			}

			Vec3 camPos = context.levelState().cameraRenderState.pos;
			if (camPos == null) {
				return;
			}

			float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
			PoseStack poseStack = context.poseStack();

			MooringAim.Target aim = MooringAim.find(player, level, partialTick);
			if (aim == null) {
				return;
			}

			context.submitNodeCollector().submitCustomGeometry(
				poseStack,
				RenderTypes.lines(),
				(pose, consumer) -> {
					if (aim.cleat()) {
						if (level.getEntity(aim.boatId()) instanceof SailboatEntity boat) {
							drawCleatWire(pose, consumer, boat, aim.cleatIndex(), partialTick, camPos);
						}
						return;
					}
					if (aim.fence() == null) {
						return;
					}
					// Post column only — not the rails that run into the next fence.
					drawAabbEdges(pose, consumer, MooringAim.postColumn(aim.fence()).move(-camPos.x, -camPos.y, -camPos.z), 160);
					LeashFenceKnotEntity.getKnot(level, aim.fence()).ifPresent(knot ->
						drawAabbEdges(pose, consumer, knot.getBoundingBox().move(-camPos.x, -camPos.y, -camPos.z), 160)
					);
				}
			);
		} catch (Throwable t) {
			com.piotrek.pwwindnsails.WindAndSailsMod.LOGGER.error("Error rendering cleat highlight outline", t);
		}
	}

	private static void drawCleatWire(PoseStack.Pose pose, VertexConsumer consumer, SailboatEntity boat, int cleat, float partialTick, Vec3 camPos) {
		float boatYaw = Mth.rotLerp(partialTick, boat.yRotO, boat.getYRot());
		Vec3 boatPos = boat.getPosition(partialTick);
		float yawRad = boatYaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		AABB local = MooringAim.cleatLocalBox(boat.getCleatLocalOffset(cleat), 0.0);
		float[] rx = new float[8];
		float[] ry = new float[8];
		float[] rz = new float[8];
		Vec3[] corners = new Vec3[]{
			new Vec3(local.minX, local.minY, local.minZ),
			new Vec3(local.maxX, local.minY, local.minZ),
			new Vec3(local.maxX, local.minY, local.maxZ),
			new Vec3(local.minX, local.minY, local.maxZ),
			new Vec3(local.minX, local.maxY, local.minZ),
			new Vec3(local.maxX, local.maxY, local.minZ),
			new Vec3(local.maxX, local.maxY, local.maxZ),
			new Vec3(local.minX, local.maxY, local.maxZ)
		};
		for (int k = 0; k < 8; k++) {
			rx[k] = (float) (boatPos.x + rightX * corners[k].x + fwdX * corners[k].z - camPos.x);
			ry[k] = (float) (boatPos.y + corners[k].y - camPos.y);
			rz[k] = (float) (boatPos.z + rightZ * corners[k].x + fwdZ * corners[k].z - camPos.z);
		}
		int alpha = 160;
		drawLine(pose, consumer, rx[0], ry[0], rz[0], rx[1], ry[1], rz[1], alpha);
		drawLine(pose, consumer, rx[1], ry[1], rz[1], rx[2], ry[2], rz[2], alpha);
		drawLine(pose, consumer, rx[2], ry[2], rz[2], rx[3], ry[3], rz[3], alpha);
		drawLine(pose, consumer, rx[3], ry[3], rz[3], rx[0], ry[0], rz[0], alpha);
		drawLine(pose, consumer, rx[4], ry[4], rz[4], rx[5], ry[5], rz[5], alpha);
		drawLine(pose, consumer, rx[5], ry[5], rz[5], rx[6], ry[6], rz[6], alpha);
		drawLine(pose, consumer, rx[6], ry[6], rz[6], rx[7], ry[7], rz[7], alpha);
		drawLine(pose, consumer, rx[7], ry[7], rz[7], rx[4], ry[4], rz[4], alpha);
		drawLine(pose, consumer, rx[0], ry[0], rz[0], rx[4], ry[4], rz[4], alpha);
		drawLine(pose, consumer, rx[1], ry[1], rz[1], rx[5], ry[5], rz[5], alpha);
		drawLine(pose, consumer, rx[2], ry[2], rz[2], rx[6], ry[6], rz[6], alpha);
		drawLine(pose, consumer, rx[3], ry[3], rz[3], rx[7], ry[7], rz[7], alpha);
	}

	private static void drawAabbEdges(PoseStack.Pose pose, VertexConsumer consumer, AABB box, int a) {
		float x0 = (float) box.minX;
		float y0 = (float) box.minY;
		float z0 = (float) box.minZ;
		float x1 = (float) box.maxX;
		float y1 = (float) box.maxY;
		float z1 = (float) box.maxZ;

		drawLine(pose, consumer, x0, y0, z0, x1, y0, z0, a);
		drawLine(pose, consumer, x1, y0, z0, x1, y0, z1, a);
		drawLine(pose, consumer, x1, y0, z1, x0, y0, z1, a);
		drawLine(pose, consumer, x0, y0, z1, x0, y0, z0, a);

		drawLine(pose, consumer, x0, y1, z0, x1, y1, z0, a);
		drawLine(pose, consumer, x1, y1, z0, x1, y1, z1, a);
		drawLine(pose, consumer, x1, y1, z1, x0, y1, z1, a);
		drawLine(pose, consumer, x0, y1, z1, x0, y1, z0, a);

		drawLine(pose, consumer, x0, y0, z0, x0, y1, z0, a);
		drawLine(pose, consumer, x1, y0, z0, x1, y1, z0, a);
		drawLine(pose, consumer, x1, y0, z1, x1, y1, z1, a);
		drawLine(pose, consumer, x0, y0, z1, x0, y1, z1, a);
	}

	private static void drawLine(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int a) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;
		float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = (len > 1e-5F) ? dx / len : 0.0F;
		float ny = (len > 1e-5F) ? dy / len : 1.0F;
		float nz = (len > 1e-5F) ? dz / len : 0.0F;

		consumer.addVertex(pose, x1, y1, z1).setColor(0, 0, 0, a).setNormal(pose, nx, ny, nz).setLineWidth(2.0F);
		consumer.addVertex(pose, x2, y2, z2).setColor(0, 0, 0, a).setNormal(pose, nx, ny, nz).setLineWidth(2.0F);
	}
}
