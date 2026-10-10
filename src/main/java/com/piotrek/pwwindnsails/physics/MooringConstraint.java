package com.piotrek.pwwindnsails.physics;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Inextensible mooring lines. The tied cleat turns toward its post, even when the line is diagonal.
 * A taut line does not grow.
 */
public final class MooringConstraint {
	private MooringConstraint() {}

	/** Drops only the velocity component that would lengthen the line. */
	public static Vec3 cancelOutwardVelocity(Vec3 velocity, double nx, double nz) {
		double outward = velocity.x * nx + velocity.z * nz;
		if (outward <= 0.0) {
			return velocity;
		}
		return new Vec3(velocity.x - nx * outward, velocity.y, velocity.z - nz * outward);
	}

	/**
	 * Signed yaw, in degrees, that swings this cleat toward its mooring point.
	 * Positive increases Minecraft yaw. A stern cleat therefore puts the stern on the line,
	 * not the bow. A diagonal post leaves the hull lying along that diagonal.
	 */
	public static float cleatYawError(
		float boatYawDeg,
		double boatX,
		double boatZ,
		double cleatLocalX,
		double cleatLocalZ,
		double holderX,
		double holderZ
	) {
		float yawRad = boatYawDeg * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		double cleatX = boatX + rightX * cleatLocalX + fwdX * cleatLocalZ;
		double cleatZ = boatZ + rightZ * cleatLocalX + fwdZ * cleatLocalZ;
		double cx = cleatX - boatX;
		double cz = cleatZ - boatZ;
		double hx = holderX - boatX;
		double hz = holderZ - boatZ;
		if (cx * cx + cz * cz < 1.0E-6 || hx * hx + hz * hz < 1.0E-6) {
			return 0.0F;
		}
		double cross = cx * hz - cz * hx;
		double dot = cx * hx + cz * hz;
		return (float) (Math.atan2(cross, dot) * Mth.RAD_TO_DEG);
	}

	/** How hard this line may yaw the hull. Slack still aligns; a taut line aligns faster. */
	public static float lineYawWeight(double lineLength, double maxLead) {
		float taut = maxLead <= 1.0E-4 ? 1.0F : Mth.clamp((float) (lineLength / maxLead), 0.0F, 1.0F);
		return 0.35F + 0.65F * taut;
	}
}
