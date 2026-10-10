package com.piotrek.pwwindnsails.client.mixin;

import com.piotrek.pwwindnsails.entity.SailboatEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL")
	)
	private void pwwindnsails$orientPassengerToCenter(
		Avatar entity,
		AvatarRenderState state,
		float partialTick,
		CallbackInfo ci
	) {
		if (entity.getVehicle() instanceof SailboatEntity boat) {
			if (boat.isHikeStanding()) {
				state.isPassenger = false; // Stand upright on gunwale
			}
			// Torso is always facing inward towards the center of the boat!
			// Starboard gunwale (hike >= 0) -> face left towards center (boatYaw - 90)
			// Port gunwale (hike < 0) -> face right towards center (boatYaw + 90)
			float boatYaw = boat.getYRot(partialTick);
			float hike = boat.getHike();
			float targetBodyRot = (hike >= 0) ? (boatYaw - 90.0F) : (boatYaw + 90.0F);
			state.bodyRot = targetBodyRot;
		}
	}
}
