package com.piotrek.pwwindnsails.client.mixin;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin {
	@Shadow
	protected ClientLevel level;
	@Shadow
	protected double xd;
	@Shadow
	protected double zd;

	@Inject(method = "tick", at = @At("HEAD"))
	private void pwwindnsails$applyWindToCampfireSmoke(CallbackInfo ci) {
		if (!WindAndSailsConfig.environmentalWindEffects || this.level == null) {
			return;
		}

		WindVector wind = WindManager.getInstance().getWind(this.level);
		float str = wind.strength();
		if (str > 0.02F) {
			// Accelerate campfire smoke downwind along true wind vector
			double targetXd = wind.x() * 0.18;
			double targetZd = wind.z() * 0.18;
			this.xd = Mth.lerp(0.08, this.xd, targetXd);
			this.zd = Mth.lerp(0.08, this.zd, targetZd);
		}
	}
}
