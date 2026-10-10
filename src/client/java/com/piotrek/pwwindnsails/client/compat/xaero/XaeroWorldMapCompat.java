package com.piotrek.pwwindnsails.client.compat.xaero;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import xaero.map.WorldMap;

public final class XaeroWorldMapCompat {
	private static boolean registered = false;

	private XaeroWorldMapCompat() {}

	public static boolean tryRegister() {
		if (registered) {
			return true;
		}
		try {
			if (WorldMap.mapElementRenderHandler == null) {
				return false;
			}
			WorldMap.mapElementRenderHandler.add(new BuoyWorldMapRenderer());
			registered = true;
			WindAndSailsMod.LOGGER.info("Successfully registered Peterwolf's BuoyWorldMapRenderer with Xaero's World Map!");
			return true;
		} catch (Throwable t) {
			WindAndSailsMod.LOGGER.warn("Failed to register BuoyWorldMapRenderer with Xaero's World Map", t);
			return false;
		}
	}
}
