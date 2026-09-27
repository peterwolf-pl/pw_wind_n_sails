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

	@Test
	public void testBoatSpeedScalingWithWindStrengthAndFlautaCalm() {
		// 1. In flauta calm (< 0.06), boat must not sail / accelerate forward
		WindVector flauta = new WindVector(270.0F, 0.03F); // West wind blowing East
		Vec3 rest = Vec3.ZERO;
		SailingPhysics.PhysicsResult flautaResult = SailingPhysics.step(
			rest, 0.0F, 0.0F, 0.0F, 0.5F, flauta, 0.0F, true
		);
		double flautaSpeed = Math.sqrt(flautaResult.newVelocity().x * flautaResult.newVelocity().x + flautaResult.newVelocity().z * flautaResult.newVelocity().z);
		assertEquals(0.0, flautaSpeed, 1e-4, "Boat must not move forward in flauta");

		// 2. Simulate 60 ticks of beam reach sailing under different wind speeds:
		// Light wind (0.20), Moderate wind (0.50), Strong wind (0.85)
		double speedLight = simulateBeamReachSpeed(0.20F, 60);
		double speedModerate = simulateBeamReachSpeed(0.50F, 60);
		double speedStrong = simulateBeamReachSpeed(0.85F, 60);

		assertTrue(speedLight > 0.05, "Boat must make headway in light wind: " + speedLight);
		assertTrue(speedModerate > speedLight * 1.4, "Boat must sail substantially faster in moderate wind: " + speedModerate + " vs " + speedLight);
		assertTrue(speedStrong > speedModerate * 1.2, "Boat must sail faster in strong wind: " + speedStrong + " vs " + speedModerate);
	}

	private double simulateBeamReachSpeed(float windStrength, int ticks) {
		WindVector wind = new WindVector(270.0F, windStrength);
		Vec3 vel = new Vec3(0, 0, 0.05); // slight initial headway
		for (int i = 0; i < ticks; i++) {
			SailingPhysics.PhysicsResult res = SailingPhysics.step(
				vel, 0.0F, 0.0F, 0.0F, 0.45F, wind, 0.0F, true
			);
			vel = res.newVelocity();
		}
		return Math.sqrt(vel.x * vel.x + vel.z * vel.z);
	}

	@Test
	public void testHeelDirectionOppositeToWind() {
		// Boat heading South (yaw 0).
		// Wind from West (90 deg, blows towards East 270 deg) -> Wind is on STARBOARD side
		WindVector windFromWest = new WindVector(270.0F, 0.70F);
		Vec3 headway = new Vec3(0, 0, 0.20);

		SailingPhysics.PhysicsResult resStarboardWind = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, windFromWest, 0.0F, false, true, false
		);

		// Wind on starboard must push boat/sail to port (leeward heel)
		assertTrue(resStarboardWind.heelAngleDeg() > 0.0F, "Boat must heel to leeward (positive roll away from starboard wind)");

		// Wind from East (270 deg, blows towards West 90 deg) -> Wind is on PORT side
		WindVector windFromEast = new WindVector(90.0F, 0.70F);
		SailingPhysics.PhysicsResult resPortWind = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, windFromEast, 0.0F, false, true, false
		);

		assertTrue(resPortWind.heelAngleDeg() < 0.0F, "Boat must heel to leeward (negative roll away from port wind)");
	}

	@Test
	public void testHikingBalancesBoatInStrongWind() {
		// Strong beam reach wind
		WindVector strongWind = new WindVector(270.0F, 0.75F);
		Vec3 headway = new Vec3(0, 0, 0.25);

		// 1. Without hiking (sailor in center)
		SailingPhysics.PhysicsResult unhiked = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, strongWind, 0.0F, false, true, 0
		);

		// 2. Sitting on windward gunwale (mode 1)
		SailingPhysics.PhysicsResult satOnGunwale = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, strongWind, 0.0F, false, true, 1
		);

		// 3. Standing on windward gunwale (mode 2, double space tap)
		SailingPhysics.PhysicsResult stoodOnGunwale = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, strongWind, 0.0F, false, true, 2
		);

		// Sitting on the gunwale largely rights the boat (reduces heel by ~80%)
		assertTrue(Math.abs(satOnGunwale.heelAngleDeg()) < Math.abs(unhiked.heelAngleDeg()) * 0.25F,
			"Sitting on gunwale must largely right the boat: " + satOnGunwale.heelAngleDeg() + " vs " + unhiked.heelAngleDeg());

		// Standing on the gunwale provides even more righting leverage, virtually flattening the boat
		assertTrue(Math.abs(stoodOnGunwale.heelAngleDeg()) <= Math.abs(satOnGunwale.heelAngleDeg()),
			"Standing on gunwale must provide maximum righting moment");
		assertTrue(Math.abs(stoodOnGunwale.heelAngleDeg()) < 2.0F,
			"Standing on gunwale must keep boat practically level even in strong wind: " + stoodOnGunwale.heelAngleDeg());
	}
}
