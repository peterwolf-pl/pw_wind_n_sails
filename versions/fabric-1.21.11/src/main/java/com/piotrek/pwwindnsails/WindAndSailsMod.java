package com.piotrek.pwwindnsails;

import com.piotrek.pwwindnsails.block.FlagpoleBlock;
import com.piotrek.pwwindnsails.block.entity.FlagpoleBlockEntity;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.entity.SportSailboatEntity;
import com.piotrek.pwwindnsails.item.FlagpoleItem;
import com.piotrek.pwwindnsails.item.SailboatItem;
import com.piotrek.pwwindnsails.item.SportSailboatItem;
import com.piotrek.pwwindnsails.network.MooringClickPayload;
import com.piotrek.pwwindnsails.network.SailboatInputPayload;
import com.piotrek.pwwindnsails.network.WindSyncPayload;
import com.piotrek.pwwindnsails.wind.WindManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
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

	public static final ResourceKey<EntityType<?>> SPORT_SAILBOAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("sport_sailboat"));
	public static final EntityType<SportSailboatEntity> SPORT_SAILBOAT_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SPORT_SAILBOAT_KEY,
		EntityType.Builder.of(SportSailboatEntity::new, MobCategory.MISC)
			.sized(1.5F, 0.8F)
			.eyeHeight(0.6F)
			.clientTrackingRange(10)
			.build(SPORT_SAILBOAT_KEY)
	);

	public static final ResourceKey<EntityType<?>> ANCHOR_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("anchor"));
	public static final EntityType<com.piotrek.pwwindnsails.entity.AnchorEntity> ANCHOR_ENTITY = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ANCHOR_KEY,
		EntityType.Builder.of(com.piotrek.pwwindnsails.entity.AnchorEntity::new, MobCategory.MISC)
			.sized(0.6F, 0.8F)
			.clientTrackingRange(10)
			.build(ANCHOR_KEY)
	);

	// Item Registration
	public static final ResourceKey<Item> SAILBOAT_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("sailboat"));
	public static final SailboatItem SAILBOAT_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		SAILBOAT_ITEM_KEY,
		new SailboatItem(new Item.Properties().setId(SAILBOAT_ITEM_KEY).stacksTo(1))
	);

	public static final ResourceKey<Item> SPORT_SAILBOAT_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("sport_sailboat"));
	public static final SportSailboatItem SPORT_SAILBOAT_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		SPORT_SAILBOAT_ITEM_KEY,
		new SportSailboatItem(new Item.Properties().setId(SPORT_SAILBOAT_ITEM_KEY).stacksTo(1))
	);

	public static final ResourceKey<Item> ANCHOR_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("anchor"));
	public static final com.piotrek.pwwindnsails.item.AnchorItem ANCHOR_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		ANCHOR_ITEM_KEY,
		new com.piotrek.pwwindnsails.item.AnchorItem(new Item.Properties().setId(ANCHOR_ITEM_KEY).stacksTo(16))
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

	// Pending manual fence mooring selection: Player clicked a fence post first, waiting to click a cleat on the boat
	private static final java.util.Map<java.util.UUID, BlockPos> PENDING_FENCE_MOORINGS = new java.util.concurrent.ConcurrentHashMap<>();

	public static void setPendingFenceMooring(java.util.UUID playerId, BlockPos pos) {
		PENDING_FENCE_MOORINGS.put(playerId, pos);
	}

	public static BlockPos getPendingFenceMooring(java.util.UUID playerId) {
		return PENDING_FENCE_MOORINGS.get(playerId);
	}

	public static BlockPos clearPendingFenceMooring(java.util.UUID playerId) {
		return PENDING_FENCE_MOORINGS.remove(playerId);
	}

	// Creative Tab
	public static final CreativeModeTab TAB = FabricItemGroup.builder()
		.title(Component.translatable("itemGroup.pw_wind_n_sails.group"))
		.icon(SAILBOAT_ITEM::getDefaultInstance)
		.displayItems((parameters, output) -> {
			output.accept(SAILBOAT_ITEM);
			output.accept(SPORT_SAILBOAT_ITEM);
			output.accept(ANCHOR_ITEM);
			output.accept(FLAGPOLE_ITEM);
		})
		.build();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Peterwolf's Wind & Sails");

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("group"), TAB);

		// Network Payload Registrations
		PayloadTypeRegistry.playC2S().register(SailboatInputPayload.TYPE, SailboatInputPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MooringClickPayload.TYPE, MooringClickPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WindSyncPayload.TYPE, WindSyncPayload.CODEC);

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

		ServerPlayNetworking.registerGlobalReceiver(MooringClickPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> SailboatEntity.handleMooringClick(
				context.player(), payload.cleat(), payload.boatId(), payload.cleatIndex(), payload.fence()));
		});

		// Commands
		com.piotrek.pwwindnsails.command.WindAndSailsCommands.register();

		// World Tick & Player Sync
		// Never scan every entity in the level here. That loop ran during world load,
		// starved chunk sending, and hung the game on exit.
		ServerTickEvents.END_WORLD_TICK.register(level -> WindManager.getInstance().tickServer(level));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			WindManager.getInstance().syncToPlayer(handler.getPlayer());
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			// Aimed cleat wire or fence post owns the click. Neighbor blocks do not.
			return SailboatEntity.handleAimedMooringClick(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			return SailboatEntity.handleAimedMooringClick(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (!(entity instanceof SailboatEntity) && !(entity instanceof LeashFenceKnotEntity)) {
				return InteractionResult.PASS;
			}
			// Boat pick-radius and a neighboring knot must not steal the highlighted post or cleat.
			return SailboatEntity.handleAimedMooringClick(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
	}
}
