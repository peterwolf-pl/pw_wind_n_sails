package com.piotrek.pwwindnsails.wind;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Daily macro weather profile for wind simulation.
 * Governs prevailing wind direction, base strength, gust frequency/intensity,
 * and day vs night dynamics across in-game days.
 */
public record DailyWindProfile(
	long dayIndex,
	WindDayType dayType,
	float prevailingDirectionDeg,
	float wanderRangeDeg,
	float baseStrength,
	float gustChance,
	float gustMinMult,
	float gustMaxMult,
	float gustMaxShiftDeg,
	int gustMinDuration,
	int gustMaxDuration,
	float nightStrengthFactor,
	boolean windyNight
) {
	public enum WindDayType {
		CALM("Cisza / Flauta"),
		LIGHT_BREEZE("Słaby wiatr"),
		MODERATE_BREEZE("Umiarkowany wiatr"),
		GUSTY_WIND("Wietrznie / Szkwały"),
		STORMY_GALE("Silny wiatr / Wichura");

		private final String displayName;

		WindDayType(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return this.displayName;
		}
	}

	public static DailyWindProfile getForDay(long dayIndex, long seed, Float day0DirOverride, Float day0StrOverride) {
		if (dayIndex == 0L && day0DirOverride != null) {
			float baseStr = day0StrOverride != null ? day0StrOverride : 0.50F;
			WindDayType type = baseStr < 0.08F ? WindDayType.CALM :
				baseStr < 0.30F ? WindDayType.LIGHT_BREEZE :
				baseStr < 0.65F ? WindDayType.MODERATE_BREEZE : WindDayType.GUSTY_WIND;
			return new DailyWindProfile(
				0L,
				type,
				WindState.normalizeDeg(day0DirOverride),
				35.0F,
				baseStr,
				0.004F,
				1.10F,
				1.40F,
				18.0F,
				40,
				240,
				0.50F,
				false
			);
		}

		long daySeed = mix64(seed ^ (dayIndex * 0x517cc1b727220a95L));
		RandomSource random = RandomSource.create(daySeed);

		float dir = random.nextFloat() * 360.0F;
		float roll = random.nextFloat();

		WindDayType type;
		float baseStrength;
		float wanderRange;
		float gustChance;
		float gustMinMult;
		float gustMaxMult;
		float gustMaxShift;
		int minDur;
		int maxDur;
		boolean windyNight;
		float nightStrengthFactor;

		if (roll < 0.12F) {
			// Flauta / Calm day (~12% chance)
			type = WindDayType.CALM;
			baseStrength = 0.01F + random.nextFloat() * 0.035F; // 0.01 - 0.045: calm doldrums
			wanderRange = 15.0F;
			gustChance = 0.0003F; // Gusts almost non-existent
			gustMinMult = 1.02F;
			gustMaxMult = 1.06F;  // "tylko o kilka % mocniejsze"
			gustMaxShift = 3.0F;
			minDur = 20;
			maxDur = 80;
			windyNight = false;
			nightStrengthFactor = 0.8F + random.nextFloat() * 0.2F;
		} else if (roll < 0.38F) {
			// Light breeze (~26% chance)
			type = WindDayType.LIGHT_BREEZE;
			baseStrength = 0.12F + random.nextFloat() * 0.16F; // 0.12 - 0.28
			wanderRange = 25.0F;
			gustChance = 0.0018F;
			gustMinMult = 1.05F;
			gustMaxMult = 1.15F;
			gustMaxShift = 9.0F;
			minDur = 30;
			maxDur = 120;
			windyNight = random.nextFloat() < 0.22F;
			nightStrengthFactor = windyNight ? (0.85F + random.nextFloat() * 0.20F) : (0.30F + random.nextFloat() * 0.20F);
		} else if (roll < 0.76F) {
			// Moderate sailing breeze (~38% chance)
			type = WindDayType.MODERATE_BREEZE;
			baseStrength = 0.32F + random.nextFloat() * 0.25F; // 0.32 - 0.57
			wanderRange = 35.0F;
			gustChance = 0.0038F;
			gustMinMult = 1.15F;
			gustMaxMult = 1.30F;
			gustMaxShift = 16.0F;
			minDur = 40;
			maxDur = 200;
			windyNight = random.nextFloat() < 0.30F;
			nightStrengthFactor = windyNight ? (0.85F + random.nextFloat() * 0.25F) : (0.35F + random.nextFloat() * 0.25F);
		} else if (roll < 0.93F) {
			// Gusty wind (~17% chance)
			type = WindDayType.GUSTY_WIND;
			baseStrength = 0.50F + random.nextFloat() * 0.25F; // 0.50 - 0.75
			wanderRange = 45.0F;
			gustChance = 0.0075F; // Frequent gusts
			gustMinMult = 1.30F;
			gustMaxMult = 1.55F; // Strong gusts
			gustMaxShift = 24.0F;
			minDur = 60;
			maxDur = 240;
			windyNight = random.nextFloat() < 0.38F;
			nightStrengthFactor = windyNight ? (0.90F + random.nextFloat() * 0.25F) : (0.40F + random.nextFloat() * 0.25F);
		} else {
			// Stormy gale (~7% chance)
			type = WindDayType.STORMY_GALE;
			baseStrength = 0.70F + random.nextFloat() * 0.22F; // 0.70 - 0.92
			wanderRange = 50.0F;
			gustChance = 0.0100F; // Very frequent gusts
			gustMinMult = 1.40F;
			gustMaxMult = 1.70F; // Powerful gusts
			gustMaxShift = 30.0F;
			minDur = 80;
			maxDur = 260;
			windyNight = random.nextFloat() < 0.50F;
			nightStrengthFactor = windyNight ? (0.95F + random.nextFloat() * 0.25F) : (0.45F + random.nextFloat() * 0.25F);
		}

		return new DailyWindProfile(
			dayIndex,
			type,
			dir,
			wanderRange,
			baseStrength,
			gustChance,
			gustMinMult,
			gustMaxMult,
			gustMaxShift,
			minDur,
			maxDur,
			nightStrengthFactor,
			windyNight
		);
	}

	public static String getCompassDirection(float deg) {
		float normalized = (deg % 360.0F + 360.0F) % 360.0F;
		if (normalized >= 337.5F || normalized < 22.5F) return "S";
		if (normalized < 67.5F) return "SW";
		if (normalized < 112.5F) return "W";
		if (normalized < 157.5F) return "NW";
		if (normalized < 202.5F) return "N";
		if (normalized < 247.5F) return "NE";
		if (normalized < 292.5F) return "E";
		return "SE";
	}

	public static String generateForecast(long dayTime, DailyWindProfile pCurrent, DailyWindProfile pNext) {
		int timeOfDay = (int) Math.floorMod(dayTime, 24000L);
		String nextDirStr = getCompassDirection(pNext.prevailingDirectionDeg());
		double nextKnots = pNext.baseStrength() * 38.87;

		if (timeOfDay < 12000) {
			// Daytime
			if (pCurrent.dayType() == WindDayType.CALM) {
				return pNext.dayType() == WindDayType.CALM
					? "Cisza utrzyma się do jutra"
					: String.format("Flauta; o świcie powiew z %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			if (pNext.dayType() == WindDayType.CALM) {
				return "Uwaga: nadchodzi flauta, jutro cisza";
			}
			if (pCurrent.windyNight()) {
				return String.format("W nocy wiatr nie osłabnie; jutro %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			if (pNext.dayType() == WindDayType.GUSTY_WIND || pNext.dayType() == WindDayType.STORMY_GALE) {
				return String.format("Po zmroku spokój; jutro szkwały z %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			return String.format("Po zmroku osłabienie wiatru; jutro z %s (~%.0f kn)", nextDirStr, nextKnots);
		} else if (timeOfDay < 18000) {
			// Evening / Dusk
			if (pNext.dayType() == WindDayType.CALM) {
				return "Wiatr powoli cichnie; jutro flauta";
			}
			if (pNext.dayType() == WindDayType.STORMY_GALE) {
				return String.format("Ostrzeżenie: o świcie wichura z %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			float dirDiff = Math.abs(Mth.wrapDegrees(pNext.prevailingDirectionDeg() - pCurrent.prevailingDirectionDeg()));
			if (dirDiff > 50.0F) {
				return String.format("W nocy spokojnie; o świcie skręt na %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			return String.format("Równy wiatr nocny; jutro z %s (~%.0f kn)", nextDirStr, nextKnots);
		} else {
			// Late night / Dawn
			if (pNext.dayType() == WindDayType.CALM) {
				return "Wiatr gaśnie – o świcie flauta";
			}
			if (pNext.dayType() == WindDayType.GUSTY_WIND || pNext.dayType() == WindDayType.STORMY_GALE) {
				return String.format("O świcie nasilenie szkwałów z %s (~%.0f kn)", nextDirStr, nextKnots);
			}
			return String.format("O świcie wiatr ustali się z %s (~%.0f kn)", nextDirStr, nextKnots);
		}
	}

	public static long mix64(long z) {
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}
}
