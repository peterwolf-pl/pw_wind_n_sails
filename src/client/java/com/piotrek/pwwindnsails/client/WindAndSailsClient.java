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
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
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

	public static final ModelLayerLocation BUOY_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("buoy"),
		"main"
	);

	public static final ModelLayerLocation FLAGPOLE_FLAG_LAYER = new ModelLayerLocation(
		WindAndSailsMod.id("flagpole_flag"),
		"main"
	);

	public static final KeyMapping KEY_TOGGLE_WIND = new KeyMapping(
		"key.pw_wind_n_sails.toggle_wind",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_TAB,
		KeyMapping.Category.GAMEPLAY
	);

	public static final KeyMapping KEY_TOGGLE_SAIL = new KeyMapping(
		"key.pw_wind_n_sails.toggle_sail",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_X,
		KeyMapping.Category.GAMEPLAY
	);

	public static final KeyMapping KEY_TOGGLE_WIND_HUD = new KeyMapping(
		"key.pw_wind_n_sails.toggle_wind_hud",
		InputConstants.Type.KEYBOARD,
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
		ModelLayerRegistry.registerModelLayer(SAILBOAT_LAYER, SailboatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SAILBOAT_WATER_PATCH, SailboatModel::createWaterPatch);

		EntityRendererRegistry.register(WindAndSailsMod.SPORT_SAILBOAT_ENTITY, SportSailboatRenderer::new);
		ModelLayerRegistry.registerModelLayer(SPORT_SAILBOAT_LAYER, SportSailboatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SPORT_SAILBOAT_WATER_PATCH, SportSailboatModel::createWaterPatch);

		EntityRendererRegistry.register(WindAndSailsMod.ANCHOR_ENTITY, AnchorRenderer::new);
		ModelLayerRegistry.registerModelLayer(ANCHOR_LAYER, AnchorModel::createBodyLayer);

		EntityRendererRegistry.register(WindAndSailsMod.BUOY_ENTITY, BuoyRenderer::new);
		ModelLayerRegistry.registerModelLayer(BUOY_LAYER, BuoyModel::createBodyLayer);

		// Block Entity Renderer & Model Layer
		BlockEntityRendererRegistry.register(WindAndSailsMod.FLAGPOLE_BLOCK_ENTITY, FlagpoleBlockEntityRenderer::new);
		ModelLayerRegistry.registerModelLayer(FLAGPOLE_FLAG_LAYER, FlagModel::createLayer);

		// Keybind
		KeyMappingHelper.registerKeyMapping(KEY_TOGGLE_WIND);
		KeyMappingHelper.registerKeyMapping(KEY_TOGGLE_SAIL);
		KeyMappingHelper.registerKeyMapping(KEY_TOGGLE_WIND_HUD);

		// HUD & Visualizer
		HudElementRegistry.addLast(WindAndSailsMod.id("sailboat_hud"), SailboatHudOverlay.INSTANCE);
		HudElementRegistry.addLast(WindAndSailsMod.id("wind_telemetry_hud"), WindHudOverlay.INSTANCE);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WindVisualizerRenderer::render);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(CleatHighlightRenderer::render);

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
			if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
				com.piotrek.pwwindnsails.client.compat.xaero.XaeroWorldMapCompat.tryRegister();
			}
			com.piotrek.pwwindnsails.client.compat.xaero.BuoyTracker.tick(client);

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
						Component cleatName = SailboatEntity.getCleatComponent(aim.cleatIndex());
						Component action = holdsThrownAnchor && !tied
							? Component.translatable("hud.pw_wind_n_sails.mooring.prompt_action.tie_anchor")
							: (tied
								? Component.translatable("hud.pw_wind_n_sails.mooring.prompt_action.untie")
								: Component.translatable("hud.pw_wind_n_sails.mooring.prompt_action.tie"));
						client.player.sendOverlayMessage(
							Component.translatable("hud.pw_wind_n_sails.mooring.prompt_cleat", cleatName, action)
						);
					} else if (aim != null && aim.fence() != null && holdsLead && !holdsThrownAnchor) {
						client.player.sendOverlayMessage(
							Component.translatable("hud.pw_wind_n_sails.mooring.prompt_fence")
						);
					} else if (holdsThrownAnchor) {
						client.player.sendOverlayMessage(
							Component.translatable("hud.pw_wind_n_sails.anchor.prompt_cleat")
						);
					}
				}
			}

			// Key H: Toggle single-line Wind HUD (when not operating a plane or paragliding)
			while (KEY_TOGGLE_WIND_HUD.consumeClick()) {
				if (client.player != null && !PlaneCompatHelper.isPlayerInPlaneOrParagliding(client.player)) {
					WindHudOverlay.toggle();
					client.player.sendOverlayMessage(
						Component.translatable(WindHudOverlay.isVisible() ? "hud.pw_wind_n_sails.wind_hud.on" : "hud.pw_wind_n_sails.wind_hud.off")
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
							client.player.sendOverlayMessage(
								Component.translatable("hud.pw_wind_n_sails.visualizer.toggle_on", WindVisualizerRenderer.getSelectedStyle().getComponent())
							);
						} else {
							client.player.sendOverlayMessage(
								Component.translatable("hud.pw_wind_n_sails.visualizer.toggle_off")
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
						client.player.sendOverlayMessage(
							Component.translatable("hud.pw_wind_n_sails.hike.standing")
						);
					} else {
						// Single space tap:
						spaceTapTimer = 8; // ~400ms window for double tap
						if (currentMode == SailboatEntity.HIKE_NONE) {
							targetHikeMode = SailboatEntity.HIKE_SIT;
							boat.setHikeMode(SailboatEntity.HIKE_SIT);
							client.player.sendOverlayMessage(
								Component.translatable("hud.pw_wind_n_sails.hike.sitting")
							);
						} else {
							targetHikeMode = SailboatEntity.HIKE_NONE;
							boat.setHikeMode(SailboatEntity.HIKE_NONE);
							client.player.sendOverlayMessage(
								Component.translatable("hud.pw_wind_n_sails.hike.center")
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
			ClientCommands.literal(rootName)
				.executes(ctx -> {
					VisualizerMode cur = WindVisualizerRenderer.getSelectedStyle();
					boolean enabled = WindVisualizerRenderer.isEnabled();
					ctx.getSource().sendFeedback(Component.translatable(
						"command.pw_wind_n_sails.windarrow.info",
						cur.getComponent(),
						Component.translatable(enabled ? "command.pw_wind_n_sails.enabled" : "command.pw_wind_n_sails.disabled"),
						rootName
					));
					return 1;
				})
				.then(ClientCommands.literal("toggle")
					.executes(ctx -> {
						boolean on = WindVisualizerRenderer.toggleSelected();
						ctx.getSource().sendFeedback(Component.translatable(
							on ? "command.pw_wind_n_sails.windarrow.toggled_on" : "command.pw_wind_n_sails.windarrow.toggled_off",
							WindVisualizerRenderer.getSelectedStyle().getComponent()
						));
						return 1;
					})
				)
				.then(ClientCommands.argument("style", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						new String[]{ "1", "2", "3", "4", "5", "seafoam", "cyan", "amber", "sky", "shadow", "toggle" }, builder
					))
					.executes(ctx -> {
						String input = StringArgumentType.getString(ctx, "style");
						if ("toggle".equalsIgnoreCase(input)) {
							boolean on = WindVisualizerRenderer.toggleSelected();
							ctx.getSource().sendFeedback(Component.translatable(
								on ? "command.pw_wind_n_sails.windarrow.toggled_on" : "command.pw_wind_n_sails.windarrow.toggled_off",
								WindVisualizerRenderer.getSelectedStyle().getComponent()
							));
							return 1;
						}
						VisualizerMode mode = VisualizerMode.fromInput(input);
						if (mode != null) {
							WindVisualizerRenderer.setSelectedStyle(mode);
							ctx.getSource().sendFeedback(Component.translatable(
								"command.pw_wind_n_sails.windarrow.set", mode.getComponent()
							));
							return 1;
						} else {
							ctx.getSource().sendError(Component.translatable(
								"command.pw_wind_n_sails.windarrow.unknown", input
							));
							return 0;
						}
					})
				)
		);
	}

	private static void registerWindsailsArrowSubcommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommands.literal("windsails")
				.then(ClientCommands.literal("arrow")
					.executes(ctx -> {
						VisualizerMode cur = WindVisualizerRenderer.getSelectedStyle();
						boolean enabled = WindVisualizerRenderer.isEnabled();
						ctx.getSource().sendFeedback(Component.translatable(
							"command.pw_wind_n_sails.windsails_arrow.info",
							cur.getComponent(),
							Component.translatable(enabled ? "command.pw_wind_n_sails.enabled" : "command.pw_wind_n_sails.disabled")
						));
						return 1;
					})
					.then(ClientCommands.argument("style", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
							new String[]{ "1", "2", "3", "4", "5", "seafoam", "cyan", "amber", "sky", "shadow", "toggle" }, builder
						))
						.executes(ctx -> {
							String input = StringArgumentType.getString(ctx, "style");
							if ("toggle".equalsIgnoreCase(input)) {
								boolean on = WindVisualizerRenderer.toggleSelected();
								ctx.getSource().sendFeedback(Component.translatable(
									on ? "command.pw_wind_n_sails.windarrow.toggled_on" : "command.pw_wind_n_sails.windarrow.toggled_off",
									WindVisualizerRenderer.getSelectedStyle().getComponent()
								));
								return 1;
							}
							VisualizerMode mode = VisualizerMode.fromInput(input);
							if (mode != null) {
								WindVisualizerRenderer.setSelectedStyle(mode);
								ctx.getSource().sendFeedback(Component.translatable(
									"command.pw_wind_n_sails.windarrow.set", mode.getComponent()
								));
								return 1;
							} else {
								ctx.getSource().sendError(Component.translatable(
									"command.pw_wind_n_sails.windarrow.unknown", input
								));
								return 0;
							}
						})
					)
				)
		);
	}
}
