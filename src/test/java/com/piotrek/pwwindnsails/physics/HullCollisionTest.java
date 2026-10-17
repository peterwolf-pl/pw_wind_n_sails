package com.piotrek.pwwindnsails.physics;

import com.piotrek.pwwindnsails.physics.SailingPhysics;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HullCollisionTest {

	@Test
	public void testDiagonalChannelClearanceWithoutFalseCollisions() {
		// Boat at (100, 64, 100), yaw = 45 degrees (sailing diagonally)
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 45.0F;

		// A shore block located in the corner of the bounding box at (101.1, 64, 101.1).
		// In the old system with bounding box inflated to 3.18m (extX = 1.59, extZ = 1.59, bounds up to 101.59),
		// this corner block was inside the axis-aligned box and stopped the boat completely,
		// even though it is over a full meter away from the physical hull!
		// With our precise SAT hull collision, this block is recognized as completely clear.
		AABB cornerDiagonalBlock = new AABB(101.1, 64.0, 101.1, 102.1, 65.0, 102.1);

		assertFalse(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, cornerDiagonalBlock),
			"Shore block in diagonal AABB corner not overlapping the physical hull must NOT collide"
		);
	}

	@Test
	public void testDirectBowCollisionDetected() {
		// Boat at (100, 64, 100), heading South (yaw = 0 deg).
		// Forward vector is (0, 1), bow stem is at Z = 101.40.
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 0.0F;

		// Solid wall directly ahead at Z = 101.2 to 102.2
		AABB wallAhead = new AABB(99.5, 64.0, 101.2, 100.5, 65.0, 102.2);

		assertTrue(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, wallAhead),
			"Wall directly in path of the bow must collide and prevent penetration"
		);
	}

	@Test
	public void testGunwaleCollisionDetected() {
		// Boat heading South (yaw = 0 deg). Right vector is (1, 0), starboard gunwale at X = 100.68.
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 0.0F;

		// Block intersecting the starboard gunwale at X = 100.5 to 101.5
		AABB starboardDock = new AABB(100.5, 64.0, 99.5, 101.5, 65.0, 100.5);

		assertTrue(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, starboardDock),
			"Pier intersecting starboard gunwale must register collision"
		);
	}

	@Test
	public void testSternCollisionDetected() {
		// Boat heading South (yaw = 0 deg). Forward vector is (0, 1), stern transom at Z = 98.70.
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 0.0F;

		// Wall directly behind the stern at Z = 98.0 to 98.80
		AABB wallBehind = new AABB(99.5, 64.0, 98.0, 100.5, 65.0, 98.80);

		assertTrue(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, wallBehind),
			"Wall behind stern must register collision"
		);
	}

	@Test
	public void dockContactPushesBackIntoTheWater() {
		Vec3 push = SailingPhysics.awayFromContact(0.0, 0.0, 2.0, 0.3, 0.05);
		assertTrue(push.x < 0.0, "A pier to the east must push the hull west, back into the water");
		assertEquals(0.0, push.y, 1e-6);
	}

	@Test
	public void testSubmergedRiverbedIgnored() {
		// Riverbed block underneath the boat hull
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 30.0F;

		// Riverbed with surface below boat water draft (Y=63.0 to 64.10)
		AABB riverbed = new AABB(99.0, 63.0, 99.0, 101.0, 64.10, 101.0);

		assertFalse(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, riverbed),
			"Riverbed blocks below hull draft line must not stop the boat"
		);
	}

	@Test
	public void testOverheadClearanceIgnored() {
		// Overhead branch/leaves hanging above cockpit
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 90.0F;

		AABB overheadLeaves = new AABB(99.0, 64.80, 99.0, 101.0, 65.80, 101.0);

		assertFalse(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, overheadLeaves),
			"Overhead blocks above hull height must not stop the boat"
		);
	}

	@Test
	public void testSportSailboatExtendedHullCollision() {
		// Sport sailboat at (100, 64, 100), heading South (yaw = 0 deg)
		// Hull length: 4.5m (fwdMax = 2.15m, fwdMin = -1.95m)
		double boatX = 100.0;
		double boatY = 64.0;
		double boatZ = 100.0;
		float yaw = 0.0F;

		// Wall at Z = 102.0 to 103.0 (in path of extended 2.15m bow stem)
		AABB wallAheadOfSportBow = new AABB(99.5, 64.0, 102.0, 100.5, 65.0, 103.0);
		assertTrue(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, wallAheadOfSportBow, -1.95, 2.15, 1.05),
			"Wall directly in front of the extended sport bow stem must be detected"
		);

		// Wall behind standard boat bow (Z = 101.6) is clear for standard boat, but hits sport boat
		AABB intermediateWall = new AABB(99.5, 64.0, 101.6, 100.5, 65.0, 102.2);
		assertFalse(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, intermediateWall, -1.30, 1.40, 0.70),
			"Standard boat (1.4m bow) must not hit wall at Z=101.6"
		);
		assertTrue(
			SailingPhysics.hullIntersectsBlock(boatX, boatY, boatZ, yaw, intermediateWall, -1.95, 2.15, 1.05),
			"Sport boat (2.15m bow) must hit wall at Z=101.6"
		);
	}
}
