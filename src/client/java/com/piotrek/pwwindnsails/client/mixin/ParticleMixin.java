package com.piotrek.pwwindnsails.client.mixin;

import com.piotrek.pwwindnsails.WindAndSailsConfig;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.FallingParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Particle.class)
public abstract class ParticleMixin {
	@Shadow
	protected ClientLevel level;

	@ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private double pwwindnsails$applyWindX(double dx) {
		if (!WindAndSailsConfig.environmentalWindEffects || this.level == null) {
			return dx;
		}
		float factor = pwwindnsails$getWindSensitivity();
		if (factor <= 0.0F) {
			return dx;
		}
		WindVector wind = WindManager.getInstance().getWind(this.level);
		return dx + wind.x() * factor;
	}

	@ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
	private double pwwindnsails$applyWindZ(double dz) {
		if (!WindAndSailsConfig.environmentalWindEffects || this.level == null) {
			return dz;
		}
		float factor = pwwindnsails$getWindSensitivity();
		if (factor <= 0.0F) {
			return dz;
		}
		WindVector wind = WindManager.getInstance().getWind(this.level);
		return dz + wind.z() * factor;
	}

	@Unique
	private float pwwindnsails$getWindSensitivity() {
		Object self = this;
		if (self instanceof CampfireSmokeParticle) {
			return 0.14F;
		}
		if (self instanceof BaseAshSmokeParticle) {
			return 0.045F;
		}
		if (self instanceof FallingParticle) {
			return 0.065F;
		}
		return 0.0F;
	}
}
