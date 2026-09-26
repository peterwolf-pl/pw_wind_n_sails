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

		// Arrow color: luminous aqua/cyan, shifts to warm gold during gust
		int r = isGusting ? 255 : 140;
		int g = isGusting ? 220 : 230;
		int b = isGusting ? 130 : 255;
		int a = isGusting ? 220 : 170;

		float animTime = (gameTime + partialTick) * (windStr * 0.12F);
		float arrowLength = 0.8F + windStr * 1.2F;
		float barbLength = 0.35F;

		context.submitNodeCollector().submitCustomGeometry(
			poseStack,
			RenderTypes.linesTranslucent(),
			(pose, consumer) -> {
				for (int dx = -radius; dx <= radius; dx += step) {
					for (int dz = -radius; dz <= radius; dz += step) {
						if (dx * dx + dz * dz > radius * radius) {
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

								// Draw main arrow shaft
								drawLine(consumer, pose, startX, startY, startZ, endX, startY, endZ, r, g, b, a);

								// Draw left barb
								double barbLeftX = endX - (dirX * barbLength) + (perpX * barbLength * 0.55);
								double barbLeftZ = endZ - (dirZ * barbLength) + (perpZ * barbLength * 0.55);
								drawLine(consumer, pose, endX, startY, endZ, barbLeftX, startY, barbLeftZ, r, g, b, a);

								// Draw right barb
								double barbRightX = endX - (dirX * barbLength) - (perpX * barbLength * 0.55);
								double barbRightZ = endZ - (dirZ * barbLength) - (perpZ * barbLength * 0.55);
								drawLine(consumer, pose, endX, startY, endZ, barbRightX, startY, barbRightZ, r, g, b, a);

								break;
							}
						}
					}
				}
			}
		);
	}

	private static void drawLine(
		VertexConsumer consumer,
		PoseStack.Pose pose,
		double x1, double y1, double z1,
		double x2, double y2, double z2,
		int r, int g, int b, int a
	) {
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		float nx = len > 1e-4F ? dx / len : 0.0F;
		float ny = len > 1e-4F ? dy / len : 1.0F;
		float nz = len > 1e-4F ? dz / len : 0.0F;

		consumer.addVertex(pose, (float) x1, (float) y1, (float) z1)
			.setColor(r, g, b, a)
			.setNormal(pose, nx, ny, nz)
			.setLineWidth(2.5F);

		consumer.addVertex(pose, (float) x2, (float) y2, (float) z2)
			.setColor(r, g, b, a)
			.setNormal(pose, nx, ny, nz)
			.setLineWidth(2.5F);
	}
}
