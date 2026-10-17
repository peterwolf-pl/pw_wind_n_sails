package com.piotrek.pwwindnsails.mixin;

import com.piotrek.pwwindnsails.entity.SailboatEntity;
import com.piotrek.pwwindnsails.physics.MooringConstraint;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
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
			boat.setDeltaMovement(boat.getDeltaMovement().add(windDir.x * drift, 0.0, windDir.z * drift));
		}
		if (boat.isLeashed()) {
			keepWithinLeashSlack(boat);
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
		boat.setPos(boat.getX() - nx * over, boat.getY(), boat.getZ() - nz * over);
		boat.setDeltaMovement(MooringConstraint.cancelOutwardVelocity(boat.getDeltaMovement(), nx, nz));
	}
}
