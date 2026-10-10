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

		Component pointOfSail;
		int colorPointOfSail;
		if (isFlauta && horizontalSpeed < 0.05) {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.calm");
			colorPointOfSail = 0xFFAAAAAA; // Gray
		} else if (relWindDeg < WindAndSailsConfig.NO_GO_ZONE_DEG) {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.in_irons");
			colorPointOfSail = 0xFFFF5555; // Red
		} else if (relWindDeg < 65.0F) {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.close_hauled");
			colorPointOfSail = 0xFF55FFFF; // Cyan
		} else if (relWindDeg < 115.0F) {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.beam_reach");
			colorPointOfSail = 0xFF55FF55; // Green
		} else if (relWindDeg < 155.0F) {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.broad_reach");
			colorPointOfSail = 0xFFFFFF55; // Yellow
		} else {
			pointOfSail = Component.translatable("hud.pw_wind_n_sails.point_of_sail.running");
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

		Component trimHint;
		int colorTrim;
		if (boat.isSailFurled()) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.furled");
			colorTrim = 0xFF55FFFF; // Cyan
		} else if (isFlauta) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.calm");
			colorTrim = 0xFFAAAAAA;
		} else if (relWindDeg < WindAndSailsConfig.NO_GO_ZONE_DEG) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.in_irons");
			colorTrim = 0xFFFF5555; // Red
		} else if (Math.abs(boat.getHeelAngle()) > 20.0F) {
			if (boat.isHikeStanding()) {
				trimHint = Component.translatable("hud.pw_wind_n_sails.trim.heel_standing");
			} else if (boat.isHikeSitting()) {
				trimHint = Component.translatable("hud.pw_wind_n_sails.trim.heel_sitting");
			} else {
				trimHint = Component.translatable("hud.pw_wind_n_sails.trim.heel_none");
			}
			colorTrim = 0xFFFF3333; // Bright Red
		} else if (aoa <= 1.0F && relWindDeg <= 90.0F) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.luffing");
			colorTrim = 0xFFAAAAAA; // Gray
		} else if (aoa >= 7.0F && aoa <= 22.0F) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.optimal");
			colorTrim = 0xFF55FF55; // Green
		} else if (aoa > 24.0F) {
			trimHint = Component.translatable("hud.pw_wind_n_sails.trim.overtrimmed");
			colorTrim = 0xFFFFAA00; // Orange
		} else {
			trimHint = pointOfSail;
			colorTrim = colorPointOfSail;
		}

		float rudder = boat.getRudderAngle();
		Component rudderComponent = Math.abs(rudder) < 1.0F
			? Component.translatable("hud.pw_wind_n_sails.rudder.center")
			: rudder < 0
				? Component.translatable("hud.pw_wind_n_sails.rudder.port", String.format("%.0f", -rudder))
				: Component.translatable("hud.pw_wind_n_sails.rudder.starboard", String.format("%.0f", rudder));

		Component sailStatus = boat.isSailFurled()
			? Component.translatable("hud.pw_wind_n_sails.sail.furled")
			: Component.translatable("hud.pw_wind_n_sails.sail.unfurled", sheetPct, barStr.toString());

		Component hikeStatus;
		if (boat.isHikeStanding()) {
			hikeStatus = Component.translatable("hud.pw_wind_n_sails.hike_status.standing");
		} else if (boat.isHikeSitting()) {
			hikeStatus = Component.translatable("hud.pw_wind_n_sails.hike_status.sitting");
		} else {
			hikeStatus = Component.translatable("hud.pw_wind_n_sails.hike_status.center");
		}

		Component sheetLine = Component.translatable("hud.pw_wind_n_sails.hud.sheet_line", sailStatus, rudderComponent, hikeStatus);
		Component speedLine = Component.translatable("hud.pw_wind_n_sails.hud.speed_line", String.format("%.1f", speedKnots), pointOfSail, trimHint);

		int hudX = width / 2;
		int hudY = height - 68;

		// Translucent dark background box
		extractor.fill(hudX - 165, hudY - 4, hudX + 165, hudY + 22, 0x90000000);
		extractor.outline(hudX - 165, hudY - 4, 330, 26, 0x40FFFFFF);

		extractor.centeredText(font, sheetLine, hudX, hudY, 0xFFFFFFFF);
		extractor.centeredText(font, speedLine, hudX, hudY + 11, colorTrim);
	}
}
