package com.piotrek.pwwindnsails.wind;

import net.minecraft.util.Mth;

/**
 * Represents a transient natural wind gust with smooth beginning, peak, and decay.
 */
public final class WindGust {
	private final long startTick;
	private final int durationTicks;
	private final float directionShiftDeg;
	private final float strengthMultiplier;

	public WindGust(long startTick, int durationTicks, float directionShiftDeg, float strengthMultiplier) {
		this.startTick = startTick;
		this.durationTicks = Math.max(1, durationTicks);
		this.directionShiftDeg = directionShiftDeg;
		this.strengthMultiplier = strengthMultiplier;
	}

	public long getStartTick() {
		return this.startTick;
	}

	public int getDurationTicks() {
		return this.durationTicks;
	}

	public float getDirectionShiftDeg() {
		return this.directionShiftDeg;
	}

	public float getStrengthMultiplier() {
		return this.strengthMultiplier;
	}

	public boolean isExpired(long currentTick) {
		return currentTick >= (this.startTick + this.durationTicks);
	}

	/**
	 * Returns the normalized bell-curve intensity of the gust at currentTick [0.0, 1.0].
	 * Reaches 1.0 at the peak (midway through duration), smooth zero at start and finish.
	 */
	public float getIntensity(long currentTick) {
		if (currentTick < this.startTick || this.isExpired(currentTick)) {
			return 0.0F;
		}
		float u = (float) (currentTick - this.startTick) / (float) this.durationTicks;
		float sin = Mth.sin(u * Mth.PI);
		return sin * sin; // Smooth C1 continuity at beginning and end
	}

	public float getEffectiveShift(long currentTick) {
		return this.directionShiftDeg * this.getIntensity(currentTick);
	}

	public float getEffectiveMultiplier(long currentTick) {
		float intensity = this.getIntensity(currentTick);
		return 1.0F + (this.strengthMultiplier - 1.0F) * intensity;
	}
}
