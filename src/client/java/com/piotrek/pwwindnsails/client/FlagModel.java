package com.piotrek.pwwindnsails.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 1 block high x 2 blocks long x 1/10 block thick wind flag with 3 articulated waving segments.
 */
public final class FlagModel extends Model<FlagpoleRenderState> {
	private final ModelPart seg1;
	private final ModelPart seg2;
	private final ModelPart seg3;

	public FlagModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
		this.seg1 = root.getChild("seg1");
		this.seg2 = this.seg1.getChild("seg2");
		this.seg3 = this.seg2.getChild("seg3");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Segment 1 (at mast hoist): length 10, height 16, thickness 1.6
		PartDefinition seg1 = root.addOrReplaceChild("seg1", CubeListBuilder.create()
			.texOffs(0, 0).addBox(0.0F, 0.0F, -0.8F, 10.0F, 16.0F, 1.6F),
			PartPose.ZERO
		);

		// Segment 2 (middle): length 11, height 16, thickness 1.6
		PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create()
			.texOffs(26, 0).addBox(0.0F, 0.0F, -0.8F, 11.0F, 16.0F, 1.6F),
			PartPose.offset(10.0F, 0.0F, 0.0F)
		);

		// Segment 3 (tail / fly): length 11, height 16, thickness 1.6
		seg2.addOrReplaceChild("seg3", CubeListBuilder.create()
			.texOffs(54, 0).addBox(0.0F, 0.0F, -0.8F, 11.0F, 16.0F, 1.6F),
			PartPose.offset(11.0F, 0.0F, 0.0F)
		);

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(FlagpoleRenderState state) {
		super.setupAnim(state);
		float windFactor = Mth.clamp(state.windStrength / 0.65F, 0.0F, 1.0F);
		float waveSpeed = 0.25F + state.windStrength * 0.45F;
		float phase = state.gameTime * waveSpeed;

		// Aerodynamic wave propagating along the flag's length
		this.seg1.yRot = Mth.sin(phase) * 0.07F * windFactor;
		this.seg2.yRot = Mth.sin(phase - 1.2F) * 0.18F * windFactor;
		this.seg3.yRot = Mth.sin(phase - 2.4F) * 0.32F * windFactor;

		this.seg2.zRot = Mth.cos(phase - 0.8F) * 0.04F * windFactor;
		this.seg3.zRot = Mth.cos(phase - 2.0F) * 0.09F * windFactor;
	}
}
