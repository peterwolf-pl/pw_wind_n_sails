package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.entity.SailboatEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;

/**
 * Helper to safely detect if the player is currently flying a paraglider
 * or operating an aircraft from PeterWolf's Planes, without hard compile-time dependencies.
 */
public final class PlaneCompatHelper {
	private static Method isParagliderMethod;
	private static boolean paragliderChecked = false;

	private PlaneCompatHelper() {}

	public static boolean isPlayerInPlaneOrParagliding(Player player) {
		if (player == null) {
			return false;
		}

		// Check if riding an aircraft (plane/helicopter)
		Entity vehicle = player.getVehicle();
		if (vehicle != null && !(vehicle instanceof SailboatEntity)) {
			String name = vehicle.getClass().getName().toLowerCase();
			if (name.contains("plane") || name.contains("helicopter") || name.contains("aircraft")) {
				return true;
			}
		}

		// Check if actively paragliding via peterwolfs-planes
		if (!paragliderChecked) {
			paragliderChecked = true;
			try {
				Class<?> clazz = Class.forName("com.piotrek.peterwolfsplanes.client.PeterwolfsPlanesClient");
				isParagliderMethod = clazz.getMethod("isParagliderVisuallyDeployed", int.class);
			} catch (Throwable ignored) {
				isParagliderMethod = null;
			}
		}

		if (isParagliderMethod != null) {
			try {
				return (boolean) isParagliderMethod.invoke(null, player.getId());
			} catch (Throwable ignored) {}
		}

		return false;
	}
}
