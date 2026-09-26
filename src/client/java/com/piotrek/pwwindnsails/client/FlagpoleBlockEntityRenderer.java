package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.pwwindnsails.WindAndSailsMod;
import com.piotrek.pwwindnsails.block.FlagpoleBlock;
import com.piotrek.pwwindnsails.block.entity.FlagpoleBlockEntity;
import com.piotrek.pwwindnsails.wind.WindManager;
import com.piotrek.pwwindnsails.wind.WindVector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class FlagpoleBlockEntityRenderer implements BlockEntityRenderer<FlagpoleBlockEntity, FlagpoleRenderState> {
	public static final Identifier TEXTURE = WindAndSailsMod.id("textures/entity/flag.png");

	private final FlagModel model;

	public FlagpoleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new FlagModel(context.bakeLayer(WindAndSailsClient.FLAGPOLE_FLAG_LAYER));
	}

	@Override
	public FlagpoleRenderState createRenderState() {
		return new FlagpoleRenderState();
	}

	@Override
	public void extractRenderState(
		FlagpoleBlockEntity blockEntity,
		FlagpoleRenderState state,
		float partialTicks,
		Vec3 cameraPosition,
		@Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		state.isTopPart = blockEntity.getBlockState().getValue(FlagpoleBlock.PART) == FlagpoleBlock.TOTAL_PARTS - 1;
		Level level = blockEntity.getLevel();
		if (level != null && state.isTopPart) {
			WindVector wind = WindManager.getInstance().getWind(level, Vec3.atCenterOf(blockEntity.getBlockPos()));
			state.windDirectionDeg = wind.directionDeg();
			state.windStrength = wind.strength();
			state.gameTime = level.getGameTime() + partialTicks;
			state.tint = blockEntity.getColor().getTextureDiffuseColor();
		}
	}

	@Override
	public void submit(
		FlagpoleRenderState state,
		PoseStack stack,
		SubmitNodeCollector collector,
		CameraRenderState camera
	) {
		// Only the masthead (part 6) draws the cloth. Returning true from
		// shouldRenderOffScreen() skips Sodium's normal block-entity pass.
		if (!state.isTopPart) {
			return;
		}

		stack.pushPose();
		// Hoist at the top of the plain pole.
		stack.translate(0.5D, 0.98D, 0.5D);

		// Stream downwind. Yaw 0 in this space is +X, which is world yaw 270.
		stack.rotateDegrees(Axis.YP, 270.0F - state.windDirectionDeg);

		// Light wind keeps the hoist panel only slightly down, so the outer
		// panels can fall to half the previous panel's height. Strong wind flies flat.
		float windFactor = Mth.clamp((state.windStrength - 0.18F) / 0.40F, 0.0F, 1.0F);
		float droopPitch = Mth.lerp(windFactor, 18.0F, 1.0F);
		stack.rotateDegrees(Axis.ZP, -droopPitch);

		this.model.setupAnim(state);

		collector.submitModel(
			this.model,
			state,
			stack,
			RenderTypes.entityCutout(TEXTURE),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			state.tint,
			null,
			0
		);

		stack.popPose();
	}
}
