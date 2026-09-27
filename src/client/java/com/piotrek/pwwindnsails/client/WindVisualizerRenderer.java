package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindGust;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindState;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side wind visualizer that renders aerodynamic wind arrows over water surfaces.
 * Toggled via TAB (or configurable keybind).
 */
public final class WindVisualizerRenderer {
	private static boolean enabled = false;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static void toggle() {
		enabled = !enabled;
	}

	public static void render(LevelRenderContext context) {
		if (!enabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			return;
		}

		Vec3 camPos = context.levelState().cameraRenderState.pos;
		if (camPos == null) {
			return;
		}

		long gameTime = level.getGameTime();
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		WindState windState = WindManager.getInstance().getWindState(level);
		WindVector wind = windState.getWindVector(gameTime);
		WindGust gust = windState.getCurrentGust();
		boolean isGusting = gust != null && !gust.isExpired(gameTime);

		float windDeg = wind.directionDeg();
		float windStr = wind.strength();
		if (windStr < 0.04F) {
			// Flauta / Calm doldrums: clear glassy water without wind streaks
			return;
		}

		float windRad = windDeg * Mth.DEG_TO_RAD;
		// Vector pointing in direction wind blows
		float dirX = -Mth.sin(windRad);
		float dirZ = Mth.cos(windRad);

		// Perpendicular vector for arrow barbs
		float perpX = -dirZ;
		float perpZ = dirX;

		int radius = (int) WindAndSailsConfig.VISUALIZER_RADIUS;
		int step = 3; // Grid spacing in blocks

		int playerX = Mth.floor(camPos.x);
		int playerY = Mth.floor(camPos.y);
		int playerZ = Mth.floor(camPos.z);

		PoseStack poseStack = context.poseStack();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		// Realistic water darkening (cat's paws / wind ripples):
		// Deep midnight marine shadow that darkens the water surface beneath it
		float alphaFactor = Mth.clamp((windStr - 0.04F) / 0.12F, 0.0F, 1.0F);
		int r = isGusting ? 6 : 8;
		int g = isGusting ? 18 : 24;
		int b = isGusting ? 38 : 48;
		int maxAlpha = isGusting ? 82 : 56;

		float animTime = (gameTime + partialTick) * (windStr * 0.12F);
		float arrowLength = 1.1F + windStr * 1.5F;
		float width = 0.28F + windStr * 0.16F;
		float hw = width * 0.5F;
		float headLen = arrowLength * 0.40F;

		context.submitNodeCollector().submitCustomGeometry(
			poseStack,
			RenderTypes.debugQuads(),
			(pose, consumer) -> {
				for (int dx = -radius; dx <= radius; dx += step) {
					for (int dz = -radius; dz <= radius; dz += step) {
						int distSq = dx * dx + dz * dz;
						if (distSq > radius * radius) {
							continue;
						}

						// Soft distance fade towards the perimeter
						float distRatio = (float) Math.sqrt(distSq) / (float) radius;
						float distFade = Mth.clamp(1.0F - distRatio * distRatio, 0.0F, 1.0F);
						int a = (int) (maxAlpha * alphaFactor * distFade);
						if (a < 3) {
							continue;
						}

						int wx = playerX + dx;
						int wz = playerZ + dz;

						// Find water surface near player level
						for (int dy = -6; dy <= 6; dy++) {
							pos.set(wx, playerY + dy, wz);
							FluidState fluid = level.getFluidState(pos);
							if (fluid.is(FluidTags.WATER) && level.getBlockState(pos.above()).isAir()) {
								double surfaceY = pos.getY() + fluid.getHeight(level, pos) + WindAndSailsConfig.VISUALIZER_ARROW_HEIGHT_OFFSET;

								// Subtle animated offset along wind flow
								float cellOffset = ((animTime + (wx * 11 + wz * 17) * 0.05F) % 1.0F) * step - (step * 0.5F);
								double startX = wx + 0.5D + dirX * cellOffset - camPos.x;
								double startY = surfaceY - camPos.y;
								double startZ = wz + 0.5D + dirZ * cellOffset - camPos.z;

								double endX = startX + dirX * arrowLength;
								double endZ = startZ + dirZ * arrowLength;
								double neckX = endX - dirX * headLen;
								double neckZ = endZ - dirZ * headLen;

								// 1. Thick main body ribbon (tapers from tail to neck)
								drawQuad(
									consumer, pose,
									startX - perpX * (hw * 0.45), startY, startZ - perpZ * (hw * 0.45),
									startX + perpX * (hw * 0.45), startY, startZ + perpZ * (hw * 0.45),
									neckX + perpX * hw, startY, neckZ + perpZ * hw,
									neckX - perpX * hw, startY, neckZ - perpZ * hw,
									r, g, b, a
								);

								// 2. Thick arrowhead chevron (broad dark ripple tip)
								drawQuad(
									consumer, pose,
									neckX - perpX * (hw * 2.3), startY, neckZ - perpZ * (hw * 2.3),
									endX, startY, endZ,
									neckX + perpX * (hw * 2.3), startY, neckZ + perpZ * (hw * 2.3),
									neckX, startY, neckZ,
									r, g, b, a
								);

								break;
							}
						}
					}
				}
			}
		);
	}

	private static void drawQuad(
		VertexConsumer consumer,
		PoseStack.Pose pose,
		double x0, double y0, double z0,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		double x3, double y3, double z3,
		int r, int g, int b, int a
	) {
		consumer.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
		consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
		consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
		consumer.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
	}
}
