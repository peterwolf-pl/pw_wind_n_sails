package com.piotrek.pwwindnsails.wind;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * State of wind for a single world/dimension.
 * Simulates daily weather regimes (flauta calm, prevailing wind direction shifts,
 * day/night diurnal breeze, and night steady laminar flow).
 */
public final class WindState {
	private float baseDirectionDeg;
	private float baseStrength;
	private float targetDirectionDeg;
	private float targetStrength;
	@Nullable
	private WindGust currentGust;

	private long seed;
	private boolean hasWorldSeed;
	private final float initialDirectionDeg;
	private final float initialStrength;

	private long cachedDayIndex = Long.MIN_VALUE;
	@Nullable
	private DailyWindProfile cachedCurrentProfile;
	@Nullable
	private DailyWindProfile cachedNextProfile;

	private float lastMacroPrevailingDir;
	private float lastNightWeight;
	private float lastDiurnalFactor = 1.0F;
	private String forecast = "";

	public WindState(float initialDirectionDeg, float initialStrength) {
		this(initialDirectionDeg, initialStrength, 12345L, false);
	}

	public WindState(float initialDirectionDeg, float initialStrength, long seed, boolean hasWorldSeed) {
		this.initialDirectionDeg = normalizeDeg(initialDirectionDeg);
		this.initialStrength = Math.max(0.0F, initialStrength);
		this.seed = seed;
		this.hasWorldSeed = hasWorldSeed;

		this.baseDirectionDeg = this.initialDirectionDeg;
		this.baseStrength = this.initialStrength;
		this.targetDirectionDeg = this.baseDirectionDeg;
		this.targetStrength = this.baseStrength;
		this.lastMacroPrevailingDir = this.baseDirectionDeg;
	}

	public static WindState createDefault() {
		return new WindState(WindAndSailsConfig.PREVAILING_WIND_DIR, 0.50F);
	}

	public void setSeed(long seed) {
		this.seed = seed;
		this.hasWorldSeed = true;
		this.cachedDayIndex = Long.MIN_VALUE; // Force re-evaluating daily profiles
	}

	public boolean hasWorldSeed() {
		return this.hasWorldSeed;
	}

	public float getBaseDirectionDeg() {
		return this.baseDirectionDeg;
	}

	public float getBaseStrength() {
		return this.baseStrength;
	}

	public float getTargetDirectionDeg() {
		return this.targetDirectionDeg;
	}

	public float getTargetStrength() {
		return this.targetStrength;
	}

	@Nullable
	public DailyWindProfile getCurrentDayProfile() {
		return this.cachedCurrentProfile;
	}

	@Nullable
	public DailyWindProfile getNextDayProfile() {
		return this.cachedNextProfile;
	}

	public String getForecast() {
		return this.forecast;
	}

	public void setForecast(String forecast) {
		this.forecast = forecast != null ? forecast : "";
	}

	public float getMacroPrevailingDir() {
		return this.lastMacroPrevailingDir;
	}

	public float getNightWeight() {
		return this.lastNightWeight;
	}

	public float getDiurnalFactor() {
		return this.lastDiurnalFactor;
	}

	@Nullable
	public WindGust getCurrentGust() {
		return this.currentGust;
	}

	public void setCurrentGust(@Nullable WindGust gust) {
		this.currentGust = gust;
	}

	public void setBase(float directionDeg, float strength) {
		this.baseDirectionDeg = normalizeDeg(directionDeg);
		this.baseStrength = Mth.clamp(strength, 0.0F, 2.0F);
		this.targetDirectionDeg = this.baseDirectionDeg;
		this.targetStrength = this.baseStrength;
	}

	public void setTarget(float directionDeg, float strength) {
		this.targetDirectionDeg = normalizeDeg(directionDeg);
		this.targetStrength = Mth.clamp(strength, 0.0F, 2.0F);
	}

	public float getEffectiveDirectionDeg(long currentTick) {
		float shift = (this.currentGust != null) ? this.currentGust.getEffectiveShift(currentTick) : 0.0F;
		return normalizeDeg(this.baseDirectionDeg + shift);
	}

	public float getEffectiveStrength(long currentTick) {
		float mult = (this.currentGust != null) ? this.currentGust.getEffectiveMultiplier(currentTick) : 1.0F;
		return Math.max(0.0F, this.baseStrength * mult);
	}

	public WindVector getWindVector(long currentTick) {
		return new WindVector(this.getEffectiveDirectionDeg(currentTick), this.getEffectiveStrength(currentTick));
	}

