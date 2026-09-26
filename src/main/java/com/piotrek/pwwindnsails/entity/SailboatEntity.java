package com.piotrek.pwwindnsails.entity;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.physics.SailingPhysics;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 3.0m x 1.5m single-mast sailing boat entity.
 * Controlled by rudder (A/D) and mainsail sheet (W/S). Server-authoritative physics.
 */
public class SailboatEntity extends Entity {
	private static final EntityDataAccessor<Float> RUDDER_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MAINSHEET = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> BOOM_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEEL_ANGLE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HIKE = SynchedEntityData.defineId(SailboatEntity.class, EntityDataSerializers.FLOAT);
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

	public SailboatEntity(EntityType<? extends SailboatEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected InterpolationHandler createInterpolationHandler() {
		return LinearInterpolationHandler.create(this, 3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(RUDDER_ANGLE, 0.0F);
		builder.define(MAINSHEET, 0.5F); // default to 50% sheet
		builder.define(BOOM_ANGLE, 0.0F);
		builder.define(HEEL_ANGLE, 0.0F);
		builder.define(HIKE, 0.0F);
		builder.define(SAIL_FURLED, false);
		builder.define(HURT_TIME, 0);
		builder.define(DAMAGE, 0.0F);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setRudderAngle(input.getFloatOr("RudderAngle", 0.0F));
		this.setMainsheet(input.getFloatOr("Mainsheet", 0.5F));
		this.setBoomAngle(input.getFloatOr("BoomAngle", 0.0F));
		this.setHeelAngle(input.getFloatOr("HeelAngle", 0.0F));
		this.setHike(input.getFloatOr("Hike", 0.0F));
		this.setSailFurled(input.getBooleanOr("SailFurled", false));
		this.setDamage(input.getFloatOr("Damage", 0.0F));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("RudderAngle", this.getRudderAngle());
		output.putFloat("Mainsheet", this.getMainsheet());
		output.putFloat("BoomAngle", this.getBoomAngle());
		output.putFloat("HeelAngle", this.getHeelAngle());
		output.putFloat("Hike", this.getHike());
		output.putBoolean("SailFurled", this.isSailFurled());
		output.putFloat("Damage", this.getDamage());
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
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (this.hasPassenger(player)) {
			// Player is seated in the boat: right click (on mast or cockpit) toggles furling/unfurling!
			if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
				this.toggleSailFurled();
			}
			return InteractionResult.SUCCESS;
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
			this.spawnAtLocation(level, new ItemStack(WindAndSailsMod.SAILBOAT_ITEM));
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
		this.fitBoundingBoxToHull();

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
			return;
		}

		// Server side logic
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

		SailingPhysics.PhysicsResult physics = SailingPhysics.step(
			velWithY,
			this.getYRot(),
			this.yawVelocity,
			currentRudder,
			currentSheet,
			trueWind,
			this.getHeelAngle(),
			this.isSailFurled(),
			inWater
		);

		this.yawVelocity = physics.newYawVelocity();
		this.setYRot(physics.newYaw());
		this.setDeltaMovement(physics.newVelocity());

		// Apply synched data
		this.setRudderAngle(currentRudder);
		this.setMainsheet(currentSheet);
		this.setBoomAngle(physics.boomAngleDeg());
		this.setHeelAngle(physics.heelAngleDeg());

		this.move(MoverType.SELF, this.getDeltaMovement());
		this.fitBoundingBoxToHull();
		this.liftOutOfShore();
		this.fitBoundingBoxToHull();
		this.needsSync = true;
	}

	/**
	 * Axis-aligned box that contains the 3.0 x 1.5 hull at the current yaw.
	 * The registered size is only 1.5 square, which let the bow enter land by ~0.75 blocks.
	 */
	private void fitBoundingBoxToHull() {
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		double halfLength = WindAndSailsConfig.BOAT_LENGTH * 0.5;
		double halfWidth = WindAndSailsConfig.BOAT_WIDTH * 0.5;
		double extX = Math.abs(fwdX) * halfLength + Math.abs(rightX) * halfWidth;
		double extZ = Math.abs(fwdZ) * halfLength + Math.abs(rightZ) * halfWidth;
		double y = this.getY();
		this.setBoundingBox(new AABB(
			this.getX() - extX, y, this.getZ() - extZ,
			this.getX() + extX, y + 0.6, this.getZ() + extZ
		));
	}

	/** If movement left the hull inside a shore block, step up by at most half a block. */
	private void liftOutOfShore() {
		AABB box = this.getBoundingBox();
		if (this.level().noCollision(this, box)) {
			return;
		}
		for (int i = 1; i <= 10; i++) {
			double step = i * 0.05;
			if (this.level().noCollision(this, box.move(0.0, step, 0.0))) {
				this.setPos(this.getX(), this.getY() + step, this.getZ());
				Vec3 vel = this.getDeltaMovement();
				this.setDeltaMovement(vel.x, 0.0, vel.z);
				return;
			}
		}
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		// Passenger sits on the aft bench, above the dry cockpit sole
		float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
		double xOffset = -Mth.sin(yawRad) * (-0.70);
		double zOffset = Mth.cos(yawRad) * (-0.70);
		return new Vec3(xOffset, 0.42, zOffset);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		Vec3 right = new Vec3(Mth.cos(this.getYRot() * Mth.DEG_TO_RAD) * 1.5, 0, Mth.sin(this.getYRot() * Mth.DEG_TO_RAD) * 1.5);
		return this.position().add(right);
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
