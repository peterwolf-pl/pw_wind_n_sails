package com.piotrek.pwwindnsails;

/**
 * Centralized configuration constants for wind simulation, sailing physics,
 * boat dimensions, controls, and rendering.
 */
public final class WindAndSailsConfig {
	private WindAndSailsConfig() {}

	// --- Boat Dimensions ---
	public static final float BOAT_LENGTH = 3.0F;
	public static final float BOAT_WIDTH = 1.5F;
	public static final float BOAT_HEIGHT = 0.8F;
	public static final float SAIL_AREA = 5.0F;

	// --- Wind Simulation ---
	public static final float WIND_SPEED_MIN = 0.25F;
	public static final float WIND_SPEED_MAX = 0.85F;
	public static final float WIND_DIR_CHANGE_RATE = 0.15F; // Degrees per tick smooth wander
	public static final float WIND_SPEED_CHANGE_RATE = 0.001F;

	// Overworld prevailing wind alignment: Minecraft clouds drift east (+X, which in MC yaw is 270 or -90 deg).
	public static final float PREVAILING_WIND_DIR = 270.0F; // Facing +X
	public static final float PREVAILING_WANDER_RANGE = 45.0F;

	// --- Gusts ---
	public static final float GUST_TRIGGER_CHANCE = 0.004F; // ~Every 250 ticks (~12.5s) on average
	public static final int GUST_MIN_DURATION_TICKS = 40;   // 2 seconds
	public static final int GUST_MAX_DURATION_TICKS = 240;  // 12 seconds
	public static final float GUST_MIN_MULT = 1.10F;
	public static final float GUST_MAX_MULT = 1.40F;
	public static final float GUST_MAX_DIR_SHIFT_DEG = 18.0F;

	// --- Rig & Sheet ---
	public static final float MIN_BOOM_ANGLE_DEG = 6.0F;
	public static final float MAX_BOOM_ANGLE_DEG = 88.0F; // fully eased boom trails wind line
	public static final float SHEET_CHANGE_RATE = 0.04F; // per tick when W/S held

	// --- Rudder (A / D Steering, Configurable via /windsails command) ---
	public static float maxRudderAngleDeg = 32.0F;
	public static float rudderRatePerTick = 2.2F;       // Rate rudder turns when A/D held (deg/tick)
	public static float rudderAutoCenterRate = 2.5F;    // Rate rudder returns to 0 when released (deg/tick)
	public static float rudderForceCoeff = 0.045F;      // Turning torque
	public static float rudderSensitivity = 1.0F;       // Global command-tunable multiplier
	public static float rudderMinEffectiveSpeed = 0.010F;
	public static float rudderDragCoeff = 0.025F;

	// --- Aerodynamics (Lift & Drag) ---
	public static final float NO_GO_ZONE_DEG = 40.0F;
	public static final float OPTIMAL_AOA_DEG = 15.0F;
	public static final float LIFT_COEFF_PEAK = 1.45F;
	public static final float DRAG_COEFF_MIN = 0.08F;
	public static final float DRAG_COEFF_DOWNWIND = 0.95F;
	public static final float SAIL_FORCE_SCALE = 0.052F;

	// --- Hydrodynamics (Water Resistance & Keel) ---
	// Realistic water drag values: preserves forward momentum during tacks (zwrot przez sztag)
	public static final float WATER_LINEAR_DRAG = 0.005F;
	public static final float WATER_QUAD_DRAG = 0.015F;
	public static final float LATERAL_SLIP_DAMPING = 0.90F; // 90% sideways drift damped per tick
	public static final float YAW_ANGULAR_DAMPING = 0.88F;  // Angular momentum carrying boat through turns

	// --- Heel (Roll) ---
	public static final float MAX_HEEL_DEG = 32.0F;
	public static final float HEEL_SENSITIVITY = 160.0F; // Realistic dynamic heel from sail lateral pressure
	public static final float HEEL_LERP_FACTOR = 0.15F;

	// --- Wind Visualizer ---
	public static final float VISUALIZER_RADIUS = 28.0F;
	public static final float VISUALIZER_ARROW_HEIGHT_OFFSET = 0.06F;
}
