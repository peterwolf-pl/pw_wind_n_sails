package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Aerodynamic and hydrodynamic simulation for sailboats.
 * Pure server-authoritative physics model with apparent wind, sail lift/drag,
 * authentic mainsheet trimming, and realistic dynamic heel.
 *
 * Eased sail flutters freely along the wind line generating zero thrust.
 * Trimming the sheet pulls the sail to the centerline, generating drive and leeward heel.
 * Easing the sheet or luffing up into the wind immediately sheds heeling force and rights the hull.
 */
public final class SailingPhysics {
	private SailingPhysics() {}

	public record PhysicsResult(
		Vec3 newVelocity,
		float newYaw,
		float newYawVelocity,
		float boomAngleDeg,
		float heelAngleDeg,
		float apparentWindSpeed,
		float apparentWindAngleDeg,
		boolean inIrons
	) {}

	public static PhysicsResult step(
		Vec3 currentVelocity,
		float currentYaw,
		float yawVelocity,
		float rudderAngleDeg,
		float mainsheet,
		WindVector trueWind,
		float currentHeelDeg,
		boolean inWater
	) {
		return step(currentVelocity, currentYaw, yawVelocity, rudderAngleDeg, mainsheet, trueWind, currentHeelDeg, false, inWater);
	}

	/**
	 * Computes one physics tick for a sailboat.
	 */
	public static PhysicsResult step(
		Vec3 currentVelocity,
		float currentYaw,
		float yawVelocity,
		float rudderAngleDeg,
		float mainsheet,
		WindVector trueWind,
		float currentHeelDeg,
		boolean sailFurled,
		boolean inWater
	) {
		if (!inWater) {
			// In air or on land: apply gravity and ground friction
			Vec3 landVel = new Vec3(currentVelocity.x * 0.75, currentVelocity.y - 0.04, currentVelocity.z * 0.75);
			return new PhysicsResult(
				landVel,
				currentYaw,
				yawVelocity * 0.5F,
				0.0F,
				currentHeelDeg * 0.85F,
				trueWind.strength(),
				0.0F,
				false
			);
		}

		// 1. Boat forward heading unit vector
		float yawRad = currentYaw * Mth.DEG_TO_RAD;
		double headingX = -Mth.sin(yawRad);
		double headingZ = Mth.cos(yawRad);

		// Starboard (right) unit vector
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);

		// Decompose current horizontal velocity into longitudinal (forward) and lateral (sideways)
		double vx = currentVelocity.x;
		double vz = currentVelocity.z;
		double vForward = vx * headingX + vz * headingZ;
		double vLateral = vx * rightX + vz * rightZ;

		// Strong lateral resistance from keel/hull: eliminates leeway drift
		vLateral *= (1.0F - WindAndSailsConfig.LATERAL_SLIP_DAMPING);

		// 2. Apparent wind: W_app = W_true - V_boat
		double appWindX = trueWind.x() - vx;
		double appWindZ = trueWind.z() - vz;
		float appWindSpeed = (float) Math.sqrt(appWindX * appWindX + appWindZ * appWindZ);

		// Apparent wind direction (direction wind blows towards)
		float appWindTowardsDeg = (float) (Mth.atan2(-appWindX, appWindZ) * Mth.RAD_TO_DEG);
		// Direction FROM which wind blows
		float appWindFromDeg = WindAndSailsConfig.PREVAILING_WIND_DIR;
		if (appWindSpeed > 1e-4F) {
			appWindFromDeg = appWindTowardsDeg + 180.0F;
		}

		// Relative apparent wind angle to boat heading (-180 to +180)
		// 0 = dead into wind (headwind), +90 = starboard beam, -90 = port beam, 180 = dead downwind
		float relAppWindDeg = Mth.wrapDegrees(appWindFromDeg - currentYaw);
		float absRelWind = Math.abs(relAppWindDeg);
		float windSideSign = Math.signum(relAppWindDeg);
		if (Math.abs(windSideSign) < 1e-3F) {
			windSideSign = 1.0F;
		}

		// 3. Mainsheet limit and Boom Angle
		float clampedSheet = Mth.clamp(mainsheet, 0.0F, 1.0F);
		float maxBoomAngle = Mth.lerp(clampedSheet, WindAndSailsConfig.MIN_BOOM_ANGLE_DEG, WindAndSailsConfig.MAX_BOOM_ANGLE_DEG);

		// The boom is blown freely out towards the apparent wind line (up to physical rig limit ~88 deg).
		// But is restrained and pulled inward towards the boat's centerline by the mainsheet:
		float freeWindBoomAngle = Math.min(WindAndSailsConfig.MAX_BOOM_ANGLE_DEG, absRelWind);
		float actualBoomMag = Math.min(maxBoomAngle, freeWindBoomAngle);

		// Signed boom angle: negative if wind from starboard, positive if wind from port (leeward)
		float boomAngleDeg = -windSideSign * actualBoomMag;

		// 4. No-go zone check (In Irons)
		boolean inIrons = absRelWind < WindAndSailsConfig.NO_GO_ZONE_DEG;

		// 5. Aerodynamic sail forces
		double forwardThrust = 0.0;
		double lateralAeroForce = 0.0;

