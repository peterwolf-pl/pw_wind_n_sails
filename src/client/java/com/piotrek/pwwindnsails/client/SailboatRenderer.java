package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.SailboatEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.level.material.FogType;

public final class SailboatRenderer extends EntityRenderer<SailboatEntity, SailboatRenderState> {
	public static final Identifier TEXTURE = WindAndSailsMod.id("textures/entity/sailboat.png");

	private final SailboatModel model;
	private final Model.Simple waterMask;

	public SailboatRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new SailboatModel(context.bakeLayer(WindAndSailsClient.SAILBOAT_LAYER));
		this.waterMask = new Model.Simple(
			context.bakeLayer(WindAndSailsClient.SAILBOAT_WATER_PATCH),
			id -> RenderTypes.waterMask()
		);
		this.shadowRadius = 1.2F;
	}

	@Override
	public SailboatRenderState createRenderState() {
		return new SailboatRenderState();
	}

	@Override
	public void extractRenderState(SailboatEntity entity, SailboatRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yaw = entity.getYRot();
		state.heel = entity.getVisualHeelAngle(partialTick);
		state.boomAngle = entity.getVisualBoomAngle(partialTick);
		state.rudderAngle = entity.getVisualRudderAngle(partialTick);
		state.sailFurled = entity.isSailFurled();
		state.speed = (float) entity.getDeltaMovement().horizontalDistance();
		state.hurtTime = entity.getHurtTime();
		state.damage = entity.getDamage();
	}

	@Override
	public void submit(SailboatRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		stack.pushPose();
		stack.rotateDegrees(Axis.YP, 180.0F - state.yaw);

		// Cut the water surface out of the cockpit. Kept level, not heeled with the hull.
		if (camera.fogType != FogType.WATER) {
			stack.pushPose();
			stack.scale(-1.0F, -1.0F, 1.0F);
			stack.translate(0.0F, -1.5F, 0.0F);
			collector.submitModel(
				this.waterMask,
				Unit.INSTANCE,
				stack,
				TEXTURE,
				state.lightCoords,
				OverlayTexture.NO_OVERLAY,
				0
			);
			stack.popPose();
		}

		// Heel (roll) opposite the wind side
		stack.rotateDegrees(Axis.ZP, state.heel);

		// Hurt shake
		if (state.hurtTime > 0) {
			float wobble = Mth.sin(state.hurtTime) * 3.0F;
			stack.rotateDegrees(Axis.ZP, wobble);
		}

		stack.scale(-1.0F, -1.0F, 1.0F);
		stack.translate(0.0F, -1.5F, 0.0F);

		this.model.setupAnim(state);
		collector.submitModel(
			this.model,
			state,
			stack,
			RenderTypes.entityCutout(TEXTURE),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			state.outlineColor
		);
		stack.popPose();
	}
}
