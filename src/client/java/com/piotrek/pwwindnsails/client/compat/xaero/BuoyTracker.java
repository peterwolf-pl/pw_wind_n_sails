package com.piotrek.pwwindnsails.client.compat.xaero;

import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuoyTracker {
	public record BuoyMapTarget(
		UUID uuid,
		ResourceKey<Level> dimension,
		Vec3 pos,
		int type,
		@Nullable Component name,
		boolean loaded,
		long lastSeenMs
	) {
		public String getDisplayName() {
			if (name != null) {
				return name.getString();
			}
			return type == BuoyEntity.TYPE_BLUE_CYLINDRICAL ? "Boja cylindryczna" : "Boja sferyczna";
		}
	}

	private static final Map<UUID, BuoyMapTarget> TARGETS = new ConcurrentHashMap<>();

	private BuoyTracker() {}

	public static void track(BuoyEntity buoy) {
		if (buoy.level() == null || buoy.isRemoved()) {
			return;
		}
		ResourceKey<Level> dim = buoy.level().dimension();
		Component name = buoy.hasCustomName() ? buoy.getCustomName() : null;
		TARGETS.put(buoy.getUUID(), new BuoyMapTarget(
			buoy.getUUID(),
			dim,
			buoy.position(),
			buoy.getBuoyType(),
			name,
			true,
			System.currentTimeMillis()
		));
	}

	public static void untrack(BuoyEntity buoy) {
		TARGETS.remove(buoy.getUUID());
	}

	public static void tick(Minecraft client) {
		if (client.level == null) {
			TARGETS.clear();
			return;
		}
		ResourceKey<Level> currentDim = client.level.dimension();

		// Refresh state of tracked buoys
		long now = System.currentTimeMillis();
		for (Map.Entry<UUID, BuoyMapTarget> entry : TARGETS.entrySet()) {
			BuoyMapTarget target = entry.getValue();
			if (!target.dimension().equals(currentDim)) {
				continue;
			}
			// If not seen loaded for 5 minutes and far away, keep it as persistent waypoint
			// Only remove if explicitly removed by player in range
		}
	}

	public static List<BuoyMapTarget> getTargetsForDimension(ResourceKey<Level> dimension) {
		List<BuoyMapTarget> result = new ArrayList<>();
		for (BuoyMapTarget target : TARGETS.values()) {
			if (target.dimension().equals(dimension)) {
				result.add(target);
			}
		}
		return result;
	}
}
