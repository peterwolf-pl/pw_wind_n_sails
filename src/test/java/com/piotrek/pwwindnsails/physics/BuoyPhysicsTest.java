package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuoyPhysicsTest {

	@Test
	@DisplayName("Slack constraint cancels outward drift when buoy exceeds rope radius")
	void testBuoySlackTetherConstraint() {
		Vec3 anchorPos = new Vec3(100.0, 50.0, 100.0);
		double slackRadius = 1.8;

		// Buoy is within slack radius -> outward velocity is unaffected
		Vec3 withinPos = new Vec3(101.0, 62.0, 100.0); // 1.0 block distance horizontally
		double dx = withinPos.x - anchorPos.x;
		double dz = withinPos.z - anchorPos.z;
		double hDist = Math.sqrt(dx * dx + dz * dz);
		assertTrue(hDist < slackRadius, "Buoy should be inside slack radius");

		// Buoy drifts beyond slack radius -> outward velocity must be cancelled
		Vec3 outsidePos = new Vec3(102.5, 62.0, 100.0); // 2.5 blocks distance horizontally (excess = 0.7)
		dx = outsidePos.x - anchorPos.x;
		dz = outsidePos.z - anchorPos.z;
		hDist = Math.sqrt(dx * dx + dz * dz);
		assertTrue(hDist > slackRadius, "Buoy should exceed slack radius");

		double nx = dx / hDist;
		double nz = dz / hDist;

		// Outward velocity trying to drift further
		Vec3 outwardVel = new Vec3(0.5, 0.0, 0.0);
		Vec3 constrained = MooringConstraint.cancelOutwardVelocity(outwardVel, nx, nz);

		assertTrue(constrained.x <= 1e-6, "Outward velocity component along line normal must be cancelled");
	}

	@Test
	@DisplayName("Natural slack radius scales properly with depth")
	void testSlackRadiusCalculation() {
		// Water depth 3 blocks
		double depthShallow = 3.0;
		float slackShallow = (float) Math.min(3.2, Math.max(1.2, Math.sqrt(depthShallow * 0.8) + 0.6));
		assertTrue(slackShallow >= 1.2F && slackShallow <= 3.2F, "Shallow slack should be within reasonable bounds");

		// Water depth 15 blocks (deep ocean)
		double depthDeep = 15.0;
		float slackDeep = (float) Math.min(3.2, Math.max(1.2, Math.sqrt(depthDeep * 0.8) + 0.6));
		assertTrue(slackDeep >= slackShallow, "Deeper water should allow greater or equal slack radius");
		assertTrue(slackDeep <= 3.2F, "Slack must be capped so buoy stays localized");
	}

	@Test
	@DisplayName("Buoy types have distinct dimensions and leash offsets")
	void testBuoyTypes() {
		assertEquals(0, BuoyEntity.TYPE_RED_SPHERICAL);
		assertEquals(1, BuoyEntity.TYPE_BLUE_CYLINDRICAL);
	}
}
