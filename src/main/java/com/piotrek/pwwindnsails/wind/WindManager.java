package com.piotrek.pwwindnsails.wind;

import com.piotrek.pwwindnsails.network.WindSyncPayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * World-level wind manager providing wind conditions for both server logic
 * (boat physics, gusts, synchronization) and client rendering.
 */
public final class WindManager {
	private static final WindManager INSTANCE = new WindManager();

	private final Map<ResourceKey<Level>, WindState> serverStates = new ConcurrentHashMap<>();
	private final WindState clientState = WindState.createDefault();

	private WindManager() {}

	public static WindManager getInstance() {
		return INSTANCE;
	}

	public WindState getClientState() {
		return this.clientState;
	}

	public WindState getWindState(Level level) {
		if (level.isClientSide()) {
			return this.clientState;
		}
		return this.serverStates.computeIfAbsent(level.dimension(), dim -> {
			WindState state = WindState.createDefault();
			if (level instanceof ServerLevel sl) {
				state.setSeed(sl.getSeed() ^ (long) dim.identifier().hashCode());
			}
			return state;
		});
	}

	public WindVector getWind(Level level, Vec3 pos) {
		return this.getWind(level);
	}

	public WindVector getWind(Level level) {
		long gameTime = level.getGameTime();
		return this.getWindState(level).getWindVector(gameTime);
	}

	public void tickServer(ServerLevel level) {
		long tick = level.getGameTime();
		long dayTime = level.getDefaultClockTime();
		WindState state = this.getWindState(level);
		if (!state.hasWorldSeed()) {
			state.setSeed(level.getSeed() ^ (long) level.dimension().identifier().hashCode());
		}
		WindGust beforeGust = state.getCurrentGust();

		state.tickServer(tick, dayTime, level.getRandom());

		WindGust afterGust = state.getCurrentGust();
		boolean gustChanged = (beforeGust != afterGust);

		// Synchronize periodically every 20 ticks (1 sec) or immediately when a gust starts/ends
		if (tick % 20 == 0 || gustChanged) {
			WindSyncPayload payload = createPayload(state, tick, dayTime);
			for (ServerPlayer player : PlayerLookup.level(level)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public void syncToPlayer(ServerPlayer player) {
		ServerLevel level = player.level();
		long tick = level.getGameTime();
		long dayTime = level.getDefaultClockTime();
		WindState state = this.getWindState(level);
		ServerPlayNetworking.send(player, createPayload(state, tick, dayTime));
	}

	public void applyClientSync(WindSyncPayload payload) {
		this.clientState.setTarget(payload.targetDirection(), payload.targetStrength());
		this.clientState.setBase(payload.baseDirection(), payload.baseStrength());
		this.clientState.setForecast(payload.forecast());
		com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects = payload.environmentalEffects();

		if (payload.gustActive()) {
			this.clientState.setCurrentGust(new WindGust(
				payload.gustStartTick(),
				payload.gustDurationTicks(),
				payload.gustShiftDeg(),
				payload.gustStrengthMult()
			));
		} else {
			this.clientState.setCurrentGust(null);
		}
	}

	public void tickClient(long gameTime) {
		this.clientState.tickClient(gameTime);
	}

	private static WindSyncPayload createPayload(WindState state, long currentTick, long dayTime) {
		WindGust gust = state.getCurrentGust();
		boolean gustActive = gust != null && !gust.isExpired(currentTick);
		DailyWindProfile pCurrent = state.getCurrentDayProfile();
		DailyWindProfile pNext = state.getNextDayProfile();
		String forecast = (pCurrent != null && pNext != null)
			? DailyWindProfile.generateForecast(dayTime, pCurrent, pNext)
			: "Stabilna pogoda";

		return new WindSyncPayload(
			state.getBaseDirectionDeg(),
			state.getBaseStrength(),
			state.getTargetDirectionDeg(),
			state.getTargetStrength(),
			gustActive,
			gustActive ? gust.getStartTick() : 0L,
			gustActive ? gust.getDurationTicks() : 0,
			gustActive ? gust.getDirectionShiftDeg() : 0.0F,
			gustActive ? gust.getStrengthMultiplier() : 1.0F,
			currentTick,
			forecast,
			com.piotrek.pwwindnsails.WindAndSailsConfig.environmentalWindEffects
		);
	}
}
