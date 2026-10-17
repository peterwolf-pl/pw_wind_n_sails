package com.piotrek.pwwindnsails.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Non-blocking chunk check. {@code getChunk(..., false)} still waits on the server
 * thread and generates terrain, which freezes world load.
 */
public final class ChunkReady {
	private ChunkReady() {}

	public static boolean is(Level level, int blockX, int blockZ) {
		if (level == null) {
			return false;
		}
		return level.getChunkSource().getChunkNow(blockX >> 4, blockZ >> 4) != null;
	}

	public static boolean is(Level level, BlockPos pos) {
		return pos != null && is(level, pos.getX(), pos.getZ());
	}
}