		if (sailFurled) {
			// Sail is furled: zero aerodynamic thrust and zero heeling force, boom centered
			boomAngleDeg = 0.0F;
		} else if (appWindSpeed > 1e-3F) {
			// Dynamic aerodynamic pressure
			double dynamicPressure = 0.5 * 1.225 * appWindSpeed * appWindSpeed * WindAndSailsConfig.SAIL_AREA * WindAndSailsConfig.SAIL_FORCE_SCALE;

			if (inIrons) {
				// In irons: sail luffs (flaps along centerline), producing zero forward lift
				// and only minimal headwind drag (sail is edge-on to wind)
				double headDrag = 0.04 * dynamicPressure;
				forwardThrust = -headDrag;
			} else {
				// Angle of attack: difference between apparent wind line and trimmed boom angle
				float aoaDeg = absRelWind - actualBoomMag;

				// Lift coefficient curve
				// When sheet is fully eased, aoaDeg is ~0, so lift is 0.
				// As sheet is trimmed (W), aoaDeg increases, developing lift up to peak at ~15 deg.
				float cl = 0.0F;
				if (aoaDeg > 0.8F) {
					float x = aoaDeg / WindAndSailsConfig.OPTIMAL_AOA_DEG;
					if (x <= 1.0F) {
						cl = WindAndSailsConfig.LIFT_COEFF_PEAK * (2.0F * x - x * x);
					} else {
						// Over-sheeted stall: flow separates, lift decreases
						cl = (float) (WindAndSailsConfig.LIFT_COEFF_PEAK * Math.exp(-0.75 * (x - 1.0F)));
					}
				}

				// Drag coefficient: profile drag + induced drag + downwind broadside drag
				float cdInd = 0.16F * cl * cl;
				float downwindRatio = absRelWind / 180.0F;
				float cdDownwind = WindAndSailsConfig.DRAG_COEFF_DOWNWIND * downwindRatio * downwindRatio
					* (float) Math.pow(Math.sin(actualBoomMag * Mth.DEG_TO_RAD), 2.0);
				float cd = WindAndSailsConfig.DRAG_COEFF_MIN + cdInd + cdDownwind;

				double lift = cl * dynamicPressure;
				double drag = cd * dynamicPressure;

				// Decompose into forward drive and lateral force
				float relRad = absRelWind * Mth.DEG_TO_RAD;
				double drive = lift * Mth.sin(relRad) - drag * Mth.cos(relRad);
				double side = lift * Mth.cos(relRad) + drag * Mth.sin(relRad);

				forwardThrust = drive;
				lateralAeroForce = windSideSign * side;
			}
		}

		// 6. Water drag on forward motion
		double forwardSpeed = vForward;
		double linearDrag = WindAndSailsConfig.WATER_LINEAR_DRAG * forwardSpeed;
		double quadDrag = WindAndSailsConfig.WATER_QUAD_DRAG * forwardSpeed * Math.abs(forwardSpeed);

		// Rudder induced drag (turning rudder slows the boat slightly)
		float rudderRad = rudderAngleDeg * Mth.DEG_TO_RAD;
		double rudderDrag = WindAndSailsConfig.rudderDragCoeff * Math.abs(Mth.sin(rudderRad)) * Math.abs(forwardSpeed);

		vForward += forwardThrust - (linearDrag + quadDrag + rudderDrag);

		// 7. Rudder torque (turning)
		// Torque is proportional to water speed over rudder: at 0 speed, rudder has almost no authority!
		double waterSpeed = Math.abs(vForward);
		float effectiveSpeedFactor = (float) Math.max(0.0, (waterSpeed - WindAndSailsConfig.rudderMinEffectiveSpeed) * 3.5);
		effectiveSpeedFactor = Math.min(1.5F, effectiveSpeedFactor);

		// Turning direction: negative rudder turns port (-yaw, left), positive turns starboard (+yaw, right)
		// Sensitivity multiplier from config / command:
		double rudderTorque = rudderAngleDeg * WindAndSailsConfig.rudderForceCoeff * WindAndSailsConfig.rudderSensitivity * effectiveSpeedFactor;
		if (vForward < -1e-3) {
			// Reversing water flow reverses rudder steering direction
			rudderTorque = -rudderTorque;
		}

		yawVelocity = (float) (yawVelocity * WindAndSailsConfig.YAW_ANGULAR_DAMPING + rudderTorque);
		float newYaw = currentYaw + yawVelocity;

		// 8. Recombine velocity from longitudinal and lateral components
		float newYawRad = newYaw * Mth.DEG_TO_RAD;
		double newHeadingX = -Mth.sin(newYawRad);
		double newHeadingZ = Mth.cos(newYawRad);
		double newRightX = Mth.cos(newYawRad);
		double newRightZ = Mth.sin(newYawRad);

		// 9. Heel (roll) angle
		// Lateral force from the sail heels the boat to leeward
		double targetHeel = -lateralAeroForce * WindAndSailsConfig.HEEL_SENSITIVITY;

		// If boat heels steeply (> 26 deg), the sail is angled away from vertical and spills wind
		if (Math.abs(targetHeel) > 26.0) {
			double spillFactor = Math.max(0.25, 1.0 - (Math.abs(targetHeel) - 26.0) * 0.12);
			forwardThrust *= spillFactor;
		}

		targetHeel = Mth.clamp(targetHeel, -WindAndSailsConfig.MAX_HEEL_DEG, WindAndSailsConfig.MAX_HEEL_DEG);
		float newHeel = (float) Mth.lerp(WindAndSailsConfig.HEEL_LERP_FACTOR, currentHeelDeg, targetHeel);

		double newVx = vForward * newHeadingX + vLateral * newRightX;
		double newVz = vForward * newHeadingZ + vLateral * newRightZ;

		return new PhysicsResult(
			new Vec3(newVx, currentVelocity.y, newVz),
			newYaw,
			yawVelocity,
			boomAngleDeg,
			newHeel,
			appWindSpeed,
			relAppWindDeg,
			inIrons
		);
	}
}
