package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.client.compat.xaero.BuoyTracker;
import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class BuoyRenderer extends EntityRenderer<BuoyEntity, BuoyRenderState> {
	public static final Identifier TEXTURE_RED = WindAndSailsMod.id("textures/entity/buoy_red.png");
	public static final Identifier TEXTURE_BLUE = WindAndSailsMod.id("textures/entity/buoy_blue.png");

	private final BuoyModel model;

	public BuoyRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new BuoyModel(context.bakeLayer(WindAndSailsClient.BUOY_LAYER));
		this.shadowRadius = 0.45F;
	}

	@Override
	public BuoyRenderState createRenderState() {
		return new BuoyRenderState();
	}

	@Override
	public void extractRenderState(BuoyEntity entity, BuoyRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.buoyType = entity.getBuoyType();
		state.id = entity.getId();
		state.texture = entity.getBuoyType() == BuoyEntity.TYPE_BLUE_CYLINDRICAL ? TEXTURE_BLUE : TEXTURE_RED;
		state.hasAnchor = entity.getAnchorBedPos().isPresent();
		state.anchorBedPos = entity.getAnchorBedPos().orElse(null);

		BuoyTracker.track(entity);
	}

	@Override
	public void submit(BuoyRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, stack, collector, camera);

		stack.pushPose();
		stack.scale(-1.0F, -1.0F, 1.0F);
		stack.translate(0.0F, -1.5F, 0.0F);

		this.model.setupAnim(state);
		collector.submitModel(
			this.model,
			state,
			stack,
			RenderTypes.entityCutout(state.texture),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			state.outlineColor
		);
		stack.popPose();
	}
}
