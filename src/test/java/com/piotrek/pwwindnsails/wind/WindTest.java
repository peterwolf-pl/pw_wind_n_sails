package com.piotrek.pwwindnsails.wind;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WindTest {

	@Test
	public void testWindVectorDirections() {
		// Yaw 0 = South (+Z)
		WindVector south = new WindVector(0.0F, 1.0F);
		assertEquals(0.0, south.toVec3().x, 1e-4);
		assertEquals(1.0, south.toVec3().z, 1e-4);

		// Yaw 90 = West (-X)
		WindVector west = new WindVector(90.0F, 1.0F);
		assertEquals(-1.0, west.toVec3().x, 1e-4);
		assertEquals(0.0, west.toVec3().z, 1e-4);

		// Yaw 180 = North (-Z)
		WindVector north = new WindVector(180.0F, 1.0F);
		assertEquals(0.0, north.toVec3().x, 1e-4);
		assertEquals(-1.0, north.toVec3().z, 1e-4);

		// Yaw 270 = East (+X)
		WindVector east = new WindVector(270.0F, 1.0F);
		assertEquals(1.0, east.toVec3().x, 1e-4);
		assertEquals(0.0, east.toVec3().z, 1e-4);
	}

	@Test
	public void testWindGustLifecycle() {
		long start = 1000L;
		int duration = 100;
		WindGust gust = new WindGust(start, duration, 15.0F, 1.30F);

		// Before start
		assertEquals(0.0F, gust.getIntensity(999L), 1e-4F);
		assertFalse(gust.isExpired(1050L));

		// At start
		assertEquals(0.0F, gust.getIntensity(1000L), 1e-4F);

		// At peak (midway = 1050L)
		float peakIntensity = gust.getIntensity(1050L);
		assertEquals(1.0F, peakIntensity, 0.05F);
		assertEquals(15.0F, gust.getEffectiveShift(1050L), 0.75F);
		assertEquals(1.30F, gust.getEffectiveMultiplier(1050L), 0.02F);

		// At end
		assertTrue(gust.isExpired(1100L));
		assertEquals(0.0F, gust.getIntensity(1100L), 1e-4F);
	}

	@Test
	public void testWindStateNormalization() {
		WindState state = new WindState(-45.0F, 0.5F);
		assertEquals(315.0F, state.getBaseDirectionDeg(), 1e-4F);

		float normalized = WindState.normalizeDeg(400.0F);
		assertEquals(40.0F, normalized, 1e-4F);
	}
}
