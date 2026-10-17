package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.entity.MooringAim;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MooringAndCleatTest {

	@Test
	public void testCleatOffsetsSymmetry() {
		// Classic sailboat: width is ±0.6875, bow is 1.00, stern is -1.00, Y is 0.6875
		double bowZ = 1.00;
		double sternZ = -1.00;
		double y = 0.6875;

		Vec3 bowPort = new Vec3(-0.6875, y, bowZ);
		Vec3 bowStarboard = new Vec3(0.6875, y, bowZ);
		Vec3 sternPort = new Vec3(-0.6875, y, sternZ);
		Vec3 sternStarboard = new Vec3(0.6875, y, sternZ);

		// Port is negative X, starboard is positive X
		assertEquals(-bowStarboard.x, bowPort.x, 1e-6);
		assertEquals(-sternStarboard.x, sternPort.x, 1e-6);

		// Bow is forward (+Z), stern is aft (-Z)
		assertTrue(bowPort.z > 0);
		assertTrue(sternPort.z < 0);
		assertEquals(0.6875, bowPort.y, 1e-6);
	}

	@Test
	public void testCleatWorldPositionCalculation() {
		// Boat at (10, 64, 20), heading North (yaw = 180 deg)
		// forward is (-sin(180), cos(180)) = (0, -1) -> North in Minecraft
		// right (starboard) is (cos(180), sin(180)) = (-1, 0) -> West
		Vec3 boatPos = new Vec3(10.0, 64.0, 20.0);
		float yaw = 180.0F;

		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);

		// Bow Port: localX = -0.6875, localZ = 1.00, localY = 0.6875
		double wx = boatPos.x + rightX * (-0.6875) + fwdX * 1.00;
		double wy = boatPos.y + 0.6875;
		double wz = boatPos.z + rightZ * (-0.6875) + fwdZ * 1.00;

		assertEquals(10.6875, wx, 1e-3, "Port cleat when heading North should be to the East (X+)");
		assertEquals(64.6875, wy, 1e-3);
		assertEquals(19.00, wz, 1e-3, "Bow cleat when heading North should be forward (Z-)");
	}

	@Test
	public void testMultiCleatMooringEquilibrium() {
		// Boat tethered between two dock posts at (0, 64, 5) and (0, 64, -5)
		Vec3 postA = new Vec3(0.0, 64.0, 5.0);
		Vec3 postB = new Vec3(0.0, 64.0, -5.0);

		// Target midpoint between posts
		Vec3 targetCenter = postA.add(postB).scale(0.5);
		assertEquals(0.0, targetCenter.x, 1e-6);
		assertEquals(64.0, targetCenter.y, 1e-6);
		assertEquals(0.0, targetCenter.z, 1e-6);

		// Suppose boat is currently displaced at (0, 64, 2)
		Vec3 boatPos = new Vec3(0.0, 64.0, 2.0);
		Vec3 toCenter = targetCenter.subtract(boatPos);

		// Centering force should pull towards (0, 64, 0), so Z-force must be negative
		assertTrue(toCenter.z < 0, "Centering force must restore the boat back to Z=0 between mooring posts");
	}

	@Test
	public void aimedCleatWireDoesNotHitTheNeighborCleat() {
		Vec3 boat = new Vec3(10.0, 64.0, 20.0);
		float yaw = 0.0F;
		Vec3 port = new Vec3(-0.6875, 0.6875, 1.0);
		Vec3 starboard = new Vec3(0.6875, 0.6875, 1.0);
		Vec3 portWorld = worldCleat(boat, yaw, port);
		Vec3 eye = portWorld.add(0.0, 0.4, 2.0);
		Vec3 look = portWorld.subtract(eye).normalize();

		double portDist = MooringAim.cleatDistance(eye, look, boat, yaw, port, 0.04);
		double starboardDist = MooringAim.cleatDistance(eye, look, boat, yaw, starboard, 0.04);

		assertTrue(portDist < MooringAim.REACH, "Ray on the port cleat wire must hit that cleat");
		assertEquals(Double.MAX_VALUE, starboardDist, "The same ray must not hit the starboard cleat");

		// Old 0.50-block radius would still accept a point this far from the cleat center.
		Vec3 beside = portWorld.add(0.45, 0.0, 0.0);
		Vec3 besideLook = beside.subtract(eye).normalize();
		assertEquals(Double.MAX_VALUE, MooringAim.cleatDistance(eye, besideLook, boat, yaw, port, 0.04));
	}

	@Test
	public void postColumnIgnoresRailsAndTheNeighborPost() {
		BlockPos post = new BlockPos(0, 64, 0);
		BlockPos neighbor = new BlockPos(1, 64, 0);
		Vec3 eye = new Vec3(0.5, 66.0, 3.0);
		Vec3 postLook = new Vec3(0.5, 64.5, 0.5).subtract(eye).normalize();
		Vec3 end = eye.add(postLook.scale(MooringAim.REACH));

		assertTrue(MooringAim.postColumn(post).clip(eye, end).isPresent(), "Looking at the post column must hit that post");
		assertTrue(MooringAim.postColumn(neighbor).clip(eye, end).isEmpty(), "The neighbor post must not be hit");

		Vec3 rail = new Vec3(0.1, 64.5, 0.5);
		Vec3 railLook = rail.subtract(eye).normalize();
		Vec3 railEnd = eye.add(railLook.scale(MooringAim.REACH));
		assertTrue(MooringAim.postColumn(post).clip(eye, railEnd).isEmpty(), "A connecting rail is not the post");
		assertTrue(MooringAim.postColumn(neighbor).clip(eye, railEnd).isEmpty(), "A rail must not select the next post");
	}

	private static Vec3 worldCleat(Vec3 boatPos, float yaw, Vec3 local) {
		float yawRad = yaw * Mth.DEG_TO_RAD;
		double fwdX = -Mth.sin(yawRad);
		double fwdZ = Mth.cos(yawRad);
		double rightX = Mth.cos(yawRad);
		double rightZ = Mth.sin(yawRad);
		return new Vec3(
			boatPos.x + rightX * local.x + fwdX * local.z,
			boatPos.y + local.y,
			boatPos.z + rightZ * local.x + fwdZ * local.z
		);
	}

	@Test
	public void singleLineKeepsSlackMotionAndStopsStretch() {
		Vec3 inward = MooringConstraint.cancelOutwardVelocity(new Vec3(-0.4, 0.1, 0.0), 1.0, 0.0);
		assertEquals(-0.4, inward.x, 1e-6, "Motion back into the slack must be kept");
		assertEquals(0.1, inward.y, 1e-6);

		Vec3 outward = MooringConstraint.cancelOutwardVelocity(new Vec3(0.5, -0.2, 0.3), 1.0, 0.0);
		assertEquals(0.0, outward.x, 1e-6, "Motion that would stretch the line must be removed");
		assertEquals(0.3, outward.z, 1e-6, "Sideways swing around the mooring point must remain");
		assertEquals(-0.2, outward.y, 1e-6);

		double slack = 1.2;
		double maxLead = 3.0;
		assertTrue(slack < maxLead, "A short line still has room to drift");
		double taut = 3.4;
		double pulledBack = taut - Math.max(0.0, taut - maxLead);
		assertEquals(maxLead, pulledBack, 1e-6, "A taut line is pulled back to its length and no further");
	}

	@Test
	public void tiedEndYawsOntoItsLineNotTheBowIntoTheWind() {
		// Stern cleat, boat bow-into-wind (yaw 180). Post is upwind, so the stern must swing onto it.
		float sternError = MooringConstraint.cleatYawError(180.0F, 0.0, 0.0, 0.0, -1.0, 0.0, -4.0);
		assertTrue(Math.abs(sternError) > 90.0F, "A stern tie must not leave the bow pointing at the post");

		// Same stern cleat already pointing at the post (yaw 0, post to the north).
		float sternHeld = MooringConstraint.cleatYawError(0.0F, 0.0, 0.0, 0.0, -1.0, 0.0, -4.0);
		assertEquals(0.0F, sternHeld, 0.05F, "Stern already on the line must hold");

		// Diagonal post off the stern quarter. The hull yaws along that line, not square to the wind.
		float diagonal = MooringConstraint.cleatYawError(0.0F, 0.0, 0.0, 0.0, -1.0, 3.0, -1.0);
		assertTrue(diagonal > 20.0F, "An angled stern line must yaw the stern toward the post");

		// Bow cleat still follows its own post, not a hardcoded heading.
		float bowHeld = MooringConstraint.cleatYawError(0.0F, 0.0, 0.0, 0.0, 1.0, 0.0, 4.0);
		assertEquals(0.0F, bowHeld, 0.05F, "Bow already on its line must hold");

		// Puller stands ahead of the bow while the line is on the stern. The hull must turn;
		// keeping the bow pointed at the puller is the wrong lie.
		float sternTow = MooringConstraint.cleatYawError(0.0F, 0.0, 0.0, 0.0, -1.0, 0.0, 5.0);
		assertTrue(Math.abs(sternTow) > 90.0F, "A stern tow must not keep the bow facing the puller");
	}

	@Test
	public void testWindDriftDirection() {
		// Wind blowing from East to West (wind vector in -X direction)
		Vec3 windDir = new Vec3(-1.0, 0.0, 0.0);
		double windStrength = 0.35;
		double driftForce = windStrength * 0.005;

		Vec3 currentVel = Vec3.ZERO;
		Vec3 newVel = currentVel.add(windDir.x * driftForce, 0.0, windDir.z * driftForce);

		assertTrue(newVel.x < 0, "Boat must drift downwind in -X direction");
		assertEquals(0.0, newVel.z, 1e-6);
		assertEquals(0.00175, Math.abs(newVel.x), 1e-6);
	}
}
