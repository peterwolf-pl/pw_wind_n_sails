package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Unobtrusive sailing telemetry HUD rendered while riding the sailboat.
 * Displays mainsheet setting, boat speed, and point of sail trim guide.
 */
public final class SailboatHudOverlay implements HudElement {
	public static final SailboatHudOverlay INSTANCE = new SailboatHudOverlay();

	private SailboatHudOverlay() {}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !(client.player.getVehicle() instanceof SailboatEntity boat)) {
			return;
		}

		Font font = client.font;
		int width = extractor.guiWidth();
		int height = extractor.guiHeight();

		float sheet = boat.getMainsheet();
		int sheetPct = Math.round(sheet * 100.0F);

		Vec3 motion = boat.getDeltaMovement();
		double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		// 1 block/tick = 20 blocks/sec ~ 38.8 knots
		double speedKnots = horizontalSpeed * 38.87;

		// Calculate point of sail
		WindVector wind = WindManager.getInstance().getWind(boat.level(), boat.position());
		float appWindX = (float) (wind.x() - motion.x);
		float appWindZ = (float) (wind.z() - motion.z);
		float appWindSpeed = (float) Math.sqrt(appWindX * appWindX + appWindZ * appWindZ);
		float appWindTowardsDeg = (float) (Mth.atan2(-appWindX, appWindZ) * Mth.RAD_TO_DEG);
		float appWindFromDeg = (appWindSpeed > 1e-4F) ? (appWindTowardsDeg + 180.0F) : (wind.directionDeg() + 180.0F);
		float relWindDeg = Math.abs(Mth.wrapDegrees(appWindFromDeg - boat.getYRot()));

		boolean isFlauta = wind.strength() < 0.06F;

		String pointOfSail;
		int colorPointOfSail;
		if (isFlauta && horizontalSpeed < 0.05) {
			pointOfSail = "FLAUTA (Cisza na wodzie)";
			colorPointOfSail = 0xFFAAAAAA; // Gray
		} else if (relWindDeg < WindAndSailsConfig.NO_GO_ZONE_DEG) {
			pointOfSail = "IN IRONS (Head to Wind)";
			colorPointOfSail = 0xFFFF5555; // Red
		} else if (relWindDeg < 65.0F) {
			pointOfSail = "Close-Hauled (Tight Sheet)";
			colorPointOfSail = 0xFF55FFFF; // Cyan
		} else if (relWindDeg < 115.0F) {
			pointOfSail = "Beam Reach (Fast)";
			colorPointOfSail = 0xFF55FF55; // Green
		} else if (relWindDeg < 155.0F) {
			pointOfSail = "Broad Reach";
			colorPointOfSail = 0xFFFFFF55; // Yellow
		} else {
			pointOfSail = "Running Downwind (Ease Sheet)";
			colorPointOfSail = 0xFFFFAA00; // Orange
		}

		// Gauge bar [==========|==========]
		int barTotalUnits = 20;
		int filledUnits = Math.round(sheet * barTotalUnits);
		StringBuilder barStr = new StringBuilder("[");
		for (int i = 0; i < barTotalUnits; i++) {
			barStr.append(i < filledUnits ? "=" : "-");
		}
		barStr.append("]");

		float maxBoom = Mth.lerp(sheet, WindAndSailsConfig.MIN_BOOM_ANGLE_DEG, WindAndSailsConfig.MAX_BOOM_ANGLE_DEG);
		float freeBoom = Math.min(WindAndSailsConfig.MAX_BOOM_ANGLE_DEG, relWindDeg);
		float actualBoom = Math.min(maxBoom, freeBoom);
		float aoa = relWindDeg - actualBoom;

		String trimHint;
		int colorTrim;
		if (boat.isSailFurled()) {
			trimHint = "Żagiel ZWINIĘTY [X / PPM maszt - rozwiń]";
			colorTrim = 0xFF55FFFF; // Cyan
		} else if (isFlauta) {
			trimHint = "Flauta (Brak ciągu wiatru)";
			colorTrim = 0xFFAAAAAA;
		} else if (relWindDeg < WindAndSailsConfig.NO_GO_ZONE_DEG) {
			trimHint = "IN IRONS (Kąt martwy)";
			colorTrim = 0xFFFF5555; // Red
		} else if (Math.abs(boat.getHeelAngle()) > 20.0F) {
			if (boat.isHikeStanding()) {
				trimHint = "PRZECHYŁ! Odpuść szot (S) / Ostrz (A/D)";
			} else if (boat.isHikeSitting()) {
				trimHint = "PRZECHYŁ! Wstań na burcie [Spacja x2] / Odpuść szot (S)";
			} else {
				trimHint = "PRZECHYŁ! Balastuj (Spacja) / Odpuść szot (S)";
			}
			colorTrim = 0xFFFF3333; // Bright Red
		} else if (aoa <= 1.0F && relWindDeg <= 90.0F) {
			trimHint = "Żagiel w łopocie (Wybieraj W)";
			colorTrim = 0xFFAAAAAA; // Gray
		} else if (aoa >= 7.0F && aoa <= 22.0F) {
			trimHint = "Trym optymalny (Maks. ciąg)";
			colorTrim = 0xFF55FF55; // Green
		} else if (aoa > 24.0F) {
			trimHint = "Przebrany (Odpuść S)";
			colorTrim = 0xFFFFAA00; // Orange
		} else {
			trimHint = pointOfSail;
			colorTrim = colorPointOfSail;
		}

		float rudder = boat.getRudderAngle();
		String rudderStr = Math.abs(rudder) < 1.0F ? "Środek" : rudder < 0 ? String.format("Bakburta %.0f°", -rudder) : String.format("Sterburta %.0f°", rudder);
		String sailStatus = boat.isSailFurled() ? "ZWINIĘTY [X]" : String.format("%d%% %s", sheetPct, barStr);
		String hikeStatus;
		if (boat.isHikeStanding()) {
			hikeStatus = "§6Balast: Stoi na burcie§r";
		} else if (boat.isHikeSitting()) {
			hikeStatus = "§aBalast: Siedzi na burcie§r";
		} else {
			hikeStatus = "§7Balast: Środek [Spacja]§r";
		}
		String sheetText = String.format("Szot: %s  |  Ster: %s  |  %s", sailStatus, rudderStr, hikeStatus);
		String speedText = String.format("%.1f kn  |  %s  |  %s", speedKnots, pointOfSail, trimHint);

		int hudX = width / 2;
		int hudY = height - 68;

		// Translucent dark background box
		extractor.fill(hudX - 165, hudY - 4, hudX + 165, hudY + 22, 0x90000000);
		extractor.outline(hudX - 165, hudY - 4, 330, 26, 0x40FFFFFF);

		extractor.centeredText(font, Component.literal(sheetText), hudX, hudY, 0xFFFFFFFF);
		extractor.centeredText(font, Component.literal(speedText), hudX, hudY + 11, colorTrim);
	}
}
