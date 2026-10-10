package com.piotrek.pwwindnsails.mixin;

import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.physics.MooringConstraint;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void pw$applyWindDrift(CallbackInfo ci) {
		AbstractBoat boat = (AbstractBoat) (Object) this;
		if (boat.level().isClientSide()) {
			return;
		}
		if (boat.level().getChunkSource().getChunkNow(boat.getBlockX() >> 4, boat.getBlockZ() >> 4) == null) {
			return;
		}
		// An unleashed boat used as someone else's mooring point stays put.
		// A leashed boat may still drift inside the slack of its own line.
		if (!boat.isLeashed() && SailboatEntity.isLeadTarget(boat)) {
			return;
		}

		boolean onOrNearWater = isFloatingInWater(boat);

		// If floating on water, prevent boat from stepping onto or penetrating dock/pier obstacles
		if (onOrNearWater) {
			separateBoatFromObstacles(boat);
		}

		if (boat.isUnderWater() || boat.getWaterLevelAbove() <= 0.0F) {
			return;
		}
		if (boat.getControllingPassenger() != null) {
			return;
		}

		WindVector wind = WindManager.getInstance().getWind(boat.level(), boat.position());
		if (wind != null && wind.strength() > 0.02F) {
			Vec3 windDir = wind.toVec3().normalize();
			double drift = wind.strength() * 0.0035;
			double driftX = windDir.x * drift;
			double driftZ = windDir.z * drift;

			// Check if wind drift would push the boat directly into a solid dock/shore obstacle
			AABB futureBb = boat.getBoundingBox().move(driftX * 3.0, 0.0, driftZ * 3.0);
			if (boat.level().noCollision(boat, futureBb)) {
				boat.setDeltaMovement(boat.getDeltaMovement().add(driftX, 0.0, driftZ));
			} else {
				// Deflect along the open axis instead of driving straight into the dock
				Vec3 cur = boat.getDeltaMovement();
				if (boat.level().noCollision(boat, boat.getBoundingBox().move(driftX * 3.0, 0.0, 0.0))) {
					boat.setDeltaMovement(cur.add(driftX, 0.0, 0.0));
				} else if (boat.level().noCollision(boat, boat.getBoundingBox().move(0.0, 0.0, driftZ * 3.0))) {
					boat.setDeltaMovement(cur.add(0.0, 0.0, driftZ));
				}
			}
		}

		if (boat.isLeashed()) {
			keepWithinLeashSlack(boat);
		}

		if (onOrNearWater) {
			separateBoatFromObstacles(boat);
		}
	}

	private static boolean isFloatingInWater(AbstractBoat boat) {
		if (boat.isUnderWater() || boat.getWaterLevelAbove() > 0.0F) {
			return true;
		}
		BlockPos pos = boat.blockPosition();
		return boat.level().getFluidState(pos).is(FluidTags.WATER) ||
		       boat.level().getFluidState(pos.below()).is(FluidTags.WATER);
	}

	private static void separateBoatFromObstacles(AbstractBoat boat) {
		if (boat.level().noCollision(boat, boat.getBoundingBox())) {
			return;
		}

		double bestDist = Double.MAX_VALUE;
		double bestDx = 0.0;
		double bestDz = 0.0;

		for (int dir = 0; dir < 16; dir++) {
			double ang = dir * (Math.PI * 2.0 / 16.0);
			double dirX = Math.cos(ang);
			double dirZ = Math.sin(ang);
			for (int step = 1; step <= 15; step++) {
				double dist = step * 0.06;
				double testX = boat.getX() + dirX * dist;
				double testZ = boat.getZ() + dirZ * dist;
				AABB testBox = boat.getBoundingBox().move(dirX * dist, 0.0, dirZ * dist);
				if (boat.level().noCollision(boat, testBox)) {
					BlockPos testPos = BlockPos.containing(testX, boat.getY(), testZ);
					if (boat.level().getFluidState(testPos).is(FluidTags.WATER) ||
					    boat.level().getFluidState(testPos.below()).is(FluidTags.WATER)) {
						if (dist < bestDist) {
							bestDist = dist;
							bestDx = dirX * dist;
							bestDz = dirZ * dist;
						}
					}
					break;
				}
			}
		}

		if (bestDist < Double.MAX_VALUE) {
			boat.setPos(boat.getX() + bestDx, boat.getY(), boat.getZ() + bestDz);
			double len = Math.hypot(bestDx, bestDz);
			if (len > 1.0E-4) {
				double nx = bestDx / len;
				double nz = bestDz / len;
				Vec3 vel = boat.getDeltaMovement();
				double inward = vel.x * (-nx) + vel.z * (-nz);
				if (inward > 0.0) {
					boat.setDeltaMovement(new Vec3(vel.x + nx * inward, vel.y, vel.z + nz * inward));
				}
			}
		}
	}

	private static void keepWithinLeashSlack(AbstractBoat boat) {
		Leashable leashable = (Leashable) boat;
		Entity holder = leashable.getLeashHolder();
		if (holder == null) {
			return;
		}
		double dx = boat.getX() - holder.getX();
		double dz = boat.getZ() - holder.getZ();
		double dist = Math.hypot(dx, dz);
		if (dist < 1.0E-4) {
			return;
		}
		double slack = leashable.leashElasticDistance() - holder.getBbWidth() * 0.5 - boat.getBbWidth() * 0.5;
		slack = Math.max(2.0, slack);
		if (dist <= slack) {
			return;
		}
		double nx = dx / dist;
		double nz = dz / dist;
		double over = dist - slack;

		// Move towards holder up to leash slack, but do not drag the boat onto a dock or shore
		double pullX = -nx * over;
		double pullZ = -nz * over;
		double safeFraction = findSafeStep(boat, pullX, pullZ);
		if (safeFraction > 1.0E-4) {
			boat.setPos(boat.getX() + pullX * safeFraction, boat.getY(), boat.getZ() + pullZ * safeFraction);
		}
		boat.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(boat.getDeltaMovement(), nx, nz));
	}

	private static double findSafeStep(AbstractBoat boat, double dx, double dz) {
		AABB currentBb = boat.getBoundingBox();
		AABB targetBb = currentBb.move(dx, 0.0, dz);
		if (boat.level().noCollision(boat, targetBb)) {
			return 1.0;
		}
		double low = 0.0;
		double high = 1.0;
		for (int i = 0; i < 6; i++) {
			double mid = (low + high) * 0.5;
			if (boat.level().noCollision(boat, currentBb.move(dx * mid, 0.0, dz * mid))) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return low;
	}
}
