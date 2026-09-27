package com.piotrek.pwwindnsails.wind;

import com.piotrek.pwwindnsails.client.WindVisualizerRenderer;
import com.piotrek.pwwindnsails.physics.SailingPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
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

	@Test
	public void testCalmFlautaDayOccurrencesAndValues() {
		// Over 100 days, CALM (flauta) days must appear naturally
		int calmCount = 0;
		for (long day = 1; day <= 100; day++) {
			DailyWindProfile p = DailyWindProfile.getForDay(day, 424242L, null, null);
			if (p.dayType() == DailyWindProfile.WindDayType.CALM) {
				calmCount++;
				// Verify flauta properties: minimal strength, barely noticeable gusts ("o kilka % mocniejsze")
				assertTrue(p.baseStrength() <= 0.055F, "Calm flauta base strength must be near zero: " + p.baseStrength());
				assertTrue(p.gustChance() <= 0.0005F, "Calm flauta gust chance must be negligible: " + p.gustChance());
				assertTrue(p.gustMaxMult() <= 1.07F, "Calm day gusts must be only a few % stronger: " + p.gustMaxMult());
			}
		}
		assertTrue(calmCount > 0, "Calm/flauta days must occur naturally across 100 days");
	}

	@Test
	public void testWindDirectionDiversityAcrossDays() {
		// Wind should blow from different directions on different days
		Set<Integer> quadrants = new HashSet<>();
		for (long day = 1; day <= 40; day++) {
			DailyWindProfile p = DailyWindProfile.getForDay(day, 888888L, null, null);
			int quadrant = (int) (p.prevailingDirectionDeg() / 90.0F);
			quadrants.add(quadrant);
		}
		// Must cover all 4 quadrants across days
		assertEquals(4, quadrants.size(), "Wind directions across days must span all quadrants (North, South, East, West)");
	}

	@Test
	public void testGustIntensityDifferenceAcrossDays() {
		boolean foundStrongGustDay = false;
		boolean foundGentleGustDay = false;

		for (long day = 1; day <= 80; day++) {
			DailyWindProfile p = DailyWindProfile.getForDay(day, 1234567L, null, null);
			if (p.dayType() == DailyWindProfile.WindDayType.GUSTY_WIND || p.dayType() == DailyWindProfile.WindDayType.STORMY_GALE) {
				foundStrongGustDay = true;
				assertTrue(p.gustMaxMult() >= 1.40F, "Gusty days must have strong gusts >= 1.40x: " + p.gustMaxMult());
				assertTrue(p.gustChance() >= 0.005F, "Gusty days must have frequent gusts: " + p.gustChance());
			}
			if (p.dayType() == DailyWindProfile.WindDayType.CALM) {
				foundGentleGustDay = true;
				assertTrue(p.gustMaxMult() <= 1.07F, "Quiet days must have gusts only a few % stronger: " + p.gustMaxMult());
			}
		}

		assertTrue(foundStrongGustDay, "Must generate gusty days with frequent strong gusts");
		assertTrue(foundGentleGustDay, "Must generate quiet days where gusts barely exceed base wind");
	}

	@Test
	public void testDiurnalCycleAndWindyNights() {
		boolean foundWindyNight = false;
		boolean foundStandardThermalDay = false;

		for (long day = 1; day <= 80; day++) {
			DailyWindProfile p = DailyWindProfile.getForDay(day, 999999L, null, null);
			if (p.windyNight()) {
				foundWindyNight = true;
				assertTrue(p.nightStrengthFactor() >= 0.85F, "Windy nights should preserve/boost wind: " + p.nightStrengthFactor());
			} else if (p.dayType() != DailyWindProfile.WindDayType.CALM) {
				foundStandardThermalDay = true;
				assertTrue(p.nightStrengthFactor() <= 0.70F, "Standard diurnal days should have lower night wind: " + p.nightStrengthFactor());
			}
		}

		assertTrue(foundStandardThermalDay, "Most days should have weaker winds at night than day");
		assertTrue(foundWindyNight, "Windy nights must also occur occasionally");
	}

	@Test
	public void testNighttimeEvenWind() {
		WindState state = new WindState(180.0F, 0.60F);
		RandomSource random = RandomSource.create(55L);

		// Daytime: tick at midday (tick 6000)
		state.tickServer(6000L, 6000L, random);
		assertEquals(0.0F, state.getNightWeight(), 1e-3F, "Daytime nightWeight should be 0");
		assertTrue(state.getDiurnalFactor() >= 1.0F, "Daytime diurnal factor should be at or above base");

		// Nighttime: tick at midnight (tick 18000)
		state.tickServer(18000L, 18000L, random);
		assertEquals(1.0F, state.getNightWeight(), 1e-3F, "Midnight nightWeight should be 1.0");
	}

	@Test
	public void testFlautaPhysicsZeroAcceleration() {
		// Boat in flauta (calm wind 0.02)
		WindVector flautaWind = new WindVector(90.0F, 0.02F);
		Vec3 stationary = Vec3.ZERO;

		SailingPhysics.PhysicsResult result = SailingPhysics.step(
			stationary,
			0.0F, // boat heading South
			0.0F,
			0.0F,
			0.5F,
			flautaWind,
			0.0F,
			false, // sail trimmed
			true
		);

		double forwardSpeed = Math.sqrt(result.newVelocity().x * result.newVelocity().x + result.newVelocity().z * result.newVelocity().z);
		assertTrue(forwardSpeed < 0.005, "In flauta calm, sailboat should have virtually zero speed: " + forwardSpeed);
		assertEquals(0.0F, result.heelAngleDeg(), 0.5F, "Boat should stay upright in flauta");
	}

	@Test
	public void testForecastGenerationAndWindSync() {
		DailyWindProfile pCurrent = DailyWindProfile.getForDay(1L, 12345L, null, null);
		DailyWindProfile pNext = DailyWindProfile.getForDay(2L, 12345L, null, null);

		// Daytime forecast
		String daytimeForecast = DailyWindProfile.generateForecast(6000L, pCurrent, pNext);
		assertNotNull(daytimeForecast);
		assertFalse(daytimeForecast.isEmpty());

		// Evening forecast
		String eveningForecast = DailyWindProfile.generateForecast(15000L, pCurrent, pNext);
		assertNotNull(eveningForecast);
		assertFalse(eveningForecast.isEmpty());

		// Client sync test
		WindManager manager = WindManager.getInstance();
		com.piotrek.pwwindnsails.network.WindSyncPayload payload = new com.piotrek.pwwindnsails.network.WindSyncPayload(
			180.0F, 0.45F, 190.0F, 0.50F, true, 100L, 80, 15.0F, 1.30F, 1000L, daytimeForecast
		);
		manager.applyClientSync(payload);

		assertEquals(daytimeForecast, manager.getClientState().getForecast());
		assertEquals(180.0F, manager.getClientState().getBaseDirectionDeg(), 1e-4F);
		assertEquals(0.45F, manager.getClientState().getBaseStrength(), 1e-4F);
	}

	@Test
	public void testPlaneCompatHelperNullSafety() {
		assertFalse(com.piotrek.pwwindnsails.client.PlaneCompatHelper.isPlayerInPlaneOrParagliding(null));
	}

	@Test
	public void testFlagDesynchronizationAcrossPositions() {
		BlockPos pos1 = new BlockPos(10, 64, 20);
		BlockPos pos2 = new BlockPos(10, 64, 25);
		BlockPos pos3 = new BlockPos(15, 64, 20);

		long h1 = (long) pos1.getX() * 3129871L ^ (long) pos1.getZ() * 116129781L ^ (long) pos1.getY() * 4231L;
		h1 = (h1 ^ (h1 >>> 16)) * 0x45d9f3bL;
		h1 = (h1 ^ (h1 >>> 16)) * 0x45d9f3bL;
		h1 = h1 ^ (h1 >>> 16);

		long h2 = (long) pos2.getX() * 3129871L ^ (long) pos2.getZ() * 116129781L ^ (long) pos2.getY() * 4231L;
		h2 = (h2 ^ (h2 >>> 16)) * 0x45d9f3bL;
		h2 = (h2 ^ (h2 >>> 16)) * 0x45d9f3bL;
		h2 = h2 ^ (h2 >>> 16);

		assertNotEquals(h1, h2, "Flags at different positions must produce distinct animation hashes");
		float phase1 = (float) ((h1 & 0xFFFF) / 65535.0 * 2.0 * Math.PI);
		float phase2 = (float) ((h2 & 0xFFFF) / 65535.0 * 2.0 * Math.PI);
		assertNotEquals(phase1, phase2, 1e-3F, "Flag phase offsets must be desynchronized");
	}

	@Test
	public void testVisualizerModeCycling() {
		WindVisualizerRenderer.setMode(WindVisualizerRenderer.VisualizerMode.SEAFOAM_WHITE);
		assertTrue(WindVisualizerRenderer.isEnabled());

		WindVisualizerRenderer.VisualizerMode m1 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.BRIGHT_CYAN, m1);

		WindVisualizerRenderer.VisualizerMode m2 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.WARM_AMBER, m2);

		WindVisualizerRenderer.VisualizerMode m3 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.SOFT_SKY, m3);

		WindVisualizerRenderer.VisualizerMode m4 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.WATER_SHADOW, m4);

		WindVisualizerRenderer.VisualizerMode m5 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.OFF, m5);
		assertFalse(WindVisualizerRenderer.isEnabled());

		WindVisualizerRenderer.VisualizerMode m0 = WindVisualizerRenderer.cycleMode();
		assertEquals(WindVisualizerRenderer.VisualizerMode.SEAFOAM_WHITE, m0);
		assertTrue(WindVisualizerRenderer.isEnabled());
	}

	@Test
	public void testEnvironmentalWindEffectsConfigAndPayload() {
		com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects = true;
		assertTrue(com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects);

		// Test payload sync
		com.piotrek.pwwindnsails.network.WindSyncPayload payloadOff = new com.piotrek.pwwindnsails.network.WindSyncPayload(
			180.0F, 0.5F, 180.0F, 0.5F, false, 0L, 0, 0.0F, 1.0F, 100L, "Prognoza", false
		);
		assertFalse(payloadOff.environmentalEffects());
		WindManager.getInstance().applyClientSync(payloadOff);
		assertFalse(com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects);

		com.piotrek.pwwindnsails.network.WindSyncPayload payloadOn = new com.piotrek.pwwindnsails.network.WindSyncPayload(
			180.0F, 0.5F, 180.0F, 0.5F, false, 0L, 0, 0.0F, 1.0F, 100L, "Prognoza", true
		);
		assertTrue(payloadOn.environmentalEffects());
		WindManager.getInstance().applyClientSync(payloadOn);
		assertTrue(com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects);
	}
}
