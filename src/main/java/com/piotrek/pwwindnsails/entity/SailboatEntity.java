package com.piotrek.pwwindnsails.entity;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.physics.MooringConstraint;
import com.piotrek.pwwindnsails.physics.SailingPhysics;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3.0m x 1.5m single-mast sailing boat entity.
 * Controlled by rudder (A/D) and mainsail sheet (W/S). Server-authoritative physics.
 */
public class SailboatEntity extends Entity implements Leashable {
	public static final int HIKE_NONE = 0;
	public static final int HIKE_SIT = 1;
	public static final int HIKE_STAND = 2;

	public static final int CLEAT_COUNT = 4;
	public static final int CLEAT_BOW_PORT = 0;
	public static final int CLEAT_BOW_STARBOARD = 1;
	public static final int CLEAT_STERN_PORT = 2;
	public static final int CLEAT_STERN_STARBOARD = 3;

	private static final EntityDataAccessor<Integer> CLEAT_0_HOLDER = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLEAT_1_HOLDER = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLEAT_2_HOLDER = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLEAT_3_HOLDER = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.INT);

	@SuppressWarnings("unchecked")
	public static final EntityDataAccessor<Integer>[] CLEAT_HOLDERS = new EntityDataAccessor[]{
		CLEAT_0_HOLDER, CLEAT_1_HOLDER, CLEAT_2_HOLDER, CLEAT_3_HOLDER
	};

	private static final EntityDataAccessor<Optional<BlockPos>> CLEAT_0_POS = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Optional<BlockPos>> CLEAT_1_POS = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Optional<BlockPos>> CLEAT_2_POS = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Optional<BlockPos>> CLEAT_3_POS = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

	@SuppressWarnings("unchecked")
	public static final EntityDataAccessor<Optional<BlockPos>>[] CLEAT_POS = new EntityDataAccessor[]{
		CLEAT_0_POS, CLEAT_1_POS, CLEAT_2_POS, CLEAT_3_POS
	};

	private final UUID[] cleatHolderUuids = new UUID[CLEAT_COUNT];
	private final BlockPos[] cleatHolderPositions = new BlockPos[CLEAT_COUNT];
	private final Entity[] cleatHolderEntities = new Entity[CLEAT_COUNT];

	private static final EntityDataAccessor<Float> RUDDER_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MAINSHEET = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> BOOM_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEEL_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HIKE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> HIKE_MODE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> SAIL_FURLED = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> HURT_TIME = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);

	// Client-side smooth rendering interpolation
	private float visualBoomAngleO;
	private float visualBoomAngle;
	private float visualHeelAngleO;
	private float visualHeelAngle;
	private float visualRudderAngleO;
	private float visualRudderAngle;

	// Server-side control input state
	private float clientRudderInput;
	private float clientSheetInput;
	private int inputFreshTicks;
	private float yawVelocity;
	private double portFaceSmooth = Double.NaN;
	private double starboardFaceSmooth = Double.NaN;
	private float mooringSway;

	@Nullable
	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
	private Leashable.LeashData leashData;

	public SailboatEntity(EntityType<? extends SailboatEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	public InterpolationHandler getInterpolation() {
		return this.interpolation;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(RUDDER_ANGLE, 0.0F);
		builder.define(MAINSHEET, 0.5F); // default to 50% sheet
		builder.define(BOOM_ANGLE, 0.0F);
		builder.define(HEEL_ANGLE, 0.0F);
		builder.define(HIKE, 0.0F);
		builder.define(HIKE_MODE, (byte) HIKE_NONE);
		builder.define(SAIL_FURLED, true);
		builder.define(HURT_TIME, 0);
		builder.define(DAMAGE, 0.0F);
		for (int i = 0; i < CLEAT_COUNT; i++) {
			builder.define(CLEAT_HOLDERS[i], -1);
			builder.define(CLEAT_POS[i], Optional.empty());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setRudderAngle(input.getFloatOr("RudderAngle", 0.0F));
		this.setMainsheet(input.getFloatOr("Mainsheet", 0.5F));
		this.setBoomAngle(input.getFloatOr("BoomAngle", 0.0F));
		this.setHeelAngle(input.getFloatOr("HeelAngle", 0.0F));
		this.setHike(input.getFloatOr("Hike", 0.0F));
		this.setHikeMode(input.getByteOr("HikeMode", (byte) HIKE_NONE));
		this.setSailFurled(input.getBooleanOr("SailFurled", true));
		this.setDamage(input.getFloatOr("Damage", 0.0F));
		this.readLeashData(input);
		for (int i = 0; i < CLEAT_COUNT; i++) {
			String uuidStr = input.getStringOr("Cleat" + i + "Uuid", "");
			if (!uuidStr.isEmpty()) {
				try {
					this.cleatHolderUuids[i] = UUID.fromString(uuidStr);
				} catch (IllegalArgumentException ignored) {}
			}
			int cx = input.getIntOr("Cleat" + i + "X", Integer.MIN_VALUE);
			if (cx != Integer.MIN_VALUE) {
				int cy = input.getIntOr("Cleat" + i + "Y", 0);
				int cz = input.getIntOr("Cleat" + i + "Z", 0);
				BlockPos knot = new BlockPos(cx, cy, cz);
				this.cleatHolderPositions[i] = knot;
				this.entityData.set(CLEAT_POS[i], Optional.of(knot));
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("RudderAngle", this.getRudderAngle());
		output.putFloat("Mainsheet", this.getMainsheet());
		output.putFloat("BoomAngle", this.getBoomAngle());
		output.putFloat("HeelAngle", this.getHeelAngle());
		output.putFloat("Hike", this.getHike());
		output.putByte("HikeMode", (byte) this.getHikeMode());
		output.putBoolean("SailFurled", this.isSailFurled());
		output.putFloat("Damage", this.getDamage());
		this.writeLeashData(output, this.leashData);
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.cleatHolderUuids[i] != null) {
				output.putString("Cleat" + i + "Uuid", this.cleatHolderUuids[i].toString());
			}
			BlockPos knot = this.cleatHolderPositions[i];
			if (knot == null) {
				knot = this.getCleatKnotPos(i).orElse(null);
			}
			if (knot != null) {
				output.putInt("Cleat" + i + "X", knot.getX());
				output.putInt("Cleat" + i + "Y", knot.getY());
				output.putInt("Cleat" + i + "Z", knot.getZ());
			}
		}
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		if (other != null && this.hasPassenger(other)) {
			return false;
		}
		return true;
	}

	@Override
	public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
		return this.interact(player, hand);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		// Only the looked-at cleat wire or fence post. Hull clicks must not select a neighbor cleat.
		if (handleAimedMooringClick(player)) {
			return InteractionResult.SUCCESS;
		}

		// If passenger is seated in the boat:
		if (this.hasPassenger(player)) {
			// Right-click ONLY toggles sails when clicking directly on the MAST!
			if (this.isAimingAtMast(player)) {
				if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
					this.toggleSailFurled();
				}
				return InteractionResult.SUCCESS;
			}
			// When looking anywhere else (water, sky, cockpit), PASS so the player can use/throw items (e.g. Anchor)!
			return InteractionResult.PASS;
		}

		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!this.level().isClientSide()) {
			return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		if (this.isInvulnerableToBase(damageSource)) {
			return false;
		}
		if (damageSource.getEntity() instanceof Player player && player.getAbilities().instabuild) {
			this.discard();
			return true;
		}
		this.setHurtTime(10);
		this.setDamage(this.getDamage() + amount * 10.0F);
		this.markHurt();
		if (this.getDamage() > 40.0F) {
			this.spawnAtLocation(level, this.getDropItemStack());
			this.discard();
		}
		return true;
	}

	public void setControlInput(float rudderInput, float sheetInput) {
		this.clientRudderInput = Mth.clamp(rudderInput, -1.0F, 1.0F);
		this.clientSheetInput = Mth.clamp(sheetInput, -1.0F, 1.0F);
		this.inputFreshTicks = 8;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateBoundingBox();

		if (this.level().isClientSide()) {
			this.getInterpolation().interpolate();
			this.visualBoomAngleO = this.visualBoomAngle;
			this.visualHeelAngleO = this.visualHeelAngle;
			this.visualRudderAngleO = this.visualRudderAngle;

			this.visualBoomAngle = Mth.lerp(0.2F, this.visualBoomAngle, this.getBoomAngle());
			this.visualHeelAngle = Mth.lerp(0.2F, this.visualHeelAngle, this.getHeelAngle());
			if (!this.rudderHeld) {
				this.visualRudderAngleO = 0.0F;
				this.visualRudderAngle = 0.0F;
			} else {
				this.visualRudderAngle = Mth.lerp(0.25F, this.visualRudderAngle, this.getRudderAngle());
			}

			// Smooth client prediction for gunwale seating and hiking
			WindVector clientWind = WindManager.getInstance().getClientState().getWindVector(this.level().getGameTime());
			float appFrom = clientWind.directionDeg() + 180.0F;
			float relW = Mth.wrapDegrees(appFrom - this.getYRot());
			float windwardSide = relW >= 0 ? 1.0F : -1.0F;
			float targetH = (clientWind.strength() < 0.04F) ? 1.0F : windwardSide;
			this.setHike(Mth.lerp(0.35F, this.getHike(), targetH));
			return;
		}

		// Server side logic. Do not touch blocks until this chunk is already in memory.
		if (!this.isChunkReady(this.getBlockX(), this.getBlockZ())) {
			return;
		}
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}

		// Flotation and water check
		AABB bb = this.getBoundingBox();
		double waterSurfaceY = Double.NEGATIVE_INFINITY;
		boolean inWater = false;
		int minX = Mth.floor(bb.minX);
		int maxX = Mth.ceil(bb.maxX);
		int minY = Mth.floor(bb.minY - 0.2);
		int maxY = Mth.ceil(bb.minY + 0.5);
		int minZ = Mth.floor(bb.minZ);
		int maxZ = Mth.ceil(bb.maxZ);

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					if (!this.isChunkReady(x, z)) {
						continue;
					}
					pos.set(x, y, z);
					FluidState fluid = this.level().getFluidState(pos);
					if (fluid.is(FluidTags.WATER)) {
						inWater = true;
						double surface = y + fluid.getHeight(this.level(), pos);
						if (surface > waterSurfaceY) {
							waterSurfaceY = surface;
						}
					}
				}
			}
		}

		// Vertical movement: buoyancy vs gravity
		Vec3 currentVel = this.getDeltaMovement();
		double yVel = currentVel.y;
		if (inWater) {
			double submergedDepth = waterSurfaceY - bb.minY;
			// Hull bottom rests on the water surface. A deeper draft clips the shore,
			// and the cockpit sole sits 0.375 blocks above this line so the interior stays dry.
			double targetYVel = submergedDepth * 0.18;
			yVel = Mth.lerp(0.35, yVel, targetYVel);
		} else {
			yVel = Math.max(-0.98, yVel - 0.04);
		}

		// 1. Gather active moorings across all 4 cleats
		List<CleatMooring> activeMoorings = new ArrayList<>();
		for (int i = 0; i < CLEAT_COUNT; i++) {
			Optional<BlockPos> knotPos = this.getCleatKnotPos(i);
			Entity holder = this.getCleatHolderEntity(i);

			if (knotPos.isPresent()) {
				BlockPos kp = knotPos.get();
				Vec3 cleatPos = this.getCleatWorldPos(i);
				Vec3 holderPos = new Vec3(kp.getX() + 0.5, kp.getY() + 0.5 + 0.2, kp.getZ() + 0.5);
				activeMoorings.add(new CleatMooring(i, holder != null ? holder : this, cleatPos, holderPos));
			} else if (holder != null && holder.isAlive()) {
				Vec3 cleatPos = this.getCleatWorldPos(i);
				Vec3 holderPos = holder.getRopeHoldPosition(1.0F);
				activeMoorings.add(new CleatMooring(i, holder, cleatPos, holderPos));
			} else if (this.cleatHolderPositions[i] != null || this.cleatHolderUuids[i] != null) {
				// Saved mooring is still resolving. Do not drop it on the first ticks after load.
			} else if ((this.entityData.get(CLEAT_HOLDERS[i]) != -1 || this.entityData.get(CLEAT_POS[i]).isPresent()) && !this.level().isClientSide()) {
				this.detachCleat(i, false);
			}
		}

		List<CleatMooring> fixedMoorings = new ArrayList<>();
		boolean singlePlayerTow = activeMoorings.size() == 1
			&& activeMoorings.get(0).holder() instanceof Player playerHolder
			&& playerHolder != this.getFirstPassenger();

		if (singlePlayerTow) {
			CleatMooring m = activeMoorings.get(0);
			// The tied cleat leads. A stern cleat must not swing the bow toward the puller.
			Vec3 local = this.getCleatLocalOffset(m.cleatIndex());
			float error = MooringConstraint.cleatYawError(
				this.getYRot(), this.getX(), this.getZ(), local.x, local.z, m.holderPos().x, m.holderPos().z);
			float turn = Mth.clamp(error * 0.08F, -5.0F, 5.0F);
			this.yawVelocity = Mth.lerp(0.5F, this.yawVelocity, turn);

			Vec3 line = m.holderPos().subtract(m.cleatPos());
			double dist = Math.hypot(line.x, line.z);
			if (dist > 2.0) {
				double tension = Math.min(1.0, (dist - 2.0) / 4.0);
				double pullStrength = tension * 0.16;
				currentVel = currentVel.add((line.x / dist) * pullStrength, 0.0, (line.z / dist) * pullStrength);
			}
			Vec3 resolvedVel = this.resolveHullCollisions(new Vec3(currentVel.x, yVel, currentVel.z));
			this.setDeltaMovement(resolvedVel);
		} else {
			for (CleatMooring mooring : activeMoorings) {
				if (this.isFixedMooring(mooring)) {
					fixedMoorings.add(mooring);
				}
			}
		}

		// Gentle wind drift for a free boat, and for a boat swinging on one or more lines.
		if ((activeMoorings.isEmpty() || !fixedMoorings.isEmpty()) && inWater && !isLeadTarget(this)) {
			if (this.isSailFurled() || this.getPassengers().isEmpty()) {
				WindVector wind = WindManager.getInstance().getWind(this.level(), this.position());
				if (wind != null && wind.strength() > 0.02F) {
					Vec3 windDir = wind.toVec3().normalize();
					double driftForce = wind.strength() * 0.005;
					currentVel = currentVel.add(windDir.x * driftForce, 0.0, windDir.z * driftForce);
					currentVel = new Vec3(currentVel.x * 0.92, yVel, currentVel.z * 0.92);
				}
			}
		}

		// Process pilot inputs
		if (this.inputFreshTicks > 0) {
			this.inputFreshTicks--;
		} else {
			this.clientRudderInput = 0.0F;
			this.clientSheetInput = 0.0F;
		}

		// A / D Rudder steering with automatic centering
		float currentRudder = this.getRudderAngle();
		if (Math.abs(this.clientRudderInput) > 0.01F) {
			currentRudder += this.clientRudderInput * WindAndSailsConfig.rudderRatePerTick * WindAndSailsConfig.rudderSensitivity;
		} else {
			// Releasing A/D centers the rudder immediately.
			currentRudder = 0.0F;
		}
		currentRudder = Mth.clamp(currentRudder, -WindAndSailsConfig.maxRudderAngleDeg, WindAndSailsConfig.maxRudderAngleDeg);

		// Mainsheet setting (W/S)
		float currentSheet = this.getMainsheet();
		if (Math.abs(this.clientSheetInput) > 0.01F) {
			currentSheet += this.clientSheetInput * WindAndSailsConfig.SHEET_CHANGE_RATE;
		}
		currentSheet = Mth.clamp(currentSheet, 0.0F, 1.0F);

		// Sailing physics step
		WindVector trueWind = WindManager.getInstance().getWind(this.level(), this.position());
		Vec3 velWithY = new Vec3(currentVel.x, yVel, currentVel.z);

		// Determine windward side for hiking: +1.0 = starboard, -1.0 = port
		float appWindFromDeg = trueWind.directionDeg() + 180.0F;
		float relWindDeg = Mth.wrapDegrees(appWindFromDeg - this.getYRot());
		float windwardSide = relWindDeg >= 0 ? 1.0F : -1.0F;

		int mode = this.getHikeMode();
		// Default position is already on the windward gunwale bench!
		float targetHike = (trueWind.strength() < 0.04F) ? 1.0F : windwardSide;
		float currentHike = this.getHike();
		float newHike = Mth.lerp(0.35F, currentHike, targetHike);
		if (Math.abs(newHike - targetHike) < 0.02F) {
			newHike = targetHike;
		}
		this.setHike(newHike);

		SailingPhysics.PhysicsResult physics = SailingPhysics.step(
			velWithY,
			this.getYRot(),
			this.yawVelocity,
			currentRudder,
			currentSheet,
			trueWind,
			this.getHeelAngle(),
			this.isSailFurled(),
			inWater,
			mode
		);

		this.yawVelocity = physics.newYawVelocity();
		float targetYaw = physics.newYaw();
		if (!fixedMoorings.isEmpty() && trueWind != null && trueWind.strength() > 0.02F) {
			float helmScale = Math.abs(this.clientRudderInput) > 0.01F ? 0.25F : this.getPassengers().isEmpty() ? 1.0F : 0.55F;
			float vane = this.lineYawStep(fixedMoorings, trueWind.strength(), helmScale);
			float proposed = targetYaw + vane;
			float excessBefore = this.lineExcess(fixedMoorings, this.getYRot());
			float excessAfter = this.lineExcess(fixedMoorings, proposed);
			// A diagonal line may yaw the tied end onto the post. Only a hard twist that
			// would snap several taut lines is refused; the length clamp still holds.
			if (excessAfter > excessBefore + 0.45F && excessBefore > 0.2F) {
				proposed = targetYaw;
				vane = 0.0F;
			}
			targetYaw = proposed;
			this.yawVelocity = physics.newYawVelocity() * 0.35F + vane;
		}
		// If rotating into a solid shore block would cause hull collision, stop yaw rotation
		if (!this.isHullColliding(this.getX(), this.getY(), this.getZ(), targetYaw)) {
			this.setYRot(targetYaw);
		} else {
			this.yawVelocity = 0.0F;
		}

		Vec3 resolvedVel = this.resolveHullCollisions(physics.newVelocity());
		if (!fixedMoorings.isEmpty() && inWater && trueWind != null && trueWind.strength() > 0.02F) {
			Vec3 downwind = trueWind.toVec3().normalize();
			double swing = trueWind.strength() * 0.018;
			resolvedVel = new Vec3(
				resolvedVel.x + downwind.x * swing,
				resolvedVel.y,
				resolvedVel.z + downwind.z * swing
			);
		}
		resolvedVel = this.resolveHullCollisions(resolvedVel.add(this.obstacleRepulsion()));
		this.setDeltaMovement(resolvedVel);

		// Apply synched data
		this.setRudderAngle(currentRudder);
		this.setMainsheet(currentSheet);
		this.setBoomAngle(physics.boomAngleDeg());
		this.setHeelAngle(physics.heelAngleDeg());

		this.move(MoverType.SELF, this.getDeltaMovement());
		this.updateBoundingBox();
		this.separateFromObstacles();
		this.updateBoundingBox();
		if (!fixedMoorings.isEmpty()) {
			for (int pass = 0; pass < 4; pass++) {
				for (CleatMooring mooring : fixedMoorings) {
					this.constrainToLeadSlack(mooring);
				}
			}
			this.separateFromObstacles();
		}
		this.applyFenderCushion();
		this.separateFromObstacles();
		this.applyMooringSway(!fixedMoorings.isEmpty() || this.hasPortDockFenders() || this.hasStarboardDockFenders());
		this.enforceInextensibleLeads();
		this.needsSync = true;
	}

	/**
	 * Symmetrical bounding box aligned with the boat center of mass and hull width (1.40m).
	 * Unlike an exaggerated 3.18m diagonal bounding box, this allows seamless passage through
	 * diagonal block channels while precise hull geometry is handled by SAT collision checks.
	 */
	private void updateBoundingBox() {
		double halfW = 0.70;
		double y = this.getY();
		this.setBoundingBox(new AABB(
			this.getX() - halfW, y, this.getZ() - halfW,
			this.getX() + halfW, y + 0.65, this.getZ() + halfW
		));
	}

	/**
	 * Precise Oriented Bounding Box (OBB) collision check for the boat hull against a block's collision box.
	 * Uses 2D Separating Axis Theorem (SAT) in the horizontal plane (XZ) with forward bow taper,
	 * preventing phantom block collision on diagonal sailing paths where blocks don't touch the physical hull.
	 */
	public static boolean hullIntersectsBlock(double boatX, double boatY, double boatZ, float yaw, AABB blockBox) {
		return SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, blockBox);
	}

	public boolean isHullColliding(double boatX, double boatY, double boatZ, float yaw) {
		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);

		double maxFwd = this.getHullMaxFwd();
		double minFwd = this.getHullMinFwd();
		double maxDim = Math.max(Math.abs(minFwd), Math.abs(maxFwd));

		double extX = Math.abs(fwdX) * maxDim + Math.abs(rightX) * 0.68;
		double extZ = Math.abs(fwdZ) * maxDim + Math.abs(rightZ) * 0.68;

		int minBlockX = Mth.floor(boatX - extX - 0.05);
		int maxBlockX = Mth.ceil(boatX + extX + 0.05);
		int minBlockZ = Mth.floor(boatZ - extZ - 0.05);
		int maxBlockZ = Mth.ceil(boatZ + extZ + 0.05);
		int minBlockY = Mth.floor(boatY);
		int maxBlockY = Mth.ceil(boatY + 0.75);

		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
		CollisionContext context = CollisionContext.of(this);

		for (int x = minBlockX; x < maxBlockX; x++) {
			for (int y = minBlockY; y <= maxBlockY; y++) {
				for (int z = minBlockZ; z < maxBlockZ; z++) {
					if (!this.isChunkReady(x, z)) {
						continue;
					}
					mpos.set(x, y, z);
					BlockState state = this.level().getBlockState(mpos);
					if (state.isAir()) {
						continue;
					}
					if (state.is(Blocks.WATER) || state.is(Blocks.LILY_PAD)) {
						continue;
					}
					VoxelShape shape = state.getCollisionShape(this.level(), mpos, context);
					if (shape.isEmpty()) {
						continue;
					}
					for (AABB box : shape.toAabbs()) {
						AABB worldBox = box.move(x, y, z);
						if (SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, worldBox, minFwd, maxFwd, this.getBowTaperStart())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private Vec3 resolveHullCollisions(Vec3 vel) {
		if (vel.lengthSqr() < 1e-7) {
			return vel;
		}
		double targetX = this.getX() + vel.x;
		double targetY = this.getY() + vel.y;
		double targetZ = this.getZ() + vel.z;
		float yaw = this.getYRot();

		// If full movement doesn't cause any hull collision, proceed directly
		if (!this.isHullColliding(targetX, targetY, targetZ, yaw)) {
			return vel;
		}

		// Try sliding along X or Z axis
		boolean canMoveX = (Math.abs(vel.x) > 1e-4) && !this.isHullColliding(targetX, targetY, this.getZ(), yaw);
		boolean canMoveZ = (Math.abs(vel.z) > 1e-4) && !this.isHullColliding(this.getX(), targetY, targetZ, yaw);

		if (canMoveX && !canMoveZ) {
			return new Vec3(vel.x, vel.y, 0.0);
		}
		if (canMoveZ && !canMoveX) {
			return new Vec3(0.0, vel.y, vel.z);
		}
		if (canMoveX && canMoveZ) {
			if (Math.abs(vel.x) >= Math.abs(vel.z)) {
				return new Vec3(vel.x, vel.y, 0.0);
			} else {
				return new Vec3(0.0, vel.y, vel.z);
			}
		}

		// Blocked completely horizontally
		return new Vec3(0.0, vel.y, 0.0);
	}

	/**
	 * Wind and lines must not carry the hull onto a dock or shore.
	 * Push back into the water; never step up onto the obstacle.
	 */
	private void separateFromObstacles() {
		if (!this.isHullColliding(this.getX(), this.getY(), this.getZ(), this.getYRot())) {
			return;
		}
		Vec3 away = this.obstacleRepulsion();
		double[] clear = this.findClearingStep(away.x, away.z, true);
		if (clear == null) {
			clear = this.findClearingStep(0.0, 0.0, false);
		}
		if (clear == null) {
			return;
		}
		this.setPos(clear[0], this.getY(), clear[1]);
		this.updateBoundingBox();
		this.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(this.getDeltaMovement(), -clear[2], -clear[3]));
	}

	/** @return x, z, dirX, dirZ of the shortest step back into clear water, or null. */
	private double[] findClearingStep(double preferX, double preferZ, boolean preferOnly) {
		boolean hasPrefer = preferX * preferX + preferZ * preferZ > 1.0E-8;
		double bestStep = Double.MAX_VALUE;
		double[] best = null;
		for (int dir = 0; dir < 16; dir++) {
			double ang = dir * (Math.PI * 2.0 / 16.0);
			double dx = Math.cos(ang);
			double dz = Math.sin(ang);
			if (preferOnly && hasPrefer && dx * preferX + dz * preferZ < 0.0) {
				continue;
			}
			for (int i = 1; i <= 20; i++) {
				double step = i * 0.08;
				double x = this.getX() + dx * step;
				double z = this.getZ() + dz * step;
				if (!this.isHullColliding(x, this.getY(), z, this.getYRot())) {
					if (step < bestStep) {
						bestStep = step;
						best = new double[]{ x, z, dx, dz };
					}
					break;
				}
			}
		}
		return best;
	}

	/** Gentle push off a nearby pier, fence post or shore, sampled along the whole hull. */
	private Vec3 obstacleRepulsion() {
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		double minFwd = this.getHullMinFwd() - 0.45;
		double maxFwd = this.getHullMaxFwd() + 0.45;
		Vec3 push = Vec3.ZERO;
		for (double f = minFwd; f <= maxFwd; f += 0.55) {
			for (double r = -1.15; r <= 1.15; r += 0.55) {
				double x = this.getX() + rightX * r + fwdX * f;
				double z = this.getZ() + rightZ * r + fwdZ * f;
				if (!this.columnBlocksHull(x, z)) {
					continue;
				}
				push = push.add(SailingPhysics.awayFromContact(this.getX(), this.getZ(), x, z, 0.012));
			}
		}
		double cap = 0.06;
		double len = Math.hypot(push.x, push.z);
		if (len > cap) {
			push = push.scale(cap / len);
		}
		return push;
	}

	private boolean isChunkReady(int blockX, int blockZ) {
		return com.piotrek.pwwindnsails.world.ChunkReady.is(this.level(), blockX, blockZ);
	}

	private boolean columnBlocksHull(double x, double z) {
		int bx = Mth.floor(x);
		int bz = Mth.floor(z);
		if (!this.isChunkReady(bx, bz)) {
			return false;
		}
		int by = Mth.floor(this.getY());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		CollisionContext context = CollisionContext.of(this);
		for (int y = by; y <= by + 1; y++) {
			pos.set(bx, y, bz);
			BlockState state = this.level().getBlockState(pos);
			if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LILY_PAD)) {
				continue;
			}
			VoxelShape shape = state.getCollisionShape(this.level(), pos, context);
			if (shape.isEmpty()) {
				continue;
			}
			for (AABB box : shape.toAabbs()) {
				double top = y + box.maxY;
				double bottom = y + box.minY;
				if (top > this.getY() + 0.15 && bottom < this.getY() + 0.9) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double xOffset = -Mth.sin(yawRad) * this.getPassengerCenterZOffset();
		double zOffset = Mth.cos(yawRad) * this.getPassengerCenterZOffset();

		float hike = this.getHike();
		if (Math.abs(hike) < 0.05F) {
			hike = 1.0F;
		}
		int mode = this.getHikeMode();

		// Default position: sitting on gunwale bench (0.56m)
		// Mode 1: deep gunwale sit (0.62m)
		// Mode 2: standing on gunwale (0.68m)
		double baseLateral = (mode >= HIKE_STAND) ? 0.68 : (mode == HIKE_SIT ? 0.62 : 0.56);
		double lateralDist = baseLateral * hike;
		double lateralX = -Mth.cos(yawRad) * lateralDist;
		double lateralZ = -Mth.sin(yawRad) * lateralDist;

		double baseY = (mode >= HIKE_STAND) ? 0.88 : (mode == HIKE_SIT ? 0.46 : 0.44);
		double yOffset = baseY + Math.abs(hike) * (mode >= HIKE_STAND ? 0.04 : 0.02);

		return new Vec3(xOffset + lateralX, yOffset, zOffset + lateralZ);
	}

	protected float clampRotation(Entity passenger) {
		float hike = this.getHike();
		// Body torso is always turned toward the center of the boat!
		// Starboard gunwale (hike >= 0) -> face left towards center (boatYaw - 90)
		// Port gunwale (hike < 0) -> face right towards center (boatYaw + 90)
		float targetBodyRot = (hike >= 0) ? (this.getYRot() - 90.0F) : (this.getYRot() + 90.0F);
		passenger.setYBodyRot(targetBodyRot);
		return Mth.wrapDegrees(passenger.getYRot() - targetBodyRot);
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		this.clampRotation(passenger);
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (this.getPassengers().isEmpty()) {
			this.setHikeMode(HIKE_NONE);
			this.setHike(0.0F);
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// 1. Search for safe dry land in the immediate vicinity of the boat (within ~4.5 blocks)
		BlockPos centerPos = this.blockPosition();
		Vec3 boatPos = this.position();
		List<Vec3> dryLandCandidates = new ArrayList<>();

		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				double distSq = dx * dx + dz * dz;
				if (distSq > 4.5 * 4.5) {
					continue;
				}
				for (int dy = 2; dy >= -2; dy--) {
					BlockPos testPos = centerPos.offset(dx, dy, dz);
					BlockPos groundPos = testPos.below();

					// Must not be in water or on water (strictly dry land!)
					if (this.level().isWaterAt(testPos) || this.level().getFluidState(testPos).is(FluidTags.WATER) ||
						this.level().isWaterAt(groundPos) || this.level().getFluidState(groundPos).is(FluidTags.WATER)) {
						continue;
					}

					// Ground block must not be air or fluid
					if (this.level().getBlockState(groundPos).isAir() || !this.level().getFluidState(groundPos).isEmpty()) {
						continue;
					}

					Vec3 safeLocation = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), testPos, true);
					if (safeLocation != null) {
						BlockPos safeBlockPos = BlockPos.containing(safeLocation);
						BlockPos safeGroundPos = safeBlockPos.below();
						// Double-check no water at the exact safe target position
						if (!this.level().isWaterAt(safeBlockPos) && !this.level().getFluidState(safeBlockPos).is(FluidTags.WATER) &&
							!this.level().isWaterAt(safeGroundPos) && !this.level().getFluidState(safeGroundPos).is(FluidTags.WATER)) {
							dryLandCandidates.add(safeLocation);
						}
					}
				}
			}
		}

		if (!dryLandCandidates.isEmpty()) {
			// Pick candidate closest to boat / passenger position
			dryLandCandidates.sort(Comparator.comparingDouble(loc -> {
				double dHoriz = Math.hypot(loc.x - boatPos.x, loc.z - boatPos.z);
				double dY = Math.abs(loc.y - boatPos.y);
				return dHoriz + dY * 0.6;
			}));
			return dryLandCandidates.get(0);
		}

		// 2. Fallback when in open water (no shore nearby):
		// Dismount onto starboard or port side, or on top of the boat
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);

		// Try starboard then port
		for (double sideSign : new double[]{ 1.5, -1.5 }) {
			Vec3 sideTarget = boatPos.add(rightX * sideSign, 0.0, rightZ * sideSign);
			BlockPos sideBlock = BlockPos.containing(sideTarget);
			Vec3 safeLoc = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), sideBlock, false);
			if (safeLoc != null) {
				return safeLoc;
			}
		}

		// Default fallback
		return new Vec3(this.getX(), this.getBoundingBox().maxY + 0.05, this.getZ());
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide()) {
			for (int i = 0; i < CLEAT_COUNT; i++) {
				if (this.isCleatAttached(i)) {
					this.detachCleat(i, false);
				}
			}
		}
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
	public boolean canHaveALeashAttachedTo(Entity holder) {
		// Prevent vanilla system from auto-binding this sailboat during general leash operations
		return false;
	}

	@Override
	public float getPickRadius() {
		return 1.6F;
	}

	@Override
	public boolean isLeashed() {
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.isCleatAttached(i)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	@Override
	public Entity getLeashHolder() {
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.isCleatAttached(i)) {
				Entity e = this.getCleatHolderEntity(i);
				if (e instanceof Player) {
					return e;
				}
			}
		}
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.isCleatAttached(i)) {
				Entity e = this.getCleatHolderEntity(i);
				if (e != null) {
					return e;
				}
			}
		}
		return null;
	}

	@Override
	public void setLeashedTo(Entity holder, boolean broadcast) {
		// Only attach to a cleat if explicitly called by our cleat system
	}

	@Override
	public void dropLeash() {
		// Do not drop all cleats automatically during vanilla tickLeash
	}

	@Override
	public Vec3 getLeashOffset() {
		// Lead attaches precisely to the bow stem at deck height
		return new Vec3(0.0, 0.45, this.getBowLeashOffsetZ());
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return new Vec3(0.0, 0.45, this.getBowLeashOffsetZ());
	}

	@Override
	public boolean checkElasticInteractions(Entity holder, Leashable.LeashData leashData) {
		// Completely disable vanilla elastic spring pull so boats never get aggressively dragged!
		return false;
	}

	@Override
	public boolean supportQuadLeash() {
		return false;
	}

	@Override
	public void onLeashRemoved() {
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.LEAD_UNTIED, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	// --- Cleat Mooring & Anchor Mechanics ---

	@Nullable
	public Entity getCleatHolderEntity(int cleatIndex) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return null;
		}
		if (this.cleatHolderEntities[cleatIndex] != null && this.cleatHolderEntities[cleatIndex].isAlive()) {
			return this.cleatHolderEntities[cleatIndex];
		}
		int entityId = this.entityData.get(CLEAT_HOLDERS[cleatIndex]);
		if (entityId != -1) {
			Entity e = this.level().getEntity(entityId);
			if (e != null) {
				this.cleatHolderEntities[cleatIndex] = e;
				return e;
			}
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			if (this.cleatHolderUuids[cleatIndex] != null) {
				Entity e = serverLevel.getEntity(this.cleatHolderUuids[cleatIndex]);
				if (e == null) {
					e = serverLevel.getPlayerByUUID(this.cleatHolderUuids[cleatIndex]);
				}
				if (e != null) {
					this.cleatHolderEntities[cleatIndex] = e;
					this.entityData.set(CLEAT_HOLDERS[cleatIndex], e.getId());
					return e;
				}
			}
			if (this.cleatHolderPositions[cleatIndex] != null) {
				BlockPos saved = this.cleatHolderPositions[cleatIndex];
				this.entityData.set(CLEAT_POS[cleatIndex], Optional.of(saved));
				if (!com.piotrek.pwwindnsails.world.ChunkReady.is(this.level(), saved)) {
					return null;
				}
				LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(serverLevel, saved);
				if (knot != null) {
					this.cleatHolderEntities[cleatIndex] = knot;
					this.cleatHolderUuids[cleatIndex] = knot.getUUID();
					this.entityData.set(CLEAT_HOLDERS[cleatIndex], knot.getId());
					return knot;
				}
			}
		}
		return null;
	}

	public boolean isCleatAttachedTo(Entity entity) {
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.getCleatHolderEntity(i) == entity) {
				return true;
			}
		}
		return false;
	}

	public double getBowCleatLocalZ() {
		return 1.00;
	}

	public double getSternCleatLocalZ() {
		return -1.00;
	}

	public Vec3 getCleatLocalOffset(int cleatIndex) {
		double x = (cleatIndex % 2 == 0) ? -0.6875 : 0.6875;
		double y = 0.6875;
		double z = (cleatIndex < 2) ? this.getBowCleatLocalZ() : this.getSternCleatLocalZ();
		return new Vec3(x, y, z);
	}

	public double getMastLocalZ() {
		return 0.78;
	}

	public double getMastHeight() {
		return 4.2;
	}

	public boolean isAimingAtMast(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);

		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double mastX = this.getX() - Mth.sin(yawRad) * this.getMastLocalZ();
		double mastZ = this.getZ() + Mth.cos(yawRad) * this.getMastLocalZ();

		double dx = mastX - eye.x;
		double dz = mastZ - eye.z;
		double lookH2 = look.x * look.x + look.z * look.z;
		if (lookH2 < 1e-4) {
			return false;
		}

		double t = (dx * look.x + dz * look.z) / lookH2;
		if (t < 0.1 || t > 7.0) {
			return false;
		}

		double rayX = eye.x + look.x * t;
		double rayZ = eye.z + look.z * t;
		double rayY = eye.y + look.y * t;

		double distH = Math.hypot(rayX - mastX, rayZ - mastZ);
		double minY = this.getY() + 0.35;
		double maxY = this.getY() + this.getMastHeight();

		return distH <= 0.45 && rayY >= minY && rayY <= maxY;
	}

	public Vec3 getCleatWorldPos(int cleatIndex) {
		return this.getCleatWorldPos(cleatIndex, this.position(), this.getYRot());
	}

	public Vec3 getCleatWorldPos(int cleatIndex, Vec3 boatPos, float yaw) {
		Vec3 local = this.getCleatLocalOffset(cleatIndex);
		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);

		double wx = boatPos.x + rightX * local.x + fwdX * local.z;
		double wy = boatPos.y + local.y;
		double wz = boatPos.z + rightZ * local.x + fwdZ * local.z;
		return new Vec3(wx, wy, wz);
	}

	public int getClosestCleat(Vec3 hitOffset) {
		float yawRad = -this.getYRot() * Mth.DEG_TO_RAD;
		double lx = hitOffset.x * Mth.cos(yawRad) - hitOffset.z * Mth.sin(yawRad);
		double lz = hitOffset.x * Mth.sin(yawRad) + hitOffset.z * Mth.cos(yawRad);

		int best = 0;
		double minD = Double.MAX_VALUE;
		for (int i = 0; i < CLEAT_COUNT; i++) {
			Vec3 c = this.getCleatLocalOffset(i);
			double d = Math.hypot(lx - c.x, lz - c.z);
			if (d < minD) {
				minD = d;
				best = i;
			}
		}
		return best;
	}

	/** Both cleats on one side tied to the dock: hang fenders and keep a half-block cushion. */
	public static final double FENDER_GAP = 0.5;

	public boolean isDockTied(int cleatIndex) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return false;
		}
		if (this.getCleatKnotPos(cleatIndex).isPresent()) {
			return true;
		}
		return this.cleatHolderPositions[cleatIndex] != null;
	}

	public boolean hasPortDockFenders() {
		return this.isDockTied(CLEAT_BOW_PORT) && this.isDockTied(CLEAT_STERN_PORT);
	}

	public boolean hasStarboardDockFenders() {
		return this.isDockTied(CLEAT_BOW_STARBOARD) && this.isDockTied(CLEAT_STERN_STARBOARD);
	}

	/** Slow beam rock instead of the line/fender buzz. */
	private void applyMooringSway(boolean moored) {
		if (!moored) {
			this.mooringSway = 0.0F;
			return;
		}
		float phase = (this.tickCount + this.getId() * 19) * 0.045F;
		float next = Mth.sin(phase) * 0.035F;
		float delta = next - this.mooringSway;
		this.mooringSway = next;
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		this.setPos(this.getX() + rightX * delta, this.getY(), this.getZ() + rightZ * delta);
		this.updateBoundingBox();
		this.setHeelAngle(Mth.sin(phase * 0.85F) * 1.8F);
	}

	private void applyFenderCushion() {
		if (this.hasPortDockFenders()) {
			this.holdOffDock(-1);
		}
		if (this.hasStarboardDockFenders()) {
			this.holdOffDock(1);
		}
	}

	/**
	 * sideSign -1 = port, +1 = starboard.
	 * Dock planks sit at the waterline, so the hull check ignores them and the boat rides up.
	 * Keep the gunwale {@link #FENDER_GAP} short of the dock's water edge.
	 */
	private void holdOffDock(int sideSign) {
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double sideX = Mth.cos(yawRad) * sideSign;
		double sideZ = Mth.sin(yawRad) * sideSign;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double face = this.nearestDockFace(sideX, sideZ, fwdX, fwdZ);
		double knotFace = this.nearestTiedPostFace(sideSign, sideX, sideZ);
		if (knotFace < face) {
			face = knotFace;
		}
		if (face == Double.POSITIVE_INFINITY) {
			return;
		}
		// Positive error: hull is too far, haul both lines in. Negative: riding onto the dock, push out.
		double measured = face;
		double smooth = sideSign < 0 ? this.portFaceSmooth : this.starboardFaceSmooth;
		if (Double.isNaN(smooth)) {
			smooth = measured;
		} else {
			smooth = Mth.lerp(0.12D, smooth, measured);
		}
		if (sideSign < 0) {
			this.portFaceSmooth = smooth;
		} else {
			this.starboardFaceSmooth = smooth;
		}
		double error = smooth - (0.72 + FENDER_GAP);
		// Wide dead zone so the hull does not buzz against the fender line.
		if (Math.abs(error) > 0.14) {
			double step = Mth.clamp(error * 0.08, -0.06, 0.05);
			this.setPos(this.getX() + sideX * step, this.getY(), this.getZ() + sideZ * step);
			this.updateBoundingBox();
		}
		this.setDeltaMovement(this.stripSideVelocity(this.getDeltaMovement(), sideX, sideZ));
	}

	private double nearestTiedPostFace(int sideSign, double sideX, double sideZ) {
		double nearest = Double.POSITIVE_INFINITY;
		int bow = sideSign < 0 ? CLEAT_BOW_PORT : CLEAT_BOW_STARBOARD;
		int stern = sideSign < 0 ? CLEAT_STERN_PORT : CLEAT_STERN_STARBOARD;
		for (int cleat : new int[]{ bow, stern }) {
			Optional<BlockPos> knot = this.getCleatKnotPos(cleat);
			if (knot.isEmpty() && this.cleatHolderPositions[cleat] != null) {
				knot = Optional.of(this.cleatHolderPositions[cleat]);
			}
			if (knot.isEmpty()) {
				continue;
			}
			BlockPos pos = knot.get();
			double x0 = pos.getX() - this.getX();
			double x1 = pos.getX() + 1.0 - this.getX();
			double z0 = pos.getZ() - this.getZ();
			double z1 = pos.getZ() + 1.0 - this.getZ();
			double face = Math.min(
				Math.min(x0 * sideX + z0 * sideZ, x1 * sideX + z0 * sideZ),
				Math.min(x0 * sideX + z1 * sideZ, x1 * sideX + z1 * sideZ)
			);
			if (face < nearest) {
				nearest = face;
			}
		}
		return nearest;
	}

	private Vec3 stripSideVelocity(Vec3 velocity, double sideX, double sideZ) {
		double along = velocity.x * sideX + velocity.z * sideZ;
		return new Vec3(velocity.x - sideX * along, velocity.y, velocity.z - sideZ * along);
	}

	/** Side-axis distance from the boat center to the dock edge. +inf if no dock on that side. */
	private double[] dropDockwardPull(double pullX, double pullZ, int sideSign) {
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double sideX = Mth.cos(yawRad) * sideSign;
		double sideZ = Mth.sin(yawRad) * sideSign;
		double toward = pullX * sideX + pullZ * sideZ;
		if (toward > 0.0) {
			pullX -= sideX * toward;
			pullZ -= sideZ * toward;
		}
		return new double[]{ pullX, pullZ };
	}

	private double nearestDockFace(double sideX, double sideZ, double fwdX, double fwdZ) {
		double nearest = Double.POSITIVE_INFINITY;
		int minX = Mth.floor(this.getX() - 4.0);
		int maxX = Mth.floor(this.getX() + 4.0);
		int minZ = Mth.floor(this.getZ() - 4.0);
		int maxZ = Mth.floor(this.getZ() + 4.0);
		int minY = Mth.floor(this.getY() - 1.0);
		int maxY = Mth.floor(this.getY() + 1.2);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		CollisionContext context = CollisionContext.of(this);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!this.isChunkReady(x, z)) {
					continue;
				}
				for (int y = minY; y <= maxY; y++) {
					pos.set(x, y, z);
					BlockState state = this.level().getBlockState(pos);
					if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LILY_PAD)) {
						continue;
					}
					VoxelShape shape = state.getCollisionShape(this.level(), pos, context);
					if (shape.isEmpty()) {
						continue;
					}
					for (AABB box : shape.toAabbs()) {
						double top = y + box.maxY;
						double bottom = y + box.minY;
						// Waterline planks count. The deep lake bed does not.
						if (top < this.getY() - 0.55 || bottom > this.getY() + 1.05) {
							continue;
						}
						double minBX = x + box.minX;
						double maxBX = x + box.maxX;
						double minBZ = z + box.minZ;
						double maxBZ = z + box.maxZ;
						double cx = (minBX + maxBX) * 0.5 - this.getX();
						double cz = (minBZ + maxBZ) * 0.5 - this.getZ();
						double along = cx * fwdX + cz * fwdZ;
						if (along < this.getHullMinFwd() - 0.6 || along > this.getHullMaxFwd() + 0.6) {
							continue;
						}
						double centerSide = cx * sideX + cz * sideZ;
						if (centerSide < -0.35) {
							continue;
						}
						double s0 = (minBX - this.getX()) * sideX + (minBZ - this.getZ()) * sideZ;
						double s1 = (maxBX - this.getX()) * sideX + (minBZ - this.getZ()) * sideZ;
						double s2 = (minBX - this.getX()) * sideX + (maxBZ - this.getZ()) * sideZ;
						double s3 = (maxBX - this.getX()) * sideX + (maxBZ - this.getZ()) * sideZ;
						double face = Math.min(Math.min(s0, s1), Math.min(s2, s3));
						if (face < nearest) {
							nearest = face;
						}
					}
				}
			}
		}
		return nearest;
	}

	public boolean isCleatAttached(int cleatIndex) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return false;
		}
		return this.entityData.get(CLEAT_HOLDERS[cleatIndex]) != -1 || this.entityData.get(CLEAT_POS[cleatIndex]).isPresent();
	}

	public Optional<BlockPos> getCleatKnotPos(int cleatIndex) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return Optional.empty();
		}
		return this.entityData.get(CLEAT_POS[cleatIndex]);
	}

	public void attachCleat(int cleatIndex, Entity holder) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return;
		}
		this.cleatHolderUuids[cleatIndex] = holder.getUUID();
		this.cleatHolderEntities[cleatIndex] = holder;
		if (holder instanceof LeashFenceKnotEntity knot) {
			this.cleatHolderPositions[cleatIndex] = knot.getPos();
			this.entityData.set(CLEAT_POS[cleatIndex], Optional.of(knot.getPos()));
		} else {
			this.cleatHolderPositions[cleatIndex] = null;
			this.entityData.set(CLEAT_POS[cleatIndex], Optional.empty());
		}
		this.entityData.set(CLEAT_HOLDERS[cleatIndex], holder.getId());
		Leashable.super.setLeashedTo(holder, true);
	}

	public void detachCleat(int cleatIndex, boolean dropLead) {
		this.detachCleat(cleatIndex, dropLead, true);
	}

	public void detachCleat(int cleatIndex, boolean dropLead, boolean dropAnchor) {
		if (cleatIndex < 0 || cleatIndex >= CLEAT_COUNT) {
			return;
		}
		Entity holder = this.getCleatHolderEntity(cleatIndex);

		this.cleatHolderUuids[cleatIndex] = null;
		this.cleatHolderEntities[cleatIndex] = null;
		this.cleatHolderPositions[cleatIndex] = null;
		this.entityData.set(CLEAT_HOLDERS[cleatIndex], -1);
		this.entityData.set(CLEAT_POS[cleatIndex], Optional.empty());

		if (!this.level().isClientSide()) {
			Vec3 pos = this.getCleatWorldPos(cleatIndex);

			if (holder instanceof AnchorEntity anchor) {
				if (dropAnchor) {
					anchor.dropLeash();
					anchor.discard();
					this.spawnAtLocation((ServerLevel) this.level(), new ItemStack(WindAndSailsMod.ANCHOR_ITEM));
				}
			} else if (holder instanceof LeashFenceKnotEntity knot) {
				knot.notifyLeasheeRemoved(this);
			}

			if (dropLead) {
				this.spawnAtLocation((ServerLevel) this.level(), new ItemStack(Items.LEAD));
			}

			this.level().playSound(null, pos.x, pos.y, pos.z,
				SoundEvents.LEAD_UNTIED, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		boolean anyTied = false;
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.isCleatAttached(i)) {
				anyTied = true;
				break;
			}
		}
		if (!anyTied) {
			this.leashData = null;
		}
	}

	public static String getCleatName(int cleatIndex) {
		return switch (cleatIndex) {
			case CLEAT_BOW_PORT -> "Dziób lewa burta (Port Bow)";
			case CLEAT_BOW_STARBOARD -> "Dziób prawa burta (Starboard Bow)";
			case CLEAT_STERN_PORT -> "Rufa lewa burta (Port Stern)";
			case CLEAT_STERN_STARBOARD -> "Rufa prawa burta (Starboard Stern)";
			default -> "Knaga " + cleatIndex;
		};
	}

	public void deployAnchor(Player player) {
		if (this.level().isClientSide()) {
			return;
		}
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		Vec3 spawnPos = this.position().add(-Mth.sin(yawRad) * 2.2, 0.1, Mth.cos(yawRad) * 2.2);

		AnchorEntity anchor = new AnchorEntity(WindAndSailsMod.ANCHOR_ENTITY, this.level());
		anchor.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
		anchor.setDeltaMovement(-Mth.sin(yawRad) * 0.12, -0.18, Mth.cos(yawRad) * 0.12);
		this.level().addFreshEntity(anchor);

		anchor.setLeashedTo(player, true);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));

		this.level().playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
			SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 0.9F);
		this.level().playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
			SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);

		player.displayClientMessage(
			Component.literal("§6[Kotwica]§r Kotwica wrzucona do wody i opada na dno! Kliknij PPM na wybraną knagę, by ją przywiązać."),
			true
		);
	}

	/** Fixed mooring lines (anchor or fence post) never grow past this length. */
	private static final double MAX_MOORING_LEAD = 3.0;

	private boolean isFixedMooring(CleatMooring mooring) {
		return !(mooring.holder() instanceof Player player && player != this.getFirstPassenger());
	}

	/**
	 * Yaw toward the actual posts. Each cleat swings onto its own line, so a stern tie
	 * puts the stern on the post and a diagonal tie leaves the hull lying along that line.
	 */
	private float lineYawStep(List<CleatMooring> moorings, float windStrength, float helmScale) {
		float sum = 0.0F;
		float weight = 0.0F;
		for (CleatMooring mooring : moorings) {
			Vec3 local = this.getCleatLocalOffset(mooring.cleatIndex());
			Vec3 holder = this.liveHolderPos(mooring);
			Vec3 cleat = this.getCleatWorldPos(mooring.cleatIndex());
			double dist = Math.hypot(cleat.x - holder.x, cleat.z - holder.z);
			float w = MooringConstraint.lineYawWeight(dist, MAX_MOORING_LEAD);
			float error = MooringConstraint.cleatYawError(
				this.getYRot(), this.getX(), this.getZ(), local.x, local.z, holder.x, holder.z);
			sum += error * w;
			weight += w;
		}
		if (weight <= 1.0E-4F) {
			return 0.0F;
		}
		float error = sum / weight;
		if (Math.abs(error) < 6.0F) {
			return 0.0F;
		}
		float windFactor = Mth.clamp(windStrength / 0.35F, 0.2F, 1.5F);
		return Mth.clamp(error * 0.012F * windFactor * helmScale, -1.2F, 1.2F);
	}

	/** How far the lines would be past their length if the hull had this yaw, center unchanged. */
	private float lineExcess(List<CleatMooring> moorings, float yaw) {
		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		double excess = 0.0;
		for (CleatMooring mooring : moorings) {
			Vec3 local = this.getCleatLocalOffset(mooring.cleatIndex());
			double cleatX = this.getX() + rightX * local.x + fwdX * local.z;
			double cleatZ = this.getZ() + rightZ * local.x + fwdZ * local.z;
			Vec3 holder = this.liveHolderPos(mooring);
			double over = Math.hypot(cleatX - holder.x, cleatZ - holder.z) - MAX_MOORING_LEAD;
			if (over > 0.0) {
				excess += over;
			}
		}
		return (float) excess;
	}

	/**
	 * Slack may drift. A taut line loses only the outward motion, so it cannot stretch.
	 */
	private void constrainToLeadSlack(CleatMooring mooring) {
		Vec3 holderPos = this.liveHolderPos(mooring);
		Vec3 cleat = this.getCleatWorldPos(mooring.cleatIndex());
		double dx = cleat.x - holderPos.x;
		double dz = cleat.z - holderPos.z;
		double dist = Math.hypot(dx, dz);
		if (dist <= 1.0E-4 || dist <= MAX_MOORING_LEAD + 0.08) {
			return;
		}
		double nx = dx / dist;
		double nz = dz / dist;
		double over = dist - MAX_MOORING_LEAD;
		double pullX = -nx * over;
		double pullZ = -nz * over;
		// Fenders win over the line: do not drag the hull back onto the dock.
		if (this.hasPortDockFenders()) {
			double[] kept = this.dropDockwardPull(pullX, pullZ, -1);
			pullX = kept[0];
			pullZ = kept[1];
		}
		if (this.hasStarboardDockFenders()) {
			double[] kept = this.dropDockwardPull(pullX, pullZ, 1);
			pullX = kept[0];
			pullZ = kept[1];
		}
		double nextX = this.getX() + pullX;
		double nextZ = this.getZ() + pullZ;
		if (!this.isHullColliding(nextX, this.getY(), nextZ, this.getYRot())) {
			this.setPos(nextX, this.getY(), nextZ);
			this.updateBoundingBox();
		}
		this.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(this.getDeltaMovement(), nx, nz));
	}

	private Vec3 liveHolderPos(CleatMooring mooring) {
		Optional<BlockPos> knot = this.getCleatKnotPos(mooring.cleatIndex());
		if (knot.isPresent()) {
			BlockPos kp = knot.get();
			return new Vec3(kp.getX() + 0.5, kp.getY() + 0.7, kp.getZ() + 0.5);
		}
		Entity holder = mooring.holder();
		if (holder != null && holder != this && holder.isAlive()) {
			return holder.getRopeHoldPosition(1.0F);
		}
		return mooring.holderPos();
	}

	/** Lead from a cleat on one boat to another boat. Slack may drift; the line must not lengthen. */
	public void enforceInextensibleLeads() {
		for (int i = 0; i < CLEAT_COUNT; i++) {
			Entity holder = this.getCleatHolderEntity(i);
			if (!isWatercraft(holder)) {
				continue;
			}
			Vec3 cleat = this.getCleatWorldPos(i);
			Vec3 end = holder.getRopeHoldPosition(1.0F);
			double dx = cleat.x - end.x;
			double dz = cleat.z - end.z;
			double dist = Math.hypot(dx, dz);
			if (dist <= MAX_MOORING_LEAD || dist <= 1.0E-4) {
				continue;
			}
			boolean holderFixed = holder instanceof SailboatEntity other && other.hasFixedMooring();
			double nx = dx / dist;
			double nz = dz / dist;
			double over = dist - MAX_MOORING_LEAD;
			double share = holderFixed ? 1.0 : 0.5;
			double mx = nx * over * share;
			double mz = nz * over * share;
			double nextX = this.getX() - mx;
			double nextZ = this.getZ() - mz;
			if (!this.isHullColliding(nextX, this.getY(), nextZ, this.getYRot())) {
				this.setPos(nextX, this.getY(), nextZ);
			}
			if (!holderFixed) {
				double holderX = holder.getX() + mx;
				double holderZ = holder.getZ() + mz;
				if (!(holder instanceof SailboatEntity otherBoat)
					|| !otherBoat.isHullColliding(holderX, otherBoat.getY(), holderZ, otherBoat.getYRot())) {
					holder.setPos(holderX, holder.getY(), holderZ);
				}
				cancelOutwardDrift(holder, -nx, -nz);
			}
			this.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(this.getDeltaMovement(), nx, nz));
			this.needsSync = true;
		}
	}

	private static void cancelOutwardDrift(Entity entity, double nx, double nz) {
		entity.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(entity.getDeltaMovement(), nx, nz));
	}

	public boolean hasFixedMooring() {
		for (int i = 0; i < CLEAT_COUNT; i++) {
			if (this.getCleatKnotPos(i).isPresent()) {
				return true;
			}
			Entity holder = this.getCleatHolderEntity(i);
			if (holder != null && !(holder instanceof Player)) {
				return true;
			}
		}
		return false;
	}

	private static final Map<UUID, Long> MOORING_CLICK_TICK = new ConcurrentHashMap<>();

	/**
	 * Right-click acts only on the cleat wire or fence post under the crosshair.
	 * A reported neighbor is ignored.
	 */
	public static void handleMooringClick(Player player, boolean cleatClick, int boatId, int cleatIndex, BlockPos fence) {
		if (player.level().isClientSide() || mooringAlreadyHandled(player)) {
			return;
		}
		MooringAim.Target aim = MooringAim.find(player, player.level(), 1.0F);
		if (aim == null) {
			aim = MooringAim.confirm(player, cleatClick, boatId, cleatIndex, fence);
		}
		if (aim == null || !shouldOwnMooringClick(player, aim)) {
			return;
		}
		markMooringHandled(player);
		applyAimedMooring(player, aim);
	}

	/**
	 * Server consumes the click when a cleat wire or fence post is the looked-at target,
	 * so a neighboring boat, rail or post cannot take it.
	 * On the client this returns false so the explicit mooring packet is still sent.
	 */
	public static boolean handleAimedMooringClick(Player player) {
		if (mooringAlreadyHandled(player)) {
			return true;
		}
		MooringAim.Target aim = MooringAim.find(player, player.level(), 1.0F);
		if (aim == null || !shouldOwnMooringClick(player, aim)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return false;
		}
		markMooringHandled(player);
		applyAimedMooring(player, aim);
		return true;
	}

	public static boolean shouldOwnMooringClick(Player player, MooringAim.Target aim) {
		if (aim == null) {
			return false;
		}
		if (holdsLead(player) || findCleatHeldBy(player) != null) {
			return true;
		}
		if (!aim.cleat() && hasVanillaBoatLeashed(player)) {
			return true;
		}
		if (aim.cleat() && AnchorEntity.findHeldBy(player) != null) {
			return true;
		}
		if (aim.cleat() && WindAndSailsMod.getPendingFenceMooring(player.getUUID()) != null) {
			return true;
		}
		if (aim.cleat()) {
			return player.level().getEntity(aim.boatId()) instanceof SailboatEntity boat && boat.isCleatAttached(aim.cleatIndex());
		}
		return fenceHasMooring(player, aim.fence());
	}

	private static boolean mooringAlreadyHandled(Player player) {
		return Long.valueOf(player.level().getGameTime()).equals(MOORING_CLICK_TICK.get(player.getUUID()));
	}

	private static void markMooringHandled(Player player) {
		MOORING_CLICK_TICK.put(player.getUUID(), player.level().getGameTime());
	}

	private static void applyAimedMooring(Player player, MooringAim.Target aim) {
		if (aim.cleat()) {
			if (player.level().getEntity(aim.boatId()) instanceof SailboatEntity boat) {
				interactHighlightedCleat(player, boat, aim.cleatIndex());
			}
			return;
		}
		if (aim.fence() != null) {
			interactHighlightedFence(player, aim.fence());
		}
	}

	private static boolean holdsLead(Player player) {
		return player.getMainHandItem().is(Items.LEAD) || player.getOffhandItem().is(Items.LEAD);
	}

	private static boolean fenceHasMooring(Player player, BlockPos fence) {
		if (fence == null) {
			return false;
		}
		AABB area = player.getBoundingBox().inflate(MooringAim.REACH + 4.0);
		for (SailboatEntity boat : player.level().getEntitiesOfClass(SailboatEntity.class, area)) {
			for (int i = 0; i < CLEAT_COUNT; i++) {
				if (isCleatTiedToFence(boat, i, fence)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean interactHighlightedCleat(Player player, SailboatEntity boat, int cleat) {
		String name = getCleatName(cleat);
		if (boat.isCleatAttached(cleat)) {
			if (!player.level().isClientSide()) {
				boat.detachCleat(cleat, true);
				player.displayClientMessage(Component.literal("§e[Cumowanie]§r Odwiązano cumę z knagi: §f" + name + "§r."), true);
			}
			return true;
		}
		AnchorEntity thrown = AnchorEntity.findHeldBy(player);
		if (thrown != null) {
			if (!player.level().isClientSide()) {
				thrown.removeLeash();
				boat.attachCleat(cleat, thrown);
				shrinkLead(player);
				player.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(),
					SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
				player.displayClientMessage(Component.literal("§6[Kotwica]§r Kotwica przywiązana do knagi: §f" + name + "§r."), true);
			}
			return true;
		}
		BlockPos pending = WindAndSailsMod.getPendingFenceMooring(player.getUUID());
		if (pending != null && player.level().getBlockState(pending).is(BlockTags.FENCES)) {
			if (!player.level().isClientSide()) {
				WindAndSailsMod.clearPendingFenceMooring(player.getUUID());
				LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot((ServerLevel) player.level(), pending);
				boat.attachCleat(cleat, knot);
				shrinkLead(player);
				knot.playPlacementSound();
				player.displayClientMessage(Component.literal("§a[Cumowanie]§r Przywiązano cumę ze słupka do knagi: §f" + name + "§r!"), true);
			}
			return true;
		}
		List<Leashable> leashed = Leashable.leashableLeashedTo(player);
		if (!leashed.isEmpty() && leashed.get(0) instanceof Entity entity) {
			if (!player.level().isClientSide()) {
				((Leashable) entity).removeLeash();
				boat.attachCleat(cleat, entity);
				shrinkLead(player);
				player.displayClientMessage(Component.literal("§a[Cumowanie]§r Przywiązano cumę do knagi: §f" + name + "§r!"), true);
			}
			return true;
		}
		if (player.getMainHandItem().is(Items.LEAD) || player.getOffhandItem().is(Items.LEAD)) {
			if (!player.level().isClientSide()) {
				boat.attachCleat(cleat, player);
				player.displayClientMessage(Component.literal("§b[Cumowanie]§r Wybrano knagę: §f" + name + "§r. Teraz kliknij PPM na zaznaczony słupek."), true);
			}
			return true;
		}
		return false;
	}

	private static boolean interactHighlightedFence(Player player, BlockPos fence) {
		AABB area = player.getBoundingBox().inflate(MooringAim.REACH + 4.0);
		for (SailboatEntity boat : player.level().getEntitiesOfClass(SailboatEntity.class, area)) {
			for (int i = 0; i < CLEAT_COUNT; i++) {
				if (!isCleatTiedToFence(boat, i, fence)) {
					continue;
				}
				if (!player.level().isClientSide()) {
					boat.detachCleat(i, true);
					player.displayClientMessage(Component.literal("§e[Cumowanie]§r Odwiązano cumę ze słupka: §f" + getCleatName(i) + "§r."), true);
				}
				return true;
			}
		}
		if (AnchorEntity.findHeldBy(player) != null && findCleatHeldBy(player) == null && !hasVanillaBoatLeashed(player)) {
			if (!player.level().isClientSide()) {
				player.displayClientMessage(Component.literal("§6[Kotwica]§r Kliknij PPM na zaznaczoną knagę, aby przywiązać kotwicę."), true);
			}
			return true;
		}
		boolean vanillaTied = bindVanillaBoats(player, fence);
		HeldCleat held = findCleatHeldBy(player);
		if (held != null) {
			if (!player.level().isClientSide()) {
				LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot((ServerLevel) player.level(), fence);
				held.boat().attachCleat(held.index(), knot);
				knot.playPlacementSound();
				player.displayClientMessage(Component.literal("§a[Cumowanie]§r Cumę (" + getCleatName(held.index()) + ") przywiązano do słupka!"), true);
			}
			return true;
		}
		if (vanillaTied) {
			return true;
		}
		if (player.getMainHandItem().is(Items.LEAD) || player.getOffhandItem().is(Items.LEAD)) {
			if (!player.level().isClientSide()) {
				LeashFenceKnotEntity.getOrCreateKnot((ServerLevel) player.level(), fence).playPlacementSound();
				WindAndSailsMod.setPendingFenceMooring(player.getUUID(), fence);
				player.displayClientMessage(Component.literal("§b[Cumowanie]§r Wybrano słupek. Kliknij PPM na zaznaczoną knagę."), true);
			}
			return true;
		}
		return false;
	}

	private static boolean hasVanillaBoatLeashed(Player player) {
		for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
			if (leashable instanceof AbstractBoat) {
				return true;
			}
		}
		return false;
	}

	/** Moves a vanilla boat's leash from the player onto the dock knot, so the rope is actually drawn. */
	private static boolean bindVanillaBoats(Player player, BlockPos fence) {
		List<AbstractBoat> boats = new ArrayList<>();
		for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
			if (leashable instanceof AbstractBoat boat) {
				boats.add(boat);
			}
		}
		if (boats.isEmpty()) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		boolean existed = MooringAim.getKnot(player.level(), fence).isPresent();
		LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot((ServerLevel) player.level(), fence);
		boolean any = false;
		boolean tooFar = false;
		for (AbstractBoat boat : boats) {
			if (!boat.canHaveALeashAttachedTo(knot)) {
				tooFar = true;
				continue;
			}
			boat.setLeashedTo(knot, true);
			any = true;
		}
		if (any) {
			knot.playPlacementSound();
			player.level().playSound(null, knot.getX(), knot.getY(), knot.getZ(),
				SoundEvents.LEAD_TIED, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.displayClientMessage(Component.literal("§a[Cumowanie]§r Przywiązano łódkę do słupka."), true);
			return true;
		}
		if (!existed) {
			knot.discard();
		}
		if (tooFar) {
			player.displayClientMessage(Component.literal("§e[Cumowanie]§r Łódka jest za daleko od słupka, żeby założyć linę."), true);
		}
		return false;
	}

	private static boolean isCleatTiedToFence(SailboatEntity boat, int cleat, BlockPos fence) {
		if (!boat.isCleatAttached(cleat) || fence == null) {
			return false;
		}
		Optional<BlockPos> knot = boat.getCleatKnotPos(cleat);
		if (knot.isPresent() && knot.get().equals(fence)) {
			return true;
		}
		Entity holder = boat.getCleatHolderEntity(cleat);
		return holder instanceof LeashFenceKnotEntity knotEntity && fence.equals(knotEntity.getPos());
	}

	private record HeldCleat(SailboatEntity boat, int index) {}

	private static HeldCleat findCleatHeldBy(Player player) {
		AABB area = player.getBoundingBox().inflate(24.0);
		for (SailboatEntity boat : player.level().getEntitiesOfClass(SailboatEntity.class, area)) {
			for (int i = 0; i < CLEAT_COUNT; i++) {
				Entity holder = boat.getCleatHolderEntity(i);
				if (holder != null && holder.getUUID().equals(player.getUUID())) {
					return new HeldCleat(boat, i);
				}
			}
		}
		return null;
	}

	private static void shrinkLead(Player player) {
		if (player.getAbilities().instabuild) {
			return;
		}
		if (player.getMainHandItem().is(Items.LEAD)) {
			player.getMainHandItem().shrink(1);
		} else if (player.getOffhandItem().is(Items.LEAD)) {
			player.getOffhandItem().shrink(1);
		}
	}

	public static boolean isLeadTarget(Entity entity) {
		if (entity == null || entity.level() == null) {
			return false;
		}
		AABB area = entity.getBoundingBox().inflate(16.0);
		for (SailboatEntity boat : entity.level().getEntitiesOfClass(SailboatEntity.class, area)) {
			if (boat != entity && boat.isCleatAttachedTo(entity)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isWatercraft(Entity entity) {
		return entity instanceof SailboatEntity || entity instanceof AbstractBoat;
	}

	public Vec3 computeShoreRepulsion() {
		Vec3 boatPos = this.position();
		Vec3 repulsion = Vec3.ZERO;
		double radius = 1.35;
		BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

		for (int deg = 0; deg < 360; deg += 45) {
			double rad = deg * Mth.DEG_TO_RAD;
			double px = boatPos.x + Math.cos(rad) * radius;
			double pz = boatPos.z + Math.sin(rad) * radius;
			mpos.set(Mth.floor(px), Mth.floor(boatPos.y + 0.2), Mth.floor(pz));

			BlockState state = this.level().getBlockState(mpos);
			if (!state.isAir() && !this.level().getFluidState(mpos).is(FluidTags.WATER) && state.isSolidRender()) {
				double pushX = -Math.cos(rad);
				double pushZ = -Math.sin(rad);
				repulsion = repulsion.add(pushX * 0.08, 0.0, pushZ * 0.08);
			}
		}
		return repulsion;
	}

	public record CleatMooring(int cleatIndex, Entity holder, Vec3 cleatPos, Vec3 holderPos) {}

	// Geometry & dimension hooks for boat variants
	protected double getHullMinFwd() { return -1.30; }
	protected double getHullMaxFwd() { return 1.40; }
	protected double getBowTaperStart() { return 0.70; }
	public double getPassengerCenterZOffset() { return -0.70; }
	public double getBowLeashOffsetZ() { return 1.45; }
	protected ItemStack getDropItemStack() {
		return new ItemStack(WindAndSailsMod.SAILBOAT_ITEM);
	}

	@Override
	public boolean isClientAuthoritative() {
		return false;
	}

	@Override
	protected boolean isLocalClientAuthoritative() {
		return false;
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		return null;
	}

	// Synched getters and setters
	private boolean rudderHeld;

	/** Client-only. While false, the tiller is drawn centered instead of easing back. */
	public void setRudderHeld(boolean held) {
		this.rudderHeld = held;
	}

	public void snapVisualRudder() {
		this.visualRudderAngleO = 0.0F;
		this.visualRudderAngle = 0.0F;
	}

	public float getRudderAngle() { return this.entityData.get(RUDDER_ANGLE); }
	public void setRudderAngle(float angle) { this.entityData.set(RUDDER_ANGLE, angle); }

	public float getMainsheet() { return this.entityData.get(MAINSHEET); }
	public void setMainsheet(float sheet) { this.entityData.set(MAINSHEET, sheet); }

	public float getBoomAngle() { return this.entityData.get(BOOM_ANGLE); }
	public void setBoomAngle(float angle) { this.entityData.set(BOOM_ANGLE, angle); }

	public float getHeelAngle() { return this.entityData.get(HEEL_ANGLE); }
	public void setHeelAngle(float angle) { this.entityData.set(HEEL_ANGLE, angle); }

	public int getHurtTime() { return this.entityData.get(HURT_TIME); }
	public void setHurtTime(int hurtTime) { this.entityData.set(HURT_TIME, hurtTime); }

	public float getDamage() { return this.entityData.get(DAMAGE); }
	public void setDamage(float damage) { this.entityData.set(DAMAGE, damage); }

	public boolean isSailFurled() { return this.entityData.get(SAIL_FURLED); }
	public void setSailFurled(boolean furled) { this.entityData.set(SAIL_FURLED, furled); }
	public void toggleSailFurled() {
		boolean newState = !this.isSailFurled();
		this.setSailFurled(newState);
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
			net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS,
			1.0F, newState ? 0.85F : 1.15F);
	}

	public float getHike() { return this.entityData.get(HIKE); }
	public void setHike(float hike) { this.entityData.set(HIKE, hike); }

	public int getHikeMode() { return this.entityData.get(HIKE_MODE); }
	public void setHikeMode(int mode) { this.entityData.set(HIKE_MODE, (byte) Mth.clamp(mode, 0, 2)); }

	public boolean isHiking() { return this.getHikeMode() > HIKE_NONE; }
	public boolean isHikeSitting() { return this.getHikeMode() == HIKE_SIT; }
	public boolean isHikeStanding() { return this.getHikeMode() == HIKE_STAND; }

	public float getVisualBoomAngle(float partialTick) {
		return Mth.lerp(partialTick, this.visualBoomAngleO, this.visualBoomAngle);
	}

	public float getVisualHeelAngle(float partialTick) {
		return Mth.lerp(partialTick, this.visualHeelAngleO, this.visualHeelAngle);
	}

	public float getVisualRudderAngle(float partialTick) {
		return Mth.lerp(partialTick, this.visualRudderAngleO, this.visualRudderAngle);
	}
}
