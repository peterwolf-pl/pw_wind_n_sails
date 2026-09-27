package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.wind.DailyWindProfile;
import com.piotrek.pwwindnsails.wind.WindGust;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindState;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Minimalist, single-line wind telemetry and forecast HUD.
 * Toggled via key H when not piloting a plane or flying a paraglider.
 */
public final class WindHudOverlay implements HudElement {
	public static final WindHudOverlay INSTANCE = new WindHudOverlay();
	private static boolean visible = false;

	private WindHudOverlay() {}

	public static boolean isVisible() {
		return visible;
	}

	public static void toggle() {
		visible = !visible;
	}

	public static void setVisible(boolean val) {
		visible = val;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		if (!visible) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}

		// When paragliding or operating an aircraft, hide wind HUD so flight/lift controls own H
		if (PlaneCompatHelper.isPlayerInPlaneOrParagliding(client.player)) {
			return;
		}

		Font font = client.font;
		int width = extractor.guiWidth();

		long tick = client.level.getGameTime();
		WindState state = WindManager.getInstance().getClientState();
		WindVector wind = state.getWindVector(tick);
		WindGust gust = state.getCurrentGust();
		boolean hasGust = (gust != null && !gust.isExpired(tick));

		float windDeg = wind.directionDeg();
		float windStr = wind.strength();
		double windKnots = windStr * 38.87;
		String windCompass = DailyWindProfile.getCompassDirection(windDeg);

		// Format wind direction & strength
		String windText;
		if (windStr < 0.06F) {
			windText = "§bWiatr:§r §7FLAUTA (0.0 kn)§r";
		} else {
			windText = String.format("§bWiatr:§r §e%.0f° %s§r §a(%.1f kn)§r", windDeg, windCompass, windKnots);
		}

		// Format gusts direction & strength
		String gustText;
		if (hasGust) {
			float gustMultPct = (gust.getStrengthMultiplier() - 1.0F) * 100.0F;
			gustText = String.format("§6Szkwał:§r §e%.0f°§r §c%.1f kn (+%.0f%%)§r", windDeg, windKnots, gustMultPct);
		} else if (windStr < 0.06F) {
			gustText = "§6Szkwał:§r §7Brak§r";
		} else {
			gustText = "§6Szkwał:§r §7Spokojnie§r";
		}

		// Forecast for the near future
		String forecast = state.getForecast();
		if (forecast == null || forecast.isEmpty()) {
			forecast = "Stabilna pogoda";
		}
		String forecastText = "§7Prognoza:§r §f" + forecast + "§r";

		Component fullLine = Component.literal(
			String.format("%s  §8|§r  %s  §8|§r  %s", windText, gustText, forecastText)
		);

		int textWidth = font.width(fullLine);
		int boxWidth = textWidth + 16;
		int boxHeight = 16;
		int boxX = (width - boxWidth) / 2;
		int boxY = 8;

		// Translucent dark background pill
		extractor.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000);
		extractor.outline(boxX, boxY, boxWidth, boxHeight, 0x35FFFFFF);

		extractor.centeredText(font, fullLine, width / 2, boxY + 4, 0xFFFFFFFF);
	}
}
