package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.network.WindSyncPayload;
import com.piotrek.pwwindnsails.wind.WindGust;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindState;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SailingAcceptanceTests {

	@Test
	public void testSailingDirectlyUpwindPrevented() {
		// Headwind from North (180 deg). Heading North (180 deg).
		WindVector wind = new WindVector(0.0F, 0.70F); // Wind blows towards South (from North)
		Vec3 currentVel = Vec3.ZERO;

		SailingPhysics.PhysicsResult res = SailingPhysics.step(
			currentVel, 180.0F, 0.0F, 0.0F, 0.2F, wind, 0.0F, true
		);

		assertTrue(res.inIrons(), "Must detect in irons / no-go zone");
		// In irons: speed towards North (-Z) must not increase
		assertTrue(res.newVelocity().z >= 0, "Cannot accelerate upwind");
	}

	@Test
	public void testBeamReachAndBroadReachPropulsion() {
		// Wind blows towards East (from West = 90 deg). Boat heading South (0 deg) -> Beam reach
		WindVector wind = new WindVector(270.0F, 0.70F);
		Vec3 headway = new Vec3(0, 0, 0.08);

		SailingPhysics.PhysicsResult beamReach = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.45F, wind, 0.0F, true
		);

		assertFalse(beamReach.inIrons());
		assertTrue(beamReach.newVelocity().z > headway.z, "Beam reach must generate forward acceleration");
		assertTrue(Math.abs(beamReach.boomAngleDeg()) > 10.0F, "Boom must swing out to leeward");

		// Broad reach: Boat heading South-East (315 deg)
		SailingPhysics.PhysicsResult broadReach = SailingPhysics.step(
			headway, 315.0F, 0.0F, 0.0F, 0.70F, wind, 0.0F, true
		);

		assertFalse(broadReach.inIrons());
		double speed = Math.sqrt(broadReach.newVelocity().x * broadReach.newVelocity().x + broadReach.newVelocity().z * broadReach.newVelocity().z);
		assertTrue(speed > headway.z, "Broad reach must accelerate boat");
	}

	@Test
	public void testRudderStationaryVsMoving() {
		WindVector calm = new WindVector(0.0F, 0.0F);

		// Stationary boat with full rudder
		SailingPhysics.PhysicsResult stat = SailingPhysics.step(
			Vec3.ZERO, 0.0F, 0.0F, 35.0F, 0.5F, calm, 0.0F, true
		);
		assertEquals(0.0F, stat.newYawVelocity(), 1e-4F, "At 0 speed, rudder must produce zero turning torque");

		// Moving boat with full rudder
		SailingPhysics.PhysicsResult moving = SailingPhysics.step(
			new Vec3(0, 0, 0.25), 0.0F, 0.0F, 35.0F, 0.5F, calm, 0.0F, true
		);
		assertTrue(Math.abs(moving.newYawVelocity()) > 0.4F, "Moving boat must turn with rudder authority");
	}

	@Test
	public void testMainsheetTightenVsEase() {
		// Wind blows towards East (from West). Boat heading South.
		WindVector wind = new WindVector(270.0F, 0.65F);
		Vec3 headway = new Vec3(0, 0, 0.10);

		// Sheet tight (0.0): boom angle restricted to ~MIN_BOOM_ANGLE
		SailingPhysics.PhysicsResult tightSheet = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 0.0F, wind, 0.0F, true
		);
		assertEquals(WindAndSailsConfig.MIN_BOOM_ANGLE_DEG, Math.abs(tightSheet.boomAngleDeg()), 1.0F);

		// Sheet eased (1.0): boom angle swings out up to ~MAX_BOOM_ANGLE
		SailingPhysics.PhysicsResult easedSheet = SailingPhysics.step(
			headway, 0.0F, 0.0F, 0.0F, 1.0F, wind, 0.0F, true
		);
		assertTrue(Math.abs(easedSheet.boomAngleDeg()) > 30.0F, "Boom must swing out when sheet eased");
	}

	@Test
	public void testGustPhysicallyAffectsBoat() {
		// Base wind
		WindVector baseWind = new WindVector(270.0F, 0.50F);
		Vec3 initial = new Vec3(0, 0, 0.12);

		SailingPhysics.PhysicsResult baseResult = SailingPhysics.step(
			initial, 0.0F, 0.0F, 0.0F, 0.45F, baseWind, 0.0F, true
		);

		// Wind during gust (higher strength)
		WindVector gustWind = new WindVector(270.0F, 0.50F * 1.35F);
		SailingPhysics.PhysicsResult gustResult = SailingPhysics.step(
			initial, 0.0F, 0.0F, 0.0F, 0.45F, gustWind, 0.0F, true
		);

		assertTrue(
			gustResult.newVelocity().z > baseResult.newVelocity().z,
			"Wind gust must produce greater forward acceleration"
		);
		assertTrue(
			Math.abs(gustResult.heelAngleDeg()) > Math.abs(baseResult.heelAngleDeg()),
			"Wind gust must produce stronger heel roll"
		);
	}

	@Test
	public void testRudderSensitivityScaling() {
		WindVector calm = new WindVector(0.0F, 0.0F);
		Vec3 headway = new Vec3(0, 0, 0.25);

		WindAndSailsConfig.rudderSensitivity = 1.0F;
		SailingPhysics.PhysicsResult normalTurn = SailingPhysics.step(
			headway, 0.0F, 0.0F, 20.0F, 0.45F, calm, 0.0F, true
		);

		WindAndSailsConfig.rudderSensitivity = 2.0F;
		SailingPhysics.PhysicsResult sharpTurn = SailingPhysics.step(
			headway, 0.0F, 0.0F, 20.0F, 0.45F, calm, 0.0F, true
		);

		assertTrue(
			Math.abs(sharpTurn.newYawVelocity()) > Math.abs(normalTurn.newYawVelocity()),
			"Higher rudder sensitivity must increase turning torque"
		);

		// Restore default
		WindAndSailsConfig.rudderSensitivity = 1.0F;
	}

	@Test
	public void testWindEvolutionAndSmoothWandering() {
		WindState state = new WindState(WindAndSailsConfig.PREVAILING_WIND_DIR, 0.5F);
		RandomSource random = RandomSource.create(42L);

		float initialDir = state.getBaseDirectionDeg();
		for (int t = 0; t < 200; t++) {
			state.tickServer(t, random);
		}

		float evolvedDir = state.getBaseDirectionDeg();
		float diff = Math.abs(evolvedDir - initialDir);
		assertTrue(diff > 0.01F, "Wind direction should evolve over time");
		// Check that prevailing wander stays within configured range
		assertTrue(
			Math.abs(state.getBaseDirectionDeg() - WindAndSailsConfig.PREVAILING_WIND_DIR) <= WindAndSailsConfig.PREVAILING_WANDER_RANGE + 5.0F,
			"Wind direction should wander naturally around prevailing axis"
		);
	}

	@Test
	public void testWindSyncStateAgreement() {
		WindManager manager = WindManager.getInstance();
		WindState serverState = WindState.createDefault();
		serverState.setBase(280.0F, 0.75F);
		serverState.setCurrentGust(new WindGust(100L, 80, 14.0F, 1.25F));

		WindSyncPayload payload = new WindSyncPayload(
			serverState.getBaseDirectionDeg(),
			serverState.getBaseStrength(),
			serverState.getTargetDirectionDeg(),
			serverState.getTargetStrength(),
			true,
			100L,
			80,
			14.0F,
			1.25F,
			140L
		);

		manager.applyClientSync(payload);
		WindState clientState = manager.getClientState();

		assertEquals(serverState.getBaseDirectionDeg(), clientState.getBaseDirectionDeg(), 1.0F);
		assertEquals(serverState.getBaseStrength(), clientState.getBaseStrength(), 0.05F);
		assertNotNull(clientState.getCurrentGust());
		assertEquals(14.0F, clientState.getCurrentGust().getDirectionShiftDeg(), 1e-4F);
	}
}
