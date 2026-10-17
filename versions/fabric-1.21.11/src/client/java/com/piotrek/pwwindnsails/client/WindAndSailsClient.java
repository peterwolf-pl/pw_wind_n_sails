package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.client.WindVisualizerRenderer.VisualizerMode;
import com.piotrek.pwwindnsails.entity.AnchorEntity;
import com.piotrek.pwwindnsails.entity.MooringAim;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.network.MooringClickPayload;
import com.piotrek.pwwindnsails.network.SailboatInputPayload;
import com.piotrek.pwwindnsails.network.WindSyncPayload;
import com.piotrek.pwwindnsails.wind.WindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class WindAndSailsClient implements ClientModInitializer {
	public static final ModelLayerLocation SAILBOAT_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("sailboat"),
		"main"
	);

	public static final ModelLayerLocation SAILBOAT_WATER_PATCH = new ModelLayerLocation(
		WindAndSailsMod.id("sailboat"),
		"water_patch"
	);

	public static final ModelLayerLocation SPORT_SAILBOAT_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("sport_sailboat"),
		"main"
	);

	public static final ModelLayerLocation SPORT_SAILBOAT_WATER_PATCH = new ModelLayerLocation(
		WindAndSailsMod.id("sport_sailboat"),
		"water_patch"
	);

	public static final ModelLayerLocation ANCHOR_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("anchor"),
		"main"
	);

	public static final ModelLayerLocation FLAGPOLE_FLAG_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("flagpole_flag"),
		"main"
	);

	public static final KeyMapping KEY_TOGGLE_WIND = new KeyMapping(
		"key.pw_wind_n_sails.toggle_wind",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_TAB,
		KeyMapping.Category.GAMEPLAY
	);

	public static final KeyMapping KEY_TOGGLE_SAIL = new KeyMapping(
		"key.pw_wind_n_sails.toggle_sail",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_X,
		KeyMapping.Category.GAMEPLAY
	);

	public static final KeyMapping KEY_TOGGLE_WIND_HUD = new KeyMapping(
		"key.pw_wind_n_sails.toggle_wind_hud",
		InputConstants.Type.KEYSYM,
		InputConstants.KEY_H,
		KeyMapping.Category.GAMEPLAY
	);

	private static int tabPressTicks = 0;
	private static int spaceTapTimer = 0;
	private static boolean wasInSailboat = false;
	private static boolean wasUseDown = false;

	@Override
	public void onInitializeClient() {
		// Entity & Model
		EntityRendererRegistry.register(WindAndSailsMod.SAILBOAT_ENTITY, SailboatRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(SAILBOAT_LAYER, SailboatModel::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(SAILBOAT_WATER_PATCH, SailboatModel::createWaterPatch);

		EntityRendererRegistry.register(WindAndSailsMod.SPORT_SAILBOAT_ENTITY, SportSailboatRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(SPORT_SAILBOAT_LAYER, SportSailboatModel::createBodyLayer);
		EntityModelLayerRegistry.registerModelLayer(SPORT_SAILBOAT_WATER_PATCH, SportSailboatModel::createWaterPatch);

		EntityRendererRegistry.register(WindAndSailsMod.ANCHOR_ENTITY, AnchorRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ANCHOR_LAYER, AnchorModel::createBodyLayer);

		// Block Entity Renderer & Model Layer
		BlockEntityRendererRegistry.register(WindAndSailsMod.FLAGPOLE_BLOCK_ENTITY, FlagpoleBlockEntityRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(FLAGPOLE_FLAG_LAYER, FlagModel::createLayer);

		// Keybind
		KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_WIND);
		KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_SAIL);
		KeyBindingHelper.registerKeyBinding(KEY_TOGGLE_WIND_HUD);

		// HUD & Visualizer
		HudElementRegistry.addLast(WindAndSailsMod.id("sailboat_hud"), SailboatHudOverlay.INSTANCE);
		HudElementRegistry.addLast(WindAndSailsMod.id("wind_telemetry_hud"), WindHudOverlay.INSTANCE);
		WorldRenderEvents.AFTER_ENTITIES.register(WindVisualizerRenderer::render);
		WorldRenderEvents.AFTER_ENTITIES.register(CleatHighlightRenderer::render);

		// Client Command: /windarrow, /windarrows, /windsails arrow
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			registerArrowCommands(dispatcher, "windarrow");
			registerArrowCommands(dispatcher, "windarrows");
			registerWindsailsArrowSubcommand(dispatcher);
		});

		// Network Receivers
		ClientPlayNetworking.registerGlobalReceiver(WindSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> WindManager.getInstance().applyClientSync(payload));
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				wasUseDown = false;
				return;
			}
			boolean useDown = client.options.keyUse.isDown();
			if (useDown && !wasUseDown) {
				MooringClickPayload click = SailboatCleatHelper.findClickTarget(client.player, client.level);
				if (click != null) {
					ClientPlayNetworking.send(click);
					while (client.options.keyUse.consumeClick()) {
						// Eat the click so the held item is not used on the highlighted cleat or post.
					}
				}
			}
			wasUseDown = useDown;
		});

		// Client Tick Loop
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean isInSailboat = client.player != null && client.player.getVehicle() instanceof SailboatEntity;
			if (!wasInSailboat && isInSailboat) {
				// Boarding sailboat: automatically enable wind HUD and water arrows!
				WindHudOverlay.setVisible(true);
				WindVisualizerRenderer.setMode(WindVisualizerRenderer.getSelectedStyle());
			} else if (wasInSailboat && !isInSailboat) {
				// Exiting sailboat: automatically close wind HUD and water arrows
				WindHudOverlay.setVisible(false);
				WindVisualizerRenderer.setMode(WindVisualizerRenderer.VisualizerMode.OFF);
			}
			wasInSailboat = isInSailboat;

			if (client.level != null && client.player != null && client.player.tickCount >= 80
				&& client.level.getChunkSource().getChunkNow(client.player.getBlockX() >> 4, client.player.getBlockZ() >> 4) != null) {
				WindManager.getInstance().tickClient(client.level.getGameTime());
				TreeWindAmbienceHandler.tick(client);
			}

			if (spaceTapTimer > 0) {
				spaceTapTimer--;
			}

			// Holding lead or a thrown anchor: prompt for the single highlighted cleat or post
			if (client.player != null && client.level != null && client.player.tickCount % 4 == 0) {
				boolean holdsLead = client.player.getMainHandItem().is(Items.LEAD) || client.player.getOffhandItem().is(Items.LEAD);
				boolean holdsThrownAnchor = AnchorEntity.findHeldBy(client.player) != null;
				if (holdsLead || holdsThrownAnchor) {
					MooringAim.Target aim = MooringAim.find(client.player, client.level, 1.0F);
					if (aim != null && aim.cleat() && client.level.getEntity(aim.boatId()) instanceof SailboatEntity aimedBoat) {
						boolean tied = aimedBoat.isCleatAttached(aim.cleatIndex());
						String action = holdsThrownAnchor && !tied
							? " §f[PPM aby przywiązać kotwicę]"
							: (tied ? " §e[PPM aby odwiązać]" : " §f[PPM aby przywiązać]");
						client.player.displayClientMessage(
							Component.literal("§6[Cumowanie] §a⚓ " + SailboatEntity.getCleatName(aim.cleatIndex()) + action),
							true
						);
					} else if (aim != null && aim.fence() != null && holdsLead && !holdsThrownAnchor) {
						client.player.displayClientMessage(
							Component.literal("§6[Cumowanie] §a⚓ Słupek ogrodzenia §f[PPM aby połączyć cumę]"),
							true
						);
					} else if (holdsThrownAnchor) {
						client.player.displayClientMessage(
							Component.literal("§6[Kotwica]§r Kliknij PPM na zaznaczoną knagę, aby przywiązać kotwicę."),
							true
						);
					}
				}
			}

			// Key H: Toggle single-line Wind HUD (when not operating a plane or paragliding)
			while (KEY_TOGGLE_WIND_HUD.consumeClick()) {
				if (client.player != null && !PlaneCompatHelper.isPlayerInPlaneOrParagliding(client.player)) {
					WindHudOverlay.toggle();
					client.player.displayClientMessage(
						Component.literal(WindHudOverlay.isVisible() ? "§b[Wiatr]§r HUD włączony" : "§7[Wiatr]§r HUD wyłączony"),
						true
					);
				}
			}

			// Short-press TAB toggles selected wind arrows ON / OFF without cycling
			if (KEY_TOGGLE_WIND.isDown()) {
				tabPressTicks++;
			} else {
				if (tabPressTicks > 0 && tabPressTicks < 6) {
					boolean enabled = WindVisualizerRenderer.toggleSelected();
					if (client.player != null) {
						if (enabled) {
							client.player.displayClientMessage(
								Component.literal("§b[Strzałki wiatru TAB]§r WŁĄCZONE §a(" + WindVisualizerRenderer.getSelectedStyle().getDisplayName() + ")§r"),
								true
							);
						} else {
							client.player.displayClientMessage(
								Component.literal("§7[Strzałki wiatru TAB]§r WYŁĄCZONE"),
								true
							);
						}
					}
				}
				tabPressTicks = 0;
			}

			// Sailboat pilot controls & wake particles
			if (client.player != null && client.player.getVehicle() instanceof SailboatEntity boat) {
				boolean forward = client.options.keyUp.isDown();
				boolean back = client.options.keyDown.isDown();
				boolean left = client.options.keyLeft.isDown();
				boolean right = client.options.keyRight.isDown();

				// W pulls sheet in (tightens), S eases sheet out (loosens)
				float sheetInput = forward ? -1.0F : back ? 1.0F : 0.0F;

				// A / D Rudder steering: A = steer port (-1.0), D = steer starboard (+1.0), neither = auto-center (0.0)
				float rudderInput = left ? -1.0F : right ? 1.0F : 0.0F;
				boat.setRudderHeld(rudderInput != 0.0F);
				if (rudderInput == 0.0F) {
					boat.snapVisualRudder();
				}

				// X toggles furling/unfurling the sail
				boolean toggleSail = false;
				while (KEY_TOGGLE_SAIL.consumeClick()) {
					toggleSail = true;
				}

				// Space toggles hiking: single tap = sit on windward gunwale, double tap (quick) = stand on gunwale
				int targetHikeMode = -1;
				while (client.options.keyJump.consumeClick()) {
					int currentMode = boat.getHikeMode();
					if (spaceTapTimer > 0) {
						// Double space tap detected: Stand on the gunwale!
						targetHikeMode = SailboatEntity.HIKE_STAND;
						spaceTapTimer = 0;
						boat.setHikeMode(SailboatEntity.HIKE_STAND);
						client.player.displayClientMessage(
							Component.literal("§6Balastowanie: Stanie na burcie [Maksymalne]§r"),
							true
						);
					} else {
						// Single space tap:
						spaceTapTimer = 8; // ~400ms window for double tap
						if (currentMode == SailboatEntity.HIKE_NONE) {
							targetHikeMode = SailboatEntity.HIKE_SIT;
							boat.setHikeMode(SailboatEntity.HIKE_SIT);
							client.player.displayClientMessage(
								Component.literal("§aBalastowanie: Siedzenie na burcie nawietrznej§r"),
								true
							);
						} else {
							targetHikeMode = SailboatEntity.HIKE_NONE;
							boat.setHikeMode(SailboatEntity.HIKE_NONE);
							client.player.displayClientMessage(
								Component.literal("§7Balastowanie: Środek łódki§r"),
								true
							);
						}
					}
				}

				ClientPlayNetworking.send(new SailboatInputPayload(boat.getId(), rudderInput, sheetInput, toggleSail, targetHikeMode, false));

				// Spawn subtle water wake particles when boat is moving
				Vec3 vel = boat.getDeltaMovement();
				double horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
				if (horizSpeed > 0.03 && client.level != null && client.level.getRandom().nextFloat() < 0.65F) {
					float yawRad = boat.getYRot() * Mth.DEG_TO_RAD;
					double bowX = boat.getX() - Mth.sin(yawRad) * 1.5;
					double bowZ = boat.getZ() + Mth.cos(yawRad) * 1.5;
					double sternX = boat.getX() + Mth.sin(yawRad) * 1.2;
					double sternZ = boat.getZ() - Mth.cos(yawRad) * 1.2;

					client.level.addParticle(ParticleTypes.SPLASH, bowX, boat.getY() + 0.05, bowZ, vel.x * 0.2, 0.04, vel.z * 0.2);
					client.level.addParticle(ParticleTypes.BUBBLE, sternX, boat.getY() + 0.02, sternZ, 0.0, 0.01, 0.0);
				}
			}
		});
	}

	private static void registerArrowCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, String rootName) {
		dispatcher.register(
			ClientCommandManager.literal(rootName)
				.executes(ctx -> {
					VisualizerMode cur = WindVisualizerRenderer.getSelectedStyle();
					boolean enabled = WindVisualizerRenderer.isEnabled();
					ctx.getSource().sendFeedback(Component.literal(
						"§b[Strzałki wiatru]§r Wybrany styl: §e" + cur.getDisplayName() + "§r (" + (enabled ? "§aWŁĄCZONE§r" : "§cWYŁĄCZONE§r") + ")\n" +
						"§7Zmień styl: §f/" + rootName + " <1..5 lub nazwa>§r\n" +
						"§81. seafoam (Jasna piana morska - Biel)\n" +
						"2. cyan    (Jasny błękit - Cyjan)\n" +
						"3. amber   (Bursztynowy wiatr - Złoto)\n" +
						"4. sky     (Subtelny błękit - Łagodny)\n" +
						"5. shadow  (Ciemny cień wody - Grafit)§r\n" +
						"§7Włączanie/wyłączanie w łódce: klawisz §eTAB§r."
					));
					return 1;
				})
				.then(ClientCommandManager.literal("toggle")
					.executes(ctx -> {
						boolean on = WindVisualizerRenderer.toggleSelected();
						ctx.getSource().sendFeedback(Component.literal(on
							? "§b[Strzałki wiatru]§r WŁĄCZONE §a(" + WindVisualizerRenderer.getSelectedStyle().getDisplayName() + ")§r"
							: "§7[Strzałki wiatru]§r WYŁĄCZONE"
						));
						return 1;
					})
				)
				.then(ClientCommandManager.argument("style", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						new String[]{ "1", "2", "3", "4", "5", "seafoam", "cyan", "amber", "sky", "shadow", "toggle" }, builder
					))
					.executes(ctx -> {
						String input = StringArgumentType.getString(ctx, "style");
						if ("toggle".equalsIgnoreCase(input)) {
							boolean on = WindVisualizerRenderer.toggleSelected();
							ctx.getSource().sendFeedback(Component.literal(on
								? "§b[Strzałki wiatru]§r WŁĄCZONE §a(" + WindVisualizerRenderer.getSelectedStyle().getDisplayName() + ")§r"
								: "§7[Strzałki wiatru]§r WYŁĄCZONE"
							));
							return 1;
						}
						VisualizerMode mode = VisualizerMode.fromInput(input);
						if (mode != null) {
							WindVisualizerRenderer.setSelectedStyle(mode);
							ctx.getSource().sendFeedback(Component.literal(
								"§b[Strzałki wiatru]§r Styl ustawiony na: §a" + mode.getDisplayName() + "§r. Włączanie/wyłączanie w łódce: klawisz §eTAB§r."
							));
							return 1;
						} else {
							ctx.getSource().sendError(Component.literal(
								"§cNieznany styl: " + input + ". Dostępne: 1..5, seafoam, cyan, amber, sky, shadow."
							));
							return 0;
						}
					})
				)
		);
	}

	private static void registerWindsailsArrowSubcommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommandManager.literal("windsails")
				.then(ClientCommandManager.literal("arrow")
					.executes(ctx -> {
						VisualizerMode cur = WindVisualizerRenderer.getSelectedStyle();
						boolean enabled = WindVisualizerRenderer.isEnabled();
						ctx.getSource().sendFeedback(Component.literal(
							"§b[Strzałki wiatru]§r Wybrany styl: §e" + cur.getDisplayName() + "§r (" + (enabled ? "§aWŁĄCZONE§r" : "§cWYŁĄCZONE§r") + ")\n" +
							"§7Użyj: §f/windsails arrow <1..5 lub nazwa>§r\n" +
							"§81. seafoam, 2. cyan, 3. amber, 4. sky, 5. shadow§r"
						));
						return 1;
					})
					.then(ClientCommandManager.argument("style", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
							new String[]{ "1", "2", "3", "4", "5", "seafoam", "cyan", "amber", "sky", "shadow", "toggle" }, builder
						))
						.executes(ctx -> {
							String input = StringArgumentType.getString(ctx, "style");
							if ("toggle".equalsIgnoreCase(input)) {
								boolean on = WindVisualizerRenderer.toggleSelected();
								ctx.getSource().sendFeedback(Component.literal(on
									? "§b[Strzałki wiatru]§r WŁĄCZONE §a(" + WindVisualizerRenderer.getSelectedStyle().getDisplayName() + ")§r"
									: "§7[Strzałki wiatru]§r WYŁĄCZONE"
								));
								return 1;
							}
							VisualizerMode mode = VisualizerMode.fromInput(input);
							if (mode != null) {
								WindVisualizerRenderer.setSelectedStyle(mode);
								ctx.getSource().sendFeedback(Component.literal(
									"§b[Strzałki wiatru]§r Styl ustawiony na: §a" + mode.getDisplayName() + "§r. Włączanie/wyłączanie w łódce: klawisz §eTAB§r."
								));
								return 1;
							} else {
								ctx.getSource().sendError(Component.literal(
									"§cNieznany styl: " + input + ". Dostępne: 1..5, seafoam, cyan, amber, sky, shadow."
								));
								return 0;
							}
						})
					)
				)
		);
	}
}
