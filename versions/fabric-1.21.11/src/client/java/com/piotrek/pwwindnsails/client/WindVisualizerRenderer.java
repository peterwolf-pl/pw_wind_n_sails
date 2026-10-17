package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.wind.WindGust;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindState;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side wind visualizer that renders sleek aerodynamic wind arrows over water surfaces.
 * Visible exclusively while riding a sailboat.
 * Selected style is configured via /windarrow or /windsails arrow, and toggled on/off via TAB.
 */
public final class WindVisualizerRenderer {

	public enum VisualizerMode {
		SEAFOAM_WHITE(1, "1/5: Jasna piana morska (Biel)", "seafoam", 245, 252, 255, 195, 255, 255, 255, 245),
		BRIGHT_CYAN(2, "2/5: Jasny błękit (Cyjan)", "cyan", 90, 225, 255, 200, 150, 245, 255, 245),
		WARM_AMBER(3, "3/5: Bursztynowy wiatr (Złoto)", "amber", 255, 215, 95, 195, 255, 235, 140, 245),
		SOFT_SKY(4, "4/5: Subtelny błękit (Łagodny)", "sky", 155, 210, 245, 140, 185, 230, 255, 185),
		WATER_SHADOW(5, "5/5: Ciemny cień wody (Grafit)", "shadow", 12, 28, 55, 155, 8, 20, 42, 195),
		OFF(0, "Wyłączone", "off", 0, 0, 0, 0, 0, 0, 0, 0);

		private final int id;
		private final String displayName;
		private final String alias;
		private final int r, g, b, a;
		private final int gustR, gustG, gustB, gustA;

		VisualizerMode(int id, String displayName, String alias, int r, int g, int b, int a, int gustR, int gustG, int gustB, int gustA) {
			this.id = id;
			this.displayName = displayName;
			this.alias = alias;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.gustR = gustR;
			this.gustG = gustG;
			this.gustB = gustB;
			this.gustA = gustA;
		}

		public int getId() {
			return this.id;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public String getAlias() {
			return this.alias;
		}

		public boolean isEnabled() {
			return this != OFF;
		}

		public int getR(boolean gusting) {
			return gusting ? this.gustR : this.r;
		}

		public int getG(boolean gusting) {
			return gusting ? this.gustG : this.g;
		}

		public int getB(boolean gusting) {
			return gusting ? this.gustB : this.b;
		}

		public int getA(boolean gusting) {
			return gusting ? this.gustA : this.a;
		}

		public static VisualizerMode fromInput(String input) {
			if (input == null) return null;
			String trimmed = input.trim().toLowerCase();
			for (VisualizerMode m : values()) {
				if (m == OFF) continue;
				if (String.valueOf(m.id).equals(trimmed) || m.alias.equalsIgnoreCase(trimmed) || m.name().equalsIgnoreCase(trimmed)) {
					return m;
				}
			}
			return null;
		}
	}

	private static VisualizerMode selectedStyle = VisualizerMode.SEAFOAM_WHITE;
	private static VisualizerMode currentMode = VisualizerMode.SEAFOAM_WHITE;

	public static VisualizerMode getSelectedStyle() {
		return selectedStyle;
	}

	public static void setSelectedStyle(VisualizerMode style) {
		if (style != null && style != VisualizerMode.OFF) {
			selectedStyle = style;
			currentMode = style;
		}
	}

	public static VisualizerMode getCurrentMode() {
		return currentMode;
	}

	public static void setMode(VisualizerMode mode) {
		currentMode = mode != null ? mode : VisualizerMode.OFF;
		if (currentMode != VisualizerMode.OFF) {
			selectedStyle = currentMode;
		}
	}

	public static boolean isEnabled() {
		return currentMode.isEnabled();
	}

	/**
	 * Toggles between the selected arrow style and OFF.
	 * TAB now strictly toggles the selected style on/off instead of cycling through all styles.
	 */
	public static boolean toggleSelected() {
		if (currentMode == VisualizerMode.OFF) {
			currentMode = selectedStyle;
			return true;
		} else {
			currentMode = VisualizerMode.OFF;
			return false;
		}
	}

