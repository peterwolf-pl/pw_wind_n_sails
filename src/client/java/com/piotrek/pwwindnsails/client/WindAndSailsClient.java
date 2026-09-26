package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.network.SailboatInputPayload;
import com.piotrek.pwwindnsails.network.WindSyncPayload;
import com.piotrek.pwwindnsails.wind.WindManager;
import net.fabricmc.api.ClientModInitializer;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

	private static int tabPressTicks = 0;

	@Override
	public void onInitializeClient() {
		// Entity & Model
		EntityRendererRegistry.register(WindAndSailsMod.SAILBOAT_ENTITY, SailboatRenderer::new);
		ModelLayerRegistry.registerModelLayer(SAILBOAT_LAYER, SailboatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(SAILBOAT_WATER_PATCH, SailboatModel::createWaterPatch);

		// Block Entity Renderer & Model Layer
		BlockEntityRendererRegistry.register(WindAndSailsMod.FLAGPOLE_BLOCK_ENTITY, FlagpoleBlockEntityRenderer::new);
		ModelLayerRegistry.registerModelLayer(FLAGPOLE_FLAG_LAYER, FlagModel::createLayer);

		// Keybind
		KeyMappingHelper.registerKeyMapping(KEY_TOGGLE_WIND);
		KeyMappingHelper.registerKeyMapping(KEY_TOGGLE_SAIL);

		// HUD & Visualizer
		HudElementRegistry.addLast(WindAndSailsMod.id("sailboat_hud"), SailboatHudOverlay.INSTANCE);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WindVisualizerRenderer::render);

		// Network Receivers
		ClientPlayNetworking.registerGlobalReceiver(WindSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> WindManager.getInstance().applyClientSync(payload));
		});

		// Client Tick Loop
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null) {
				WindManager.getInstance().tickClient(client.level.getGameTime());
			}

			// Short-press TAB toggles wind visualization hints while preserving player list on hold
			if (KEY_TOGGLE_WIND.isDown()) {
				tabPressTicks++;
			} else {
				if (tabPressTicks > 0 && tabPressTicks < 6) {
					WindVisualizerRenderer.toggle();
					if (client.player != null) {
						client.player.sendOverlayMessage(
							Component.translatable(
								WindVisualizerRenderer.isEnabled()
									? "message.pw_wind_n_sails.wind_hints_on"
									: "message.pw_wind_n_sails.wind_hints_off"
							)
						);
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

				ClientPlayNetworking.send(new SailboatInputPayload(boat.getId(), rudderInput, sheetInput, toggleSail));

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
}
