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
		Component windComponent;
		if (windStr < 0.06F) {
			windComponent = Component.translatable("hud.pw_wind_n_sails.wind_hud.wind.calm");
		} else {
			windComponent = Component.translatable("hud.pw_wind_n_sails.wind_hud.wind.active",
				String.format("%.0f", windDeg), windCompass, String.format("%.1f", windKnots));
		}

		// Format gusts direction & strength
		Component gustComponent;
		if (hasGust) {
			float gustMultPct = (gust.getStrengthMultiplier() - 1.0F) * 100.0F;
			gustComponent = Component.translatable("hud.pw_wind_n_sails.wind_hud.gust.active",
				String.format("%.0f", windDeg), String.format("%.1f", windKnots), String.format("%.0f", gustMultPct));
		} else if (windStr < 0.06F) {
			gustComponent = Component.translatable("hud.pw_wind_n_sails.wind_hud.gust.none");
		} else {
			gustComponent = Component.translatable("hud.pw_wind_n_sails.wind_hud.gust.calm");
		}

		// Forecast for the near future
		Component forecastComponent = parseForecastComponent(state.getForecast());
		Component forecastLabel = Component.translatable("hud.pw_wind_n_sails.wind_hud.forecast.format", forecastComponent);

		Component fullLine = Component.translatable("hud.pw_wind_n_sails.wind_hud.line",
			windComponent, gustComponent, forecastLabel);

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

	public static Component parseForecastComponent(String forecast) {
		if (forecast == null || forecast.isEmpty() || "Stabilna pogoda".equals(forecast)) {
			return Component.translatable("hud.pw_wind_n_sails.wind_hud.forecast.default");
		}
		if (forecast.startsWith("hud.pw_wind_n_sails.")) {
			String[] parts = forecast.split(";");
			if (parts.length == 1) {
				return Component.translatable(parts[0]);
			}
			Object[] args = new Object[parts.length - 1];
			System.arraycopy(parts, 1, args, 0, parts.length - 1);
			return Component.translatable(parts[0], args);
		}
		return Component.literal(forecast);
	}
}
