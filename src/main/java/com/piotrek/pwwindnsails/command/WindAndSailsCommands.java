package com.piotrek.pwwindnsails.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.DailyWindProfile;
import com.piotrek.pwwindnsails.wind.WindGust;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindState;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class WindAndSailsCommands {
	private WindAndSailsCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher, "windsails");
			registerCommands(dispatcher, "sailing");
		});
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String rootName) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(rootName)
			.then(Commands.literal("rudder")
				.then(Commands.literal("sensitivity")
					.executes(ctx -> {
						ctx.getSource().sendSuccess(
							() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder sensitivity: §e%.2f§r (default: 1.0)", WindAndSailsConfig.rudderSensitivity)),
							false
						);
						return 1;
					})
					.then(Commands.argument("value", FloatArgumentType.floatArg(0.05F, 10.0F))
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> {
							float val = FloatArgumentType.getFloat(ctx, "value");
							WindAndSailsConfig.rudderSensitivity = val;
							ctx.getSource().sendSuccess(
								() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder sensitivity set to: §a%.2f§r", val)),
								true
							);
							return 1;
						})
					)
				)
				.then(Commands.literal("autocenter")
					.executes(ctx -> {
						ctx.getSource().sendSuccess(
							() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder auto-center return rate: §e%.1f°/tick§r", WindAndSailsConfig.rudderAutoCenterRate)),
							false
						);
						return 1;
					})
					.then(Commands.argument("rate", FloatArgumentType.floatArg(0.1F, 15.0F))
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> {
							float val = FloatArgumentType.getFloat(ctx, "rate");
							WindAndSailsConfig.rudderAutoCenterRate = val;
							ctx.getSource().sendSuccess(
								() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder auto-center return rate set to: §a%.1f°/tick§r", val)),
								true
							);
							return 1;
						})
					)
				)
				.then(Commands.literal("turnrate")
					.executes(ctx -> {
						ctx.getSource().sendSuccess(
							() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder A/D steer rate: §e%.1f°/tick§r", WindAndSailsConfig.rudderRatePerTick)),
							false
						);
						return 1;
					})
					.then(Commands.argument("rate", FloatArgumentType.floatArg(0.1F, 15.0F))
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> {
							float val = FloatArgumentType.getFloat(ctx, "rate");
							WindAndSailsConfig.rudderRatePerTick = val;
							ctx.getSource().sendSuccess(
								() -> Component.literal(String.format("§b[Wind & Sails]§r Rudder A/D steer rate set to: §a%.1f°/tick§r", val)),
								true
							);
							return 1;
						})
					)
				)
			)
			.then(Commands.literal("wind")
				.executes(ctx -> {
					ServerLevel level = ctx.getSource().getLevel();
					long tick = level.getGameTime();
					long dayTime = level.getDefaultClockTime();
					long dayIndex = Math.floorDiv(dayTime, 24000L);
					int timeOfDay = (int) Math.floorMod(dayTime, 24000L);
					WindState state = com.piotrek.pwwindnsails.wind.WindManager.getInstance().getWindState(level);
					com.piotrek.pwwindnsails.wind.WindVector wind = state.getWindVector(tick);
					com.piotrek.pwwindnsails.wind.DailyWindProfile profile = state.getCurrentDayProfile();
					com.piotrek.pwwindnsails.wind.WindGust gust = state.getCurrentGust();
					boolean hasGust = (gust != null && !gust.isExpired(tick));

					String dirStr = getCompassDirection(wind.directionDeg());
					String dayTypeStr = profile != null ? profile.dayType().getDisplayName() : "Umiarkowany wiatr";
					boolean isNight = (timeOfDay >= 13000 && timeOfDay <= 23000);
					String timePhaseStr = isNight ? (profile != null && profile.windyNight() ? "Noc (wietrzna, stabilna)" : "Noc (spokojna, równa)") : "Dzień (aktywny)";
					double speedKnots = wind.strength() * 38.87;

					String msg = String.format(
						"§b[Wiatr]§r Dzień #%d (%s)\n" +
						"§7Kierunek:§r §e%.1f°§r (%s), §7Siła:§r §a%.2f§r (~§a%.1f kn§r)\n" +
						"§7Warunki:§r §e%s§r | §7Pora:§r §f%s§r\n" +
						"§7Szkwał:§r %s",
						dayIndex + 1,
						timePhaseStr,
						wind.directionDeg(), dirStr, wind.strength(), speedKnots,
						dayTypeStr,
						isNight ? "Noc" : "Dzień",
						hasGust ? String.format("§cAktywny (x%.2f, odch. %.1f°)§r", gust.getStrengthMultiplier(), gust.getDirectionShiftDeg()) : "§7Brak§r"
					);
					ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
					return 1;
				})
				.then(Commands.literal("set")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("strength", FloatArgumentType.floatArg(0.0F, 2.0F))
						.executes(ctx -> {
							float str = FloatArgumentType.getFloat(ctx, "strength");
							ServerLevel level = ctx.getSource().getLevel();
							WindState state = com.piotrek.pwwindnsails.wind.WindManager.getInstance().getWindState(level);
							state.setBase(state.getBaseDirectionDeg(), str);
							ctx.getSource().sendSuccess(
								() -> Component.literal(String.format("§b[Wiatr]§r Prędkość wiatru ustawiona na: §a%.2f§r", str)),
								true
							);
							return 1;
						})
						.then(Commands.argument("direction", FloatArgumentType.floatArg(0.0F, 360.0F))
							.executes(ctx -> {
								float str = FloatArgumentType.getFloat(ctx, "strength");
								float dir = FloatArgumentType.getFloat(ctx, "direction");
								ServerLevel level = ctx.getSource().getLevel();
								WindState state = com.piotrek.pwwindnsails.wind.WindManager.getInstance().getWindState(level);
								state.setBase(dir, str);
								ctx.getSource().sendSuccess(
									() -> Component.literal(String.format("§b[Wiatr]§r Kierunek wiatru ustawiony na §e%.1f°§r, prędkość na §a%.2f§r", dir, str)),
									true
								);
								return 1;
							})
						)
					)
				)
			)
			.then(Commands.literal("status")
				.executes(ctx -> {
					ServerLevel level = ctx.getSource().getLevel();
					long tick = level.getGameTime();
					WindState state = com.piotrek.pwwindnsails.wind.WindManager.getInstance().getWindState(level);
					com.piotrek.pwwindnsails.wind.WindVector wind = state.getWindVector(tick);
					DailyWindProfile profile = state.getCurrentDayProfile();
					String dayTypeStr = profile != null ? profile.dayType().getDisplayName() : "Standard";

					ctx.getSource().sendSuccess(
						() -> Component.literal(String.format(
							"§b[Wind & Sails Status]§r\n" +
							"§7Wiatr:§r §e%.1f°§r (%s), Siła: §a%.2f§r (~%.1f kn) [%s]\n" +
							"§7Rudder Sensitivity:§r §e%.2f§r\n" +
							"§7Rudder Turn Rate:§r §e%.1f°/tick§r\n" +
							"§7Rudder Auto-Center Rate:§r §e%.1f°/tick§r\n" +
							"§7Max Rudder Angle:§r §e%.0f°§r",
							wind.directionDeg(),
							getCompassDirection(wind.directionDeg()),
							wind.strength(),
							wind.strength() * 38.87,
							dayTypeStr,
							WindAndSailsConfig.rudderSensitivity,
							WindAndSailsConfig.rudderRatePerTick,
							WindAndSailsConfig.rudderAutoCenterRate,
							WindAndSailsConfig.maxRudderAngleDeg
						)),
						false
					);
					return 1;
				})
			);

		dispatcher.register(root);
	}

	private static String getCompassDirection(float deg) {
		float normalized = (deg % 360.0F + 360.0F) % 360.0F;
		if (normalized >= 337.5F || normalized < 22.5F) return "S (+Z)";
		if (normalized < 67.5F) return "SW";
		if (normalized < 112.5F) return "W (-X)";
		if (normalized < 157.5F) return "NW";
		if (normalized < 202.5F) return "N (-Z)";
		if (normalized < 247.5F) return "NE";
		if (normalized < 292.5F) return "E (+X)";
		return "SE";
	}
}
