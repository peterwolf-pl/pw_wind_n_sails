package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SailingPhysicsTest {

	@Test
	public void testNoGoZoneInIrons() {
		// Wind blowing South (from North = 180 deg). Boat heading North (yaw = 180 deg).
		// Wind vector blows towards South: yaw 0 (x=0, z=1).
		WindVector windTowardsSouth = new WindVector(0.0F, 0.6F);
		Vec3 stationary = Vec3.ZERO;
		float boatYaw = 180.0F; // Heading North (directly into wind)

		SailingPhysics.PhysicsResult res = SailingPhysics.step(
			stationary, boatYaw, 0.0F, 0.0F, 0.0F, windTowardsSouth, 0.0F, true
		);

		assertTrue(res.inIrons(), "Boat heading directly into wind must be in irons");
		// Speed along heading (North = -Z) must not be positive
		assertTrue(res.newVelocity().z >= 0, "Boat in irons must not accelerate forward into wind");
	}

	@Test
	public void testBeamReachPropulsion() {
		// Wind blowing towards East (yaw = 270 deg). Direction FROM is West (90 deg).
		WindVector windEast = new WindVector(270.0F, 0.65F);
		Vec3 initialVel = new Vec3(0, 0, 0.05); // slight headway South (yaw = 0 deg)
		float boatYaw = 0.0F; // Heading South (90 deg beam reach from West wind)

		// Trim sheet to ~0.4 (optimal for beam reach)
		SailingPhysics.PhysicsResult resTrimmed = SailingPhysics.step(
			initialVel, boatYaw, 0.0F, 0.0F, 0.4F, windEast, 0.0F, true
		);

		assertFalse(resTrimmed.inIrons(), "Beam reach must not be in irons");
		assertTrue(resTrimmed.newVelocity().z > initialVel.z, "Boat on beam reach must accelerate forward (+Z)");
		assertTrue(Math.abs(resTrimmed.heelAngleDeg()) > 0.02F, "Boat on beam reach must heel under sail pressure");
	}

	@Test
	public void testTrimDifference() {
		// On a reach, well trimmed sheet should provide greater forward acceleration than sheet pulled dead tight (0.0)
		WindVector windEast = new WindVector(270.0F, 0.65F);
		Vec3 initialVel = new Vec3(0, 0, 0.15);
		float boatYaw = 0.0F;

		SailingPhysics.PhysicsResult wellTrimmed = SailingPhysics.step(
			initialVel, boatYaw, 0.0F, 0.0F, 0.45F, windEast, 0.0F, true
		);

		SailingPhysics.PhysicsResult badTrimOverSheeted = SailingPhysics.step(
			initialVel, boatYaw, 0.0F, 0.0F, 0.0F, windEast, 0.0F, true
		);

		assertTrue(
			wellTrimmed.newVelocity().z > badTrimOverSheeted.newVelocity().z,
			"Well-trimmed sail must yield higher speed than over-sheeted sail"
		);
	}

	@Test
	public void testRudderAuthoritySpeedDependent() {
		WindVector calm = new WindVector(0.0F, 0.0F);

		// Case 1: Boat is stationary (speed = 0)
		SailingPhysics.PhysicsResult stationaryTurn = SailingPhysics.step(
			Vec3.ZERO, 0.0F, 0.0F, 30.0F, 0.5F, calm, 0.0F, true
		);

		// Case 2: Boat is moving forward fast (speed = 0.35)
		SailingPhysics.PhysicsResult fastTurn = SailingPhysics.step(
			new Vec3(0, 0, 0.35), 0.0F, 0.0F, 30.0F, 0.5F, calm, 0.0F, true
		);

		assertEquals(0.0F, stationaryTurn.newYawVelocity(), 1e-4F, "Stationary boat must have virtually zero rudder turning authority");
		assertTrue(Math.abs(fastTurn.newYawVelocity()) > 0.5F, "Moving boat must have significant rudder turning authority");
	}

	@Test
	public void testRunningDownwind() {
		// Wind blowing South (yaw 0). Boat heading South (yaw 0).
		WindVector windSouth = new WindVector(0.0F, 0.70F);
		Vec3 initialVel = new Vec3(0, 0, 0.05);

		// Fully eased sheet
		SailingPhysics.PhysicsResult downwind = SailingPhysics.step(
			initialVel, 0.0F, 0.0F, 0.0F, 1.0F, windSouth, 0.0F, true
		);

		assertFalse(downwind.inIrons(), "Running downwind is not in irons");
		assertTrue(downwind.newVelocity().z > initialVel.z, "Running downwind must accelerate forward");
	}

	@Test
	public void testTackingMomentum() {
		// Wind blowing South (yaw 0, coming from North 180 deg). Boat heading North (yaw 170 deg) entering no-go zone
		WindVector windSouth = new WindVector(0.0F, 0.60F);
		Vec3 momentum = new Vec3(0, 0, -0.30); // Fast forward motion towards North (-Z)

		// Turn rudder to tack through wind
		SailingPhysics.PhysicsResult tacking = SailingPhysics.step(
			momentum, 175.0F, 0.0F, 25.0F, 0.3F, windSouth, 0.0F, true
		);

		// The boat is momentarily in the no-go zone but carries forward momentum
		assertTrue(tacking.inIrons(), "Boat at 175 deg heading into 180 deg wind must be in irons");
		double speed = Math.sqrt(tacking.newVelocity().x * tacking.newVelocity().x + tacking.newVelocity().z * tacking.newVelocity().z);
		assertTrue(speed > 0.2, "Boat must carry momentum through the tack rather than stopping dead");
	}
}
