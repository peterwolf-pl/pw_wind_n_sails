package com.piotrek.pwwindnsails.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
			.then(Commands.literal("status")
				.executes(ctx -> {
					ctx.getSource().sendSuccess(
						() -> Component.literal(String.format(
							"§b[Wind & Sails Status]§r\n" +
							"§7Rudder Sensitivity:§r §e%.2f§r\n" +
							"§7Rudder Turn Rate:§r §e%.1f°/tick§r\n" +
							"§7Rudder Auto-Center Rate:§r §e%.1f°/tick§r\n" +
							"§7Max Rudder Angle:§r §e%.0f°§r",
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
}
