package com.piotrek.pwwindnsails.wind;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * State of wind for a single world/dimension.
 * Maintains smooth base evolution and natural gust variation.
 */
public final class WindState {
	private float baseDirectionDeg;
	private float baseStrength;
	private float targetDirectionDeg;
	private float targetStrength;
	@Nullable
	private WindGust currentGust;

	public WindState(float initialDirectionDeg, float initialStrength) {
		this.baseDirectionDeg = Mth.wrapDegrees(initialDirectionDeg);
		if (this.baseDirectionDeg < 0.0F) {
			this.baseDirectionDeg += 360.0F;
		}
		this.baseStrength = Mth.clamp(initialStrength, WindAndSailsConfig.WIND_SPEED_MIN, WindAndSailsConfig.WIND_SPEED_MAX);
		this.targetDirectionDeg = this.baseDirectionDeg;
		this.targetStrength = this.baseStrength;
	}

	public static WindState createDefault() {
		return new WindState(WindAndSailsConfig.PREVAILING_WIND_DIR, 0.50F);
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
	public WindGust getCurrentGust() {
		return this.currentGust;
	}

	public void setCurrentGust(@Nullable WindGust gust) {
		this.currentGust = gust;
	}

	public void setBase(float directionDeg, float strength) {
		this.baseDirectionDeg = normalizeDeg(directionDeg);
		this.baseStrength = Mth.clamp(strength, 0.05F, 2.0F);
		this.targetDirectionDeg = this.baseDirectionDeg;
		this.targetStrength = this.baseStrength;
	}

	public void setTarget(float directionDeg, float strength) {
		this.targetDirectionDeg = normalizeDeg(directionDeg);
		this.targetStrength = Mth.clamp(strength, 0.05F, 2.0F);
	}

	public float getEffectiveDirectionDeg(long currentTick) {
		float shift = (this.currentGust != null) ? this.currentGust.getEffectiveShift(currentTick) : 0.0F;
		return normalizeDeg(this.baseDirectionDeg + shift);
	}

	public float getEffectiveStrength(long currentTick) {
		float mult = (this.currentGust != null) ? this.currentGust.getEffectiveMultiplier(currentTick) : 1.0F;
		return this.baseStrength * mult;
	}

	public WindVector getWindVector(long currentTick) {
		return new WindVector(this.getEffectiveDirectionDeg(currentTick), this.getEffectiveStrength(currentTick));
	}

	/**
	 * Ticks the wind simulation on the server.
	 */
	public void tickServer(long currentTick, RandomSource random) {
		// Clean up expired gust
		if (this.currentGust != null && this.currentGust.isExpired(currentTick)) {
			this.currentGust = null;
		}

		// Smoothly interpolate base direction towards target
		float diffDeg = Mth.wrapDegrees(this.targetDirectionDeg - this.baseDirectionDeg);
		if (Math.abs(diffDeg) > 0.01F) {
			float step = Math.signum(diffDeg) * Math.min(Math.abs(diffDeg), WindAndSailsConfig.WIND_DIR_CHANGE_RATE);
			this.baseDirectionDeg = normalizeDeg(this.baseDirectionDeg + step);
		} else {
			// Pick new target direction within prevailing wander range
			float wander = (random.nextFloat() * 2.0F - 1.0F) * WindAndSailsConfig.PREVAILING_WANDER_RANGE;
			this.targetDirectionDeg = normalizeDeg(WindAndSailsConfig.PREVAILING_WIND_DIR + wander);
		}

		// Smoothly interpolate base strength towards target
		float strDiff = this.targetStrength - this.baseStrength;
		if (Math.abs(strDiff) > 0.002F) {
			float step = Math.signum(strDiff) * Math.min(Math.abs(strDiff), WindAndSailsConfig.WIND_SPEED_CHANGE_RATE);
			this.baseStrength = Mth.clamp(this.baseStrength + step, WindAndSailsConfig.WIND_SPEED_MIN, WindAndSailsConfig.WIND_SPEED_MAX);
		} else {
			// Pick new target strength
			this.targetStrength = WindAndSailsConfig.WIND_SPEED_MIN +
				random.nextFloat() * (WindAndSailsConfig.WIND_SPEED_MAX - WindAndSailsConfig.WIND_SPEED_MIN);
		}

		// Trigger new gust if none is currently active
		if (this.currentGust == null && random.nextFloat() < WindAndSailsConfig.GUST_TRIGGER_CHANCE) {
			int duration = WindAndSailsConfig.GUST_MIN_DURATION_TICKS +
				random.nextInt(WindAndSailsConfig.GUST_MAX_DURATION_TICKS - WindAndSailsConfig.GUST_MIN_DURATION_TICKS + 1);
			float shift = (random.nextBoolean() ? 1.0F : -1.0F) *
				(8.0F + random.nextFloat() * (WindAndSailsConfig.GUST_MAX_DIR_SHIFT_DEG - 8.0F));
			float mult = WindAndSailsConfig.GUST_MIN_MULT +
				random.nextFloat() * (WindAndSailsConfig.GUST_MAX_MULT - WindAndSailsConfig.GUST_MIN_MULT);
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
		this.baseStrength += strDiff * 0.15F;
	}

	public static float normalizeDeg(float deg) {
		float d = Mth.wrapDegrees(deg);
		return d < 0.0F ? d + 360.0F : d;
	}
}