	public static void toggle() {
		toggleSelected();
	}

	public static VisualizerMode cycleMode() {
		VisualizerMode[] values = VisualizerMode.values();
		int nextIndex = (currentMode.ordinal() + 1) % values.length;
		currentMode = values[nextIndex];
		if (currentMode != VisualizerMode.OFF) {
			selectedStyle = currentMode;
		}
		return currentMode;
	}

	public static void render(WorldRenderContext context) {
		if (!currentMode.isEnabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null || client.player == null || client.player.tickCount < 80) {
			return;
		}
		if (level.getChunkSource().getChunkNow(client.player.getBlockX() >> 4, client.player.getBlockZ() >> 4) == null) {
			return;
		}

		Vec3 camPos = context.worldState().cameraRenderState.pos;
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

		// Perpendicular vector for width & chevron wings
		float perpX = -dirZ;
		float perpZ = dirX;

		int radius = (int) WindAndSailsConfig.VISUALIZER_RADIUS;
		int step = 3; // Grid spacing in blocks

		int playerX = Mth.floor(camPos.x);
		int playerY = Mth.floor(camPos.y);
		int playerZ = Mth.floor(camPos.z);

		PoseStack poseStack = context.matrices();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		float alphaFactor = Mth.clamp((windStr - 0.04F) / 0.12F, 0.0F, 1.0F);
		int r = currentMode.getR(isGusting);
		int g = currentMode.getG(isGusting);
		int b = currentMode.getB(isGusting);
		int maxAlpha = currentMode.getA(isGusting);

		float animTime = (gameTime + partialTick) * (windStr * 0.12F);
		float arrowLength = 1.0F + windStr * 1.2F;
		// Sleek, refined aerodynamic width (approx ~3x thinner than bulky original)
		float width = 0.10F + windStr * 0.06F;
		float hw = width * 0.5F;
		float headLen = arrowLength * 0.32F;

		context.commandQueue().submitCustomGeometry(
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
						float distFade = Mth.clamp(1.0F - distRatio * 0.80F, 0.0F, 1.0F);
						int a = (int) (maxAlpha * alphaFactor * distFade);
						if (a < 5) {
							continue;
						}

						int wx = playerX + dx;
						int wz = playerZ + dz;

						// Find water surface near player level
						for (int dy = -6; dy <= 6; dy++) {
							pos.set(wx, playerY + dy, wz);
							FluidState fluid = level.getFluidState(pos);
							if (fluid.is(FluidTags.WATER) && level.getBlockState(pos.above()).isAir()) {
								double surfaceY = pos.getY() + fluid.getHeight(level, pos) + 0.02D;

								// Subtle animated offset along wind flow
								float cellOffset = ((animTime + (wx * 11 + wz * 17) * 0.05F) % 1.0F) * step - (step * 0.5F);
								double startX = wx + 0.5D + dirX * cellOffset - camPos.x;
								double startY = surfaceY - camPos.y;
								double startZ = wz + 0.5D + dirZ * cellOffset - camPos.z;

								double endX = startX + dirX * arrowLength;
								double endZ = startZ + dirZ * arrowLength;
								double neckX = endX - dirX * headLen;
								double neckZ = endZ - dirZ * headLen;

								// 1. Sleek main body ribbon (tapers gracefully from tail to neck)
								drawQuad(
									consumer, pose,
									startX - perpX * (hw * 0.40), startY, startZ - perpZ * (hw * 0.40),
									startX + perpX * (hw * 0.40), startY, startZ + perpZ * (hw * 0.40),
									neckX + perpX * hw, startY, neckZ + perpZ * hw,
									neckX - perpX * hw, startY, neckZ - perpZ * hw,
									r, g, b, a
								);

								// 2. Sleek aerodynamic arrowhead chevron
								drawQuad(
									consumer, pose,
									neckX - perpX * (hw * 2.2), startY, neckZ - perpZ * (hw * 2.2),
									endX, startY, endZ,
									neckX + perpX * (hw * 2.2), startY, neckZ + perpZ * (hw * 2.2),
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
