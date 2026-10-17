package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.entity.AnchorEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class AnchorRenderer extends EntityRenderer<AnchorEntity, AnchorRenderState> {
	public static final Identifier TEXTURE = WindAndSailsMod.id("textures/entity/anchor.png");

	private final AnchorModel model;

	public AnchorRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new AnchorModel(context.bakeLayer(WindAndSailsClient.ANCHOR_LAYER));
		this.shadowRadius = 0.4F;
	}

	@Override
	public AnchorRenderState createRenderState() {
		return new AnchorRenderState();
	}

	@Override
	public void extractRenderState(AnchorEntity entity, AnchorRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.anchored = entity.isAnchored();
	}

	@Override
	public void submit(AnchorRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, stack, collector, camera);

		stack.pushPose();
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
			state.outlineColor,
			null
		);
		stack.popPose();
	}
}
