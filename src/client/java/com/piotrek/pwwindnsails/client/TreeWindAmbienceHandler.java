package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Simulates wind blowing through tree canopies:
 * dislodging foliage leaves that stream downwind and creating ambient canopy rustle sounds.
 */
public final class TreeWindAmbienceHandler {
	private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

	private TreeWindAmbienceHandler() {}

	public static void tick(Minecraft client) {
		if (!WindAndSailsConfig.environmentalWindEffects) {
			return;
		}

		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			return;
		}

		WindVector wind = WindManager.getInstance().getWind(level, player.position());
		float windStr = wind.strength();
		if (windStr < 0.10F) {
			// Flauta / calm doldrums: leaves stay still on branches
			return;
		}

		RandomSource random = level.getRandom();
		int playerX = Mth.floor(player.getX());
		int playerY = Mth.floor(player.getY());
		int playerZ = Mth.floor(player.getZ());

		// Spawn rate scales with wind strength and gusts:
		// 0.15 wind -> ~1 attempt/tick
		// 0.50 wind -> ~4-6 attempts/tick
		// 0.85+ wind/gust -> ~10-14 attempts/tick
		int attempts = Math.max(1, Math.round(windStr * 12.0F));

		for (int i = 0; i < attempts; i++) {
			int dx = random.nextInt(37) - 18;
			int dz = random.nextInt(37) - 18;
			int dy = random.nextInt(21) - 4;

			POS.set(playerX + dx, playerY + dy, playerZ + dz);
			BlockState state = level.getBlockState(POS);
			if (state.is(BlockTags.LEAVES)) {
				// Check if there is air below for the leaf to peel off and blow away
				BlockPos below = POS.below();
				if (level.getBlockState(below).isAir()) {
					int tint = level.getClientLeafTintColor(POS);
					double px = POS.getX() + random.nextDouble();
					double py = POS.getY() - 0.05D;
					double pz = POS.getZ() + random.nextDouble();

					// Initial leaf velocity: carried by wind vector + slight gravity sink
					double speedMult = 0.06D + random.nextDouble() * 0.08D;
					double vx = wind.x() * speedMult;
					double vy = -0.015D - random.nextDouble() * 0.02D;
					double vz = wind.z() * speedMult;

					level.addParticle(
						ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, tint),
						px, py, pz,
						vx, vy, vz
					);

					// Ambient canopy rustle sound in moderate to strong wind
					if (windStr > 0.40F && random.nextFloat() < 0.012F) {
						float pitch = 0.8F + random.nextFloat() * 0.4F;
						float volume = Mth.clamp((windStr - 0.3F) * 0.6F, 0.1F, 0.6F);
						level.playLocalSound(
							POS.getX() + 0.5, POS.getY() + 0.5, POS.getZ() + 0.5,
							SoundEvents.CHERRY_LEAVES_FALL,
							SoundSource.AMBIENT,
							volume,
							pitch,
							false
						);
					}
				}
			}
		}
	}
}
