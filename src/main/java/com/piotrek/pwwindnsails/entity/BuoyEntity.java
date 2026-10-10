package com.piotrek.pwwindnsails.entity;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.physics.MooringConstraint;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BuoyEntity extends Entity implements Leashable {
	public static final int TYPE_RED_SPHERICAL = 0;
	public static final int TYPE_BLUE_CYLINDRICAL = 1;

	private static final EntityDataAccessor<Integer> BUOY_TYPE = SynchedEntityData.defineId(BuoyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANCHOR_ENTITY_ID = SynchedEntityData.defineId(BuoyEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<BlockPos>> ANCHOR_BED_POS = SynchedEntityData.defineId(BuoyEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Float> SLACK_RADIUS = SynchedEntityData.defineId(BuoyEntity.class, EntityDataSerializers.FLOAT);

	@Nullable
	private Leashable.LeashData leashData;
	@Nullable
	private UUID anchorUuid;
	@Nullable
	private AnchorEntity cachedAnchorEntity;

	public BuoyEntity(EntityType<? extends BuoyEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected InterpolationHandler createInterpolationHandler() {
		return LinearInterpolationHandler.create(this, 3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(BUOY_TYPE, TYPE_RED_SPHERICAL);
		builder.define(ANCHOR_ENTITY_ID, -1);
		builder.define(ANCHOR_BED_POS, Optional.empty());
		builder.define(SLACK_RADIUS, 1.8F);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setBuoyType(input.getIntOr("BuoyType", TYPE_RED_SPHERICAL));
		this.readLeashData(input);

		String uuidStr = input.getStringOr("AnchorUUID", "");
		if (!uuidStr.isEmpty()) {
			try {
				this.anchorUuid = UUID.fromString(uuidStr);
			} catch (IllegalArgumentException ignored) {}
		}

		if (input.getIntOr("HasAnchorPos", 0) == 1) {
			int x = input.getIntOr("AnchorX", 0);
			int y = input.getIntOr("AnchorY", 0);
			int z = input.getIntOr("AnchorZ", 0);
			this.entityData.set(ANCHOR_BED_POS, Optional.of(new BlockPos(x, y, z)));
		}
		this.entityData.set(SLACK_RADIUS, input.getFloatOr("SlackRadius", 1.8F));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("BuoyType", this.getBuoyType());
		this.writeLeashData(output, this.leashData);

		if (this.anchorUuid != null) {
			output.putString("AnchorUUID", this.anchorUuid.toString());
		}
		Optional<BlockPos> bedPos = this.entityData.get(ANCHOR_BED_POS);
		if (bedPos.isPresent()) {
			output.putInt("HasAnchorPos", 1);
			output.putInt("AnchorX", bedPos.get().getX());
			output.putInt("AnchorY", bedPos.get().getY());
			output.putInt("AnchorZ", bedPos.get().getZ());
		} else {
			output.putInt("HasAnchorPos", 0);
		}
		output.putFloat("SlackRadius", this.getSlackRadius());
	}

	public int getBuoyType() {
		return this.entityData.get(BUOY_TYPE);
	}

	public void setBuoyType(int type) {
		this.entityData.set(BUOY_TYPE, type);
		this.refreshDimensions();
	}

	@Nullable
	public UUID getAnchorUUID() {
		return this.anchorUuid;
	}

	public void setAnchorUUID(@Nullable UUID uuid) {
		this.anchorUuid = uuid;
	}

	public int getAnchorEntityId() {
		return this.entityData.get(ANCHOR_ENTITY_ID);
	}

	public void setAnchorEntityId(int id) {
		this.entityData.set(ANCHOR_ENTITY_ID, id);
	}

	public Optional<BlockPos> getAnchorBedPos() {
		return this.entityData.get(ANCHOR_BED_POS);
	}

	public void setAnchorBedPos(@Nullable BlockPos pos) {
		this.entityData.set(ANCHOR_BED_POS, Optional.ofNullable(pos));
	}

	public float getSlackRadius() {
		return this.entityData.get(SLACK_RADIUS);
	}

	public void setSlackRadius(float radius) {
		this.entityData.set(SLACK_RADIUS, radius);
	}

	public void setAnchor(AnchorEntity anchor) {
		this.cachedAnchorEntity = anchor;
		this.setAnchorUUID(anchor.getUUID());
		this.setAnchorEntityId(anchor.getId());
		this.setAnchorBedPos(anchor.blockPosition());
	}

	@Nullable
	public AnchorEntity getAnchorEntity() {
		if (this.cachedAnchorEntity != null && !this.cachedAnchorEntity.isRemoved()) {
			return this.cachedAnchorEntity;
		}
		if (this.anchorUuid != null && this.level() instanceof ServerLevel serverLevel) {
			Entity e = serverLevel.getEntity(this.anchorUuid);
			if (e instanceof AnchorEntity anchor) {
				this.cachedAnchorEntity = anchor;
				return anchor;
			}
		}
		int entityId = this.getAnchorEntityId();
		if (entityId != -1) {
			Entity e = this.level().getEntity(entityId);
			if (e instanceof AnchorEntity anchor) {
				this.cachedAnchorEntity = anchor;
				return anchor;
			}
		}
		return null;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return this.getBuoyType() == TYPE_BLUE_CYLINDRICAL
			? EntityDimensions.scalable(0.7F, 1.0F)
			: EntityDimensions.scalable(0.75F, 0.75F);
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (this.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		// 1. Boat Cleat Mooring
		SailboatEntity.HeldCleat held = SailboatEntity.findCleatHeldBy(player);
		if (held != null) {
			held.boat().attachCleat(held.index(), this);
			SailboatEntity.shrinkLead(player);
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable("hud.pw_wind_n_sails.mooring.cleat_tied_to_buoy",
				SailboatEntity.getCleatComponent(held.index())));
			return InteractionResult.SUCCESS;
		}

		// 2. Untie any Sailboat cleats currently moored here (if clicking without a lead)
		if (!player.getItemInHand(hand).is(Items.LEAD)) {
			AABB area = this.getBoundingBox().inflate(16.0);
			boolean detachedBoat = false;
			for (SailboatEntity boat : this.level().getEntitiesOfClass(SailboatEntity.class, area)) {
				for (int i = 0; i < SailboatEntity.CLEAT_COUNT; i++) {
					if (boat.getCleatHolderEntity(i) == this) {
						boat.detachCleat(i, true);
						player.sendOverlayMessage(Component.translatable("hud.pw_wind_n_sails.mooring.buoy_untied",
							SailboatEntity.getCleatComponent(i)));
						detachedBoat = true;
						break;
					}
				}
				if (detachedBoat) break;
			}
			if (detachedBoat) {
				return InteractionResult.SUCCESS;
			}
		}

		// 3. Tie leashed entities/mobs/boats from player to this buoy
		List<Leashable> leashedToPlayer = Leashable.leashableLeashedTo(player);
		if (!leashedToPlayer.isEmpty()) {
			for (Leashable l : leashedToPlayer) {
				l.setLeashedTo(this, true);
			}
			SailboatEntity.shrinkLead(player);
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable("hud.pw_wind_n_sails.mooring.tied_to_buoy"));
			return InteractionResult.SUCCESS;
		}

		// 4. Untie any entities/mobs leashed to this buoy
		List<Leashable> leashedToBuoy = Leashable.leashableLeashedTo(this);
		if (!leashedToBuoy.isEmpty() && !player.getItemInHand(hand).is(Items.LEAD)) {
			for (Leashable l : leashedToBuoy) {
				l.dropLeash();
			}
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.LEAD_UNTIED, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable("hud.pw_wind_n_sails.mooring.buoy_all_untied"));
			return InteractionResult.SUCCESS;
		}

		// 5. Tie lead from player to buoy (to tow or leash buoy)
		if (player.getItemInHand(hand).is(Items.LEAD)) {
			this.setLeashedTo(player, true);
			if (!player.getAbilities().instabuild) {
				player.getItemInHand(hand).shrink(1);
			}
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable("hud.pw_wind_n_sails.mooring.buoy_leashed"));
			return InteractionResult.SUCCESS;
		}

		// 6. Name Tag (Signal Buoy naming)
		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.is(Items.NAME_TAG) && heldItem.has(DataComponents.CUSTOM_NAME)) {
			this.setCustomName(heldItem.get(DataComponents.CUSTOM_NAME));
			this.setCustomNameVisible(true);
			if (!player.getAbilities().instabuild) {
				heldItem.shrink(1);
			}
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
			return InteractionResult.SUCCESS;
		}

		// 7. Pick up buoy (Sneak right click or empty hand when nothing is tied)
		if (player.isSecondaryUseActive() || heldItem.isEmpty()) {
			ItemStack drop = new ItemStack(this.getBuoyType() == TYPE_BLUE_CYLINDRICAL
				? WindAndSailsMod.CYLINDRICAL_BUOY_ITEM
				: WindAndSailsMod.BUOY_ITEM);
			if (this.hasCustomName()) {
				drop.set(DataComponents.CUSTOM_NAME, this.getCustomName());
			}
			if (!player.addItem(drop)) {
				this.spawnAtLocation((ServerLevel) this.level(), drop);
			}
			AnchorEntity anchor = this.getAnchorEntity();
			if (anchor != null) {
				anchor.dropLeash();
				anchor.discard();
			}
			this.dropLeash();
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
			this.discard();
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		if (this.isInvulnerableToBase(damageSource)) {
			return false;
		}
		ItemStack drop = new ItemStack(this.getBuoyType() == TYPE_BLUE_CYLINDRICAL
			? WindAndSailsMod.CYLINDRICAL_BUOY_ITEM
			: WindAndSailsMod.BUOY_ITEM);
		if (this.hasCustomName()) {
			drop.set(DataComponents.CUSTOM_NAME, this.getCustomName());
		}
		this.spawnAtLocation(level, drop);
		AnchorEntity anchor = this.getAnchorEntity();
		if (anchor != null) {
			anchor.dropLeash();
			anchor.discard();
		}
		this.dropLeash();
		this.discard();
		return true;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide()) {
			this.getInterpolation().interpolate();
			return;
		}

		// Check flotation
		AABB bb = this.getBoundingBox();
		double waterSurfaceY = Double.NEGATIVE_INFINITY;
		boolean inWater = false;
		int minX = Mth.floor(bb.minX);
		int maxX = Mth.ceil(bb.maxX);
		int minY = Mth.floor(bb.minY - 0.3);
		int maxY = Mth.ceil(bb.maxY + 0.3);
		int minZ = Mth.floor(bb.minZ);
		int maxZ = Mth.ceil(bb.maxZ);

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					mpos.set(x, y, z);
					FluidState fluid = this.level().getFluidState(mpos);
					if (fluid.is(FluidTags.WATER)) {
						inWater = true;
						double surface = y + fluid.getHeight(this.level(), mpos);
						if (surface > waterSurfaceY) {
							waterSurfaceY = surface;
						}
					}
				}
			}
		}

		Vec3 vel = this.getDeltaMovement();

		// Update anchor bed position if anchor entity is grounded
		AnchorEntity anchor = this.getAnchorEntity();
		if (anchor != null) {
			if (anchor.isAnchored() || anchor.onGround()) {
				this.setAnchorBedPos(anchor.blockPosition());
				double depth = Math.max(1.0, this.getY() - anchor.getY());
				float calculatedSlack = (float) Math.min(3.2, Math.max(1.2, Math.sqrt(depth * 0.8) + 0.6));
				this.setSlackRadius(calculatedSlack);
			}
		}

		if (inWater) {
			// Buoyant flotation: floats smoothly at water surface with gentle wave sinusoids
			double draft = this.getBuoyType() == TYPE_BLUE_CYLINDRICAL ? 0.32 : 0.22;
			double wavePhase = (this.tickCount + this.getId() * 37) * 0.06;
			double targetY = (waterSurfaceY - draft) + Math.sin(wavePhase) * 0.025;
			double yVel = (targetY - this.getY()) * 0.22;

			// Ambient wind drift
			WindVector wind = WindManager.getInstance().getWind(this.level());
			double windX = wind.x() * 0.0016;
			double windZ = wind.z() * 0.0016;

			// Ambient harmonic wave current
			double waveX = Math.sin(wavePhase * 0.7) * 0.0012;
			double waveZ = Math.cos(wavePhase * 0.85) * 0.0012;

			vel = new Vec3(vel.x + windX + waveX, yVel, vel.z + windZ + waveZ);

			// Rope tension constraint from anchor
			Optional<BlockPos> bedPosOpt = this.getAnchorBedPos();
			if (bedPosOpt.isPresent()) {
				BlockPos bed = bedPosOpt.get();
				double dx = this.getX() - (bed.getX() + 0.5);
				double dz = this.getZ() - (bed.getZ() + 0.5);
				double hDist = Math.sqrt(dx * dx + dz * dz);
				double maxSlack = this.getSlackRadius();

				if (hDist > maxSlack && hDist > 1.0E-4) {
					double excess = hDist - maxSlack;
					double restoring = Math.min(0.2, excess * 0.07);
					double nx = dx / hDist;
					double nz = dz / hDist;
					vel = vel.add(-nx * restoring, 0.0, -nz * restoring);
					vel = MooringConstraint.cancelOutwardVelocity(vel, nx, nz);
				}
			}

			// Hydrodynamic water drag
			vel = new Vec3(vel.x * 0.86, vel.y, vel.z * 0.86);
		} else {
			// In air or on land: gravity and air resistance
			vel = new Vec3(vel.x * 0.98, vel.y - 0.04, vel.z * 0.98);
		}

		this.move(MoverType.SELF, vel);
		this.setDeltaMovement(vel);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
	}

	@Nullable
	@Override
	public Leashable.LeashData getLeashData() {
		return this.leashData;
	}

	@Override
	public void setLeashData(@Nullable Leashable.LeashData leashData) {
		this.leashData = leashData;
	}

	@Override
	public boolean canBeLeashed() {
		return true;
	}

	@Override
	public Vec3 getLeashOffset() {
		return this.getBuoyType() == TYPE_BLUE_CYLINDRICAL
			? new Vec3(0.0, 0.95, 0.0)
			: new Vec3(0.0, 0.72, 0.0);
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return this.getLeashOffset();
	}

	@Override
	public boolean supportQuadLeash() {
		return false;
	}
}
