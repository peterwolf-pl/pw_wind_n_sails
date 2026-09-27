package com.piotrek.pwwindnsails;

import com.piotrek.pwwindnsails.block.FlagpoleBlock;
import com.piotrek.pwwindnsails.block.entity.FlagpoleBlockEntity;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.item.FlagpoleItem;
import com.piotrek.pwwindnsails.item.SailboatItem;
import com.piotrek.pwwindnsails.network.SailboatInputPayload;
import com.piotrek.pwwindnsails.network.WindSyncPayload;
import com.piotrek.pwwindnsails.wind.WindManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WindAndSailsMod implements ModInitializer {
	public static final String MOD_ID = "pw_wind_n_sails";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	// Entity Registration
	public static final ResourceKey<EntityType<?>> SAILBOAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("sailboat"));
	public static final EntityType<SailboatEntity> SAILBOAT_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SAILBOAT_KEY,
		EntityType.Builder.of(SailboatEntity::new, MobCategory.MISC)
			.sized(1.5F, 0.8F)
			.eyeHeight(0.6F)
			.clientTrackingRange(10)
			.build(SAILBOAT_KEY)
	);

	// Item Registration
	public static final ResourceKey<Item> SAILBOAT_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("sailboat"));
	public static final SailboatItem SAILBOAT_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		SAILBOAT_ITEM_KEY,
		new SailboatItem(new Item.Properties().setId(SAILBOAT_ITEM_KEY).stacksTo(1))
	);

	// Block & BlockEntity Registration
	public static final ResourceKey<Block> FLAGPOLE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("flagpole"));
	public static final FlagpoleBlock FLAGPOLE_BLOCK = Registry.register(
		BuiltInRegistries.BLOCK,
		FLAGPOLE_BLOCK_KEY,
		new FlagpoleBlock(BlockBehaviour.Properties.of()
			.setId(FLAGPOLE_BLOCK_KEY)
			.mapColor(MapColor.WOOD)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.noOcclusion())
	);

	public static final ResourceKey<Item> FLAGPOLE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("flagpole"));
	public static final FlagpoleItem FLAGPOLE_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		FLAGPOLE_ITEM_KEY,
		new FlagpoleItem(FLAGPOLE_BLOCK, new Item.Properties().setId(FLAGPOLE_ITEM_KEY).stacksTo(16))
	);

	public static final ResourceKey<BlockEntityType<?>> FLAGPOLE_BE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("flagpole"));
	public static final BlockEntityType<FlagpoleBlockEntity> FLAGPOLE_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		FLAGPOLE_BE_KEY,
		FabricBlockEntityTypeBuilder.create(FlagpoleBlockEntity::new, FLAGPOLE_BLOCK).build()
	);

	// Creative Tab
	public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
		.title(Component.translatable("itemGroup.pw_wind_n_sails.group"))
		.icon(SAILBOAT_ITEM::getDefaultInstance)
		.displayItems((parameters, output) -> {
			output.accept(SAILBOAT_ITEM);
			output.accept(FLAGPOLE_ITEM);
		})
		.build();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Peterwolf's Wind & Sails");

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("group"), TAB);

		// Network Payload Registrations
		PayloadTypeRegistry.serverboundPlay().register(SailboatInputPayload.TYPE, SailboatInputPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WindSyncPayload.TYPE, WindSyncPayload.CODEC);

		// Handle Client Boat Controls on Server
		ServerPlayNetworking.registerGlobalReceiver(SailboatInputPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				if (context.player().getVehicle() instanceof SailboatEntity boat) {
					boat.setControlInput(payload.rudderInput(), payload.sheetInput());
					if (payload.toggleSail()) {
						boat.toggleSailFurled();
					}
					if (payload.targetHikeMode() >= 0) {
						boat.setHikeMode(payload.targetHikeMode());
					}
				}
			});
		});

		// Commands
		com.piotrek.pwwindnsails.command.WindAndSailsCommands.register();

		// World Tick & Player Sync
		ServerTickEvents.END_LEVEL_TICK.register(level -> WindManager.getInstance().tickServer(level));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			WindManager.getInstance().syncToPlayer(handler.getPlayer());
		});
	}
}