	/**
	 * Backward-compatible overload for tests without time-of-day.
	 */
	public void tickServer(long currentTick, RandomSource random) {
		this.tickServer(currentTick, currentTick, random);
	}

	/**
	 * Ticks the wind simulation on the server using level time-of-day and world seed.
	 */
	public void tickServer(long currentTick, long dayTime, RandomSource random) {
		// Clean up expired gust
		if (this.currentGust != null && this.currentGust.isExpired(currentTick)) {
			this.currentGust = null;
		}

		long dayIndex = Math.floorDiv(dayTime, 24000L);
		int timeOfDay = (int) Math.floorMod(dayTime, 24000L);

		// Cache or retrieve current and next day profiles
		if (this.cachedCurrentProfile == null || dayIndex != this.cachedDayIndex) {
			this.cachedCurrentProfile = DailyWindProfile.getForDay(
				dayIndex,
				this.seed,
				this.hasWorldSeed ? null : this.initialDirectionDeg,
				this.hasWorldSeed ? null : this.initialStrength
			);
			this.cachedNextProfile = DailyWindProfile.getForDay(
				dayIndex + 1L,
				this.seed,
				null,
				null
			);
			this.cachedDayIndex = dayIndex;
		}

		DailyWindProfile pCurrent = this.cachedCurrentProfile;
		DailyWindProfile pNext = this.cachedNextProfile;

		// 1. Day transition blend (between tick 18000 and 24000 - late night into sunrise)
		float transition = (timeOfDay >= 18000) ? ((float) (timeOfDay - 18000) / 6000.0F) : 0.0F;
		float blend = transition * transition * (3.0F - 2.0F * transition);

		float dirDiff = Mth.wrapDegrees(pNext.prevailingDirectionDeg() - pCurrent.prevailingDirectionDeg());
		float macroPrevailingDir = normalizeDeg(pCurrent.prevailingDirectionDeg() + dirDiff * blend);
		float macroBaseStrength = Mth.lerp(blend, pCurrent.baseStrength(), pNext.baseStrength());
		float macroWanderRange = Mth.lerp(blend, pCurrent.wanderRangeDeg(), pNext.wanderRangeDeg());
		float macroGustChance = Mth.lerp(blend, pCurrent.gustChance(), pNext.gustChance());
		float macroGustMinMult = Mth.lerp(blend, pCurrent.gustMinMult(), pNext.gustMinMult());
		float macroGustMaxMult = Mth.lerp(blend, pCurrent.gustMaxMult(), pNext.gustMaxMult());
		float macroGustMaxShift = Mth.lerp(blend, pCurrent.gustMaxShiftDeg(), pNext.gustMaxShiftDeg());
		float macroNightFactor = Mth.lerp(blend, pCurrent.nightStrengthFactor(), pNext.nightStrengthFactor());
		int minGustDuration = (int) Mth.lerp(blend, pCurrent.gustMinDuration(), pNext.gustMinDuration());
		int maxGustDuration = (int) Mth.lerp(blend, pCurrent.gustMaxDuration(), pNext.gustMaxDuration());

		this.lastMacroPrevailingDir = macroPrevailingDir;

		// 2. Diurnal modulation (day vs night thermal effect)
		float diurnalFactor;
		float nightWeight;

		if (timeOfDay < 12000) {
			// Daytime: peak convection around midday (tick 6000)
			float s = Mth.sin(((float) timeOfDay / 12000.0F) * Mth.PI);
			diurnalFactor = 1.0F + 0.15F * s;
			nightWeight = 0.0F;
		} else if (timeOfDay < 14000) {
			// Dusk transition to night
			float u = (float) (timeOfDay - 12000) / 2000.0F;
			float smoothU = u * u * (3.0F - 2.0F * u);
			diurnalFactor = Mth.lerp(smoothU, 1.0F, macroNightFactor);
			nightWeight = smoothU;
		} else if (timeOfDay < 22000) {
			// Deep night
			diurnalFactor = macroNightFactor;
			nightWeight = 1.0F;
		} else {
			// Pre-dawn transition back to day
			float u = (float) (timeOfDay - 22000) / 2000.0F;
			float smoothU = u * u * (3.0F - 2.0F * u);
			diurnalFactor = Mth.lerp(smoothU, macroNightFactor, 1.0F);
			nightWeight = 1.0F - smoothU;
		}

		if (pCurrent.dayType() == DailyWindProfile.WindDayType.CALM && pNext.dayType() == DailyWindProfile.WindDayType.CALM) {
			diurnalFactor = Math.min(diurnalFactor, 1.0F);
		}

		this.lastNightWeight = nightWeight;
		this.lastDiurnalFactor = diurnalFactor;

		float effectiveMacroStrength = Math.max(0.0F, macroBaseStrength * diurnalFactor);

		// 3. Nighttime smoothness ("w nocy wieje rowniej")
		float effectiveGustChance = macroGustChance * (1.0F - 0.75F * nightWeight);
		float excessMin = macroGustMinMult - 1.0F;
		float excessMax = macroGustMaxMult - 1.0F;
		float nightGustDamp = 1.0F - 0.65F * nightWeight;
		float effectiveGustMinMult = 1.0F + excessMin * nightGustDamp;
		float effectiveGustMaxMult = 1.0F + excessMax * nightGustDamp;
		float effectiveGustMaxShift = macroGustMaxShift * (1.0F - 0.60F * nightWeight);
		float effectiveWanderRange = macroWanderRange * (1.0F - 0.50F * nightWeight);
		float effectiveDirChangeRate = WindAndSailsConfig.WIND_DIR_CHANGE_RATE * (1.0F - 0.50F * nightWeight);

		// 4. Direction wander & smooth progression
		float targetMacroDiff = Math.abs(Mth.wrapDegrees(this.targetDirectionDeg - macroPrevailingDir));
		if (targetMacroDiff > effectiveWanderRange + 4.0F) {
			float wander = (random.nextFloat() * 2.0F - 1.0F) * effectiveWanderRange;
			this.targetDirectionDeg = normalizeDeg(macroPrevailingDir + wander);
		}

		float diffDeg = Mth.wrapDegrees(this.targetDirectionDeg - this.baseDirectionDeg);
		if (Math.abs(diffDeg) > 0.01F) {
			float step = Math.signum(diffDeg) * Math.min(Math.abs(diffDeg), effectiveDirChangeRate);
			this.baseDirectionDeg = normalizeDeg(this.baseDirectionDeg + step);
		} else {
			float wander = (random.nextFloat() * 2.0F - 1.0F) * effectiveWanderRange;
			this.targetDirectionDeg = normalizeDeg(macroPrevailingDir + wander);
		}

		// 5. Strength wander & smooth progression
		float strTargetMacroDiff = Math.abs(this.targetStrength - effectiveMacroStrength);
		float allowedVariance = Math.max(0.005F, effectiveMacroStrength * 0.15F);
		if (strTargetMacroDiff > allowedVariance + 0.04F) {
			this.targetStrength = Math.max(0.0F, effectiveMacroStrength + (random.nextFloat() * 2.0F - 1.0F) * allowedVariance);
		}

		float strDiff = this.targetStrength - this.baseStrength;
		if (Math.abs(strDiff) > 0.001F) {
			float step = Math.signum(strDiff) * Math.min(Math.abs(strDiff), WindAndSailsConfig.WIND_SPEED_CHANGE_RATE);
			this.baseStrength = Math.max(0.0F, this.baseStrength + step);
		} else {
			this.targetStrength = Math.max(0.0F, effectiveMacroStrength + (random.nextFloat() * 2.0F - 1.0F) * allowedVariance);
		}

		// 6. Natural gust triggering
		if (this.currentGust == null && random.nextFloat() < effectiveGustChance) {
			int duration = minGustDuration + random.nextInt(Math.max(1, maxGustDuration - minGustDuration + 1));
			float shift = (random.nextBoolean() ? 1.0F : -1.0F) * (random.nextFloat() * effectiveGustMaxShift);
			float mult = effectiveGustMinMult + random.nextFloat() * Math.max(0.01F, effectiveGustMaxMult - effectiveGustMinMult);
			this.currentGust = new WindGust(currentTick, duration, shift, mult);
		}
	}

	/**
	 * Client-side smoothing towards latest server snapshot.
	 */
	public void tickClient(long currentTick) {
		if (this.currentGust != null && this.currentGust.isExpired(currentTick)) {
			this.currentGust = null;
		}

		float diffDeg = Mth.wrapDegrees(this.targetDirectionDeg - this.baseDirectionDeg);
		this.baseDirectionDeg = normalizeDeg(this.baseDirectionDeg + diffDeg * 0.15F);

		float strDiff = this.targetStrength - this.baseStrength;
		this.baseStrength = Math.max(0.0F, this.baseStrength + strDiff * 0.15F);
	}

	public static float normalizeDeg(float deg) {
		float d = Mth.wrapDegrees(deg);
		return d < 0.0F ? d + 360.0F : d;
	}
}
