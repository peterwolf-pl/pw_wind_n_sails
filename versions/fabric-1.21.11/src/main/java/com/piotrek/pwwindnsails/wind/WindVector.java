package com.piotrek.pwwindnsails.wind;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Representation of wind direction (degrees, Minecraft yaw standard) and strength.
 */
public record WindVector(float directionDeg, float strength) {
	/**
	 * Converts direction and strength into a 3D velocity vector (X and Z components).
	 * Minecraft yaw convention:
	 * 0 deg   = South (+Z)
	 * 90 deg  = West  (-X)
	 * 180 deg = North (-Z)
	 * 270 deg = East  (+X)
	 */
	public Vec3 toVec3() {
		float rad = this.directionDeg * Mth.DEG_TO_RAD;
		double x = -Mth.sin(rad) * this.strength;
		double z = Mth.cos(rad) * this.strength;
		return new Vec3(x, 0.0, z);
	}

	public double x() {
		return -Mth.sin(this.directionDeg * Mth.DEG_TO_RAD) * this.strength;
	}

	public double z() {
		return Mth.cos(this.directionDeg * Mth.DEG_TO_RAD) * this.strength;
	}

	public static WindVector fromVec(double x, double z) {
		float strength = (float) Math.sqrt(x * x + z * z);
		if (strength < 1e-4F) {
			return new WindVector(0.0F, 0.0F);
		}
		float deg = (float) (Mth.atan2(-x, z) * Mth.RAD_TO_DEG);
		if (deg < 0.0F) {
			deg += 360.0F;
		}
		return new WindVector(deg, strength);
	}
}
