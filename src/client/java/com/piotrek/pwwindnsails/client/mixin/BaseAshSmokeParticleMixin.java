package com.piotrek.pwwindnsails.client.mixin;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseAshSmokeParticle.class)
public abstract class BaseAshSmokeParticleMixin {
	@Shadow
	protected ClientLevel level;
	@Shadow
	protected double xd;
	@Shadow
	protected double zd;

	@Inject(method = "tick", at = @At("HEAD"))
	private void pwwindnsails$applyWindToAshSmoke(CallbackInfo ci) {
		if (!WindAndSailsConfig.environmentalWindEffects || this.level == null) {
			return;
		}

		WindVector wind = WindManager.getInstance().getWind(this.level);
		float str = wind.strength();
		if (str > 0.02F) {
			// Drift furnace, torch, and fire smoke downwind
			this.xd += wind.x() * 0.018;
			this.zd += wind.z() * 0.018;
		}
	}
}
