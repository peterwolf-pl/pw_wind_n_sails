package com.piotrek.pwwindnsails.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.pwwindnsails.WindAndSailsMod;
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
		Level level = blockEntity.getLevel();
		if (level != null) {
			WindVector wind = WindManager.getInstance().getWind(level, Vec3.atCenterOf(blockEntity.getBlockPos()));
			state.windDirectionDeg = wind.directionDeg();
			state.windStrength = wind.strength();
			state.gameTime = level.getGameTime() + partialTicks;
		}
	}

	@Override
	public void submit(
		FlagpoleRenderState state,
		PoseStack stack,
		SubmitNodeCollector collector,
		CameraRenderState camera
	) {
		stack.pushPose();
		// Translate to the masthead near the finial (top of 4-block pole)
		stack.translate(0.5D, 0.88D, 0.5D);

		// Align yaw with world wind direction (streams leeward with wind)
		stack.rotateDegrees(Axis.YP, 270.0F - state.windDirectionDeg);

		// Droop / Hang down according to wind strength:
		// Calm wind (S=0) -> hangs ~78° down like a limp rag on a stick.
		// Strong wind (S>=0.65) -> straightens horizontally (~1°).
		float windFactor = Mth.clamp(state.windStrength / 0.65F, 0.0F, 1.0F);
		float droopPitch = Mth.lerp(windFactor, 78.0F, 1.0F);
		stack.rotateDegrees(Axis.ZP, -droopPitch);

		// Scale: 1 unit in ModelPart = 1/16 block
		stack.scale(1.0F / 16.0F, -1.0F / 16.0F, 1.0F / 16.0F);

		this.model.setupAnim(state.windStrength, state.gameTime);

		collector.submitModelPart(
			this.model.getRoot(),
			stack,
			RenderTypes.entityCutout(TEXTURE),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			null
		);

		stack.popPose();
	}
}
