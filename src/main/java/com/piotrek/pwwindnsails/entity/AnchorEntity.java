package com.piotrek.pwwindnsails.entity;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AnchorEntity extends Entity implements Leashable {
	private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);

	@Nullable
	private Leashable.LeashData leashData;
	private boolean hitSeabedSoundPlayed = false;
	private boolean wasInWater = false;

	public AnchorEntity(EntityType<? extends AnchorEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
	}

	@Override
	protected InterpolationHandler createInterpolationHandler() {
		return LinearInterpolationHandler.create(this, 3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ANCHORED, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setAnchored(input.getBooleanOr("Anchored", false));
		this.readLeashData(input);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("Anchored", this.isAnchored());
		this.writeLeashData(output, this.leashData);
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (!this.level().isClientSide()) {
			// Retrieve anchor: pick up back to inventory or drop
			ItemStack anchorStack = new ItemStack(WindAndSailsMod.ANCHOR_ITEM);
			if (!player.addItem(anchorStack)) {
				this.spawnAtLocation((ServerLevel) this.level(), anchorStack);
			}
			this.dropLeash();
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
			this.discard();
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		if (this.isInvulnerableToBase(damageSource)) {
			return false;
		}
		this.spawnAtLocation(level, new ItemStack(WindAndSailsMod.ANCHOR_ITEM));
		this.dropLeash();
		this.discard();
		return true;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide()) {
			this.getInterpolation().interpolate();
			if (!this.isAnchored() && this.isInWater() && this.random.nextFloat() < 0.4F) {
				this.level().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.3, this.getZ(), 0.0, 0.05, 0.0);
			}
			return;
		}

		BlockPos currentPos = this.blockPosition();
		boolean inWater = this.isInWater() || this.level().getFluidState(currentPos).is(FluidTags.WATER);
		if (!this.wasInWater && inWater) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.GENERIC_SPLASH, SoundSource.AMBIENT, 0.9F, 1.0F);
		}
		this.wasInWater = inWater;

		BlockPos belowPos = currentPos.below();
		BlockState belowState = this.level().getBlockState(belowPos);
		boolean groundBelow = !belowState.isAir() && belowState.isSolidRender();

		if (this.onGround() || (groundBelow && this.getY() - belowPos.getY() <= 1.05)) {
			// Resting firmly on the seabed
			if (!this.isAnchored()) {
				this.setAnchored(true);
				if (!this.hitSeabedSoundPlayed) {
					this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
						SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
					this.hitSeabedSoundPlayed = true;
				}
			}
			this.setDeltaMovement(Vec3.ZERO);
		} else {
			this.setAnchored(false);
			Vec3 vel = this.getDeltaMovement();

			if (inWater) {
				// In water: hydrodynamic drag slows horizontal momentum and sinks steadily at -0.22 m/s
				double targetY = Mth.lerp(0.18, vel.y, -0.22);
				this.setDeltaMovement(vel.x * 0.72, targetY, vel.z * 0.72);
			} else {
				// In air: authentic ballistic flight with gravity (speed 0.46 at 45 deg up gives ~6 blocks)
				double newY = vel.y - 0.04;
				this.setDeltaMovement(vel.x * 0.99, newY, vel.z * 0.99);
			}
			this.move(MoverType.SELF, this.getDeltaMovement());
		}
	}

	/** Anchor still on the line in the player's hand, waiting for a cleat click. */
	public static AnchorEntity findHeldBy(Player player) {
		if (player == null || player.level() == null) {
			return null;
		}
		AABB area = player.getBoundingBox().inflate(24.0);
		AnchorEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (AnchorEntity anchor : player.level().getEntitiesOfClass(AnchorEntity.class, area)) {
			Entity holder = anchor.getLeashHolder();
			if (holder == null || !holder.getUUID().equals(player.getUUID())) {
				continue;
			}
			double dist = anchor.distanceToSqr(player);
			if (dist < best) {
				best = dist;
				nearest = anchor;
			}
		}
		return nearest;
	}

	public boolean isAnchored() {
		return this.entityData.get(ANCHORED);
	}

	public void setAnchored(boolean anchored) {
		this.entityData.set(ANCHORED, anchored);
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
		// Shackle ring at top of anchor
		return new Vec3(0.0, 0.65, 0.0);
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return new Vec3(0.0, 0.65, 0.0);
	}

	@Override
	public boolean supportQuadLeash() {
		return false;
	}
}
