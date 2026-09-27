package com.piotrek.pwwindnsails.client.mixin;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingParticle.class)
public abstract class FallingParticleMixin {
	@Shadow
	protected ClientLevel level;
	@Shadow
	protected double xd;
	@Shadow
	protected double zd;

	@Inject(method = "tick", at = @At("HEAD"))
	private void pwwindnsails$applyWindToFallingLeaves(CallbackInfo ci) {
		if (!WindAndSailsConfig.environmentalWindEffects || this.level == null) {
			return;
		}

		WindVector wind = WindManager.getInstance().getWind(this.level);
		float str = wind.strength();
		if (str > 0.02F) {
			// Blow falling cherry, pale oak, and poplar leaves downwind
			this.xd += wind.x() * 0.045;
			this.zd += wind.z() * 0.045;
		}
	}
}
