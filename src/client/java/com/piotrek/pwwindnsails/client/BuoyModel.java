package com.piotrek.pwwindnsails.client;

import com.piotrek.pwwindnsails.entity.BuoyEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class BuoyModel extends EntityModel<BuoyRenderState> {
	private final ModelPart root;
	private final ModelPart redBuoy;
	private final ModelPart blueBuoy;

	public BuoyModel(ModelPart root) {
		super(root);
		this.root = root;
		this.redBuoy = root.getChild("red_buoy");
		this.blueBuoy = root.getChild("blue_buoy");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Red Spherical Buoy (~0.75-0.8 blocks diameter, stepped sphere)
		root.addOrReplaceChild("red_buoy", CubeListBuilder.create()
			// Center equator slice: 12x4x12
			.texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 4.0F, 12.0F)
			// Upper slice: 10x3x10
			.texOffs(0, 16).addBox(-5.0F, -11.0F, -5.0F, 10.0F, 3.0F, 10.0F)
			// Top cap: 6x1x6
			.texOffs(0, 42).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 1.0F, 6.0F)
			// Lower slice: 10x3x10
			.texOffs(0, 29).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 3.0F, 10.0F)
			// Bottom cap: 6x1x6
			.texOffs(0, 49).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 1.0F, 6.0F)
			// Mooring ring on top: 4x3x1
			.texOffs(42, 16).addBox(-2.0F, -15.0F, -0.5F, 4.0F, 3.0F, 1.0F)
			// Anchor ring at bottom: 4x2x1
			.texOffs(42, 22).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		// Blue Cylindrical Buoy (0.7 blocks diameter, 1.0 block height, octagonal cylinder)
		root.addOrReplaceChild("blue_buoy", CubeListBuilder.create()
			// Segment A: 11x14x7
			.texOffs(0, 0).addBox(-5.5F, -15.0F, -3.5F, 11.0F, 14.0F, 7.0F)
			// Segment B: 7x14x11 (crosses Segment A to form octagonal cylinder)
			.texOffs(0, 22).addBox(-3.5F, -15.0F, -5.5F, 7.0F, 14.0F, 11.0F)
			// Top cap: 7x1x7
			.texOffs(0, 48).addBox(-3.5F, -16.0F, -3.5F, 7.0F, 1.0F, 7.0F)
			// Bottom cap: 7x1x7
			.texOffs(28, 48).addBox(-3.5F, -1.0F, -3.5F, 7.0F, 1.0F, 7.0F)
			// Mooring ring on top: 4x3x1
			.texOffs(38, 0).addBox(-2.0F, -19.0F, -0.5F, 4.0F, 3.0F, 1.0F)
			// Anchor ring at bottom: 4x2x1
			.texOffs(38, 5).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(BuoyRenderState state) {
		super.setupAnim(state);

		boolean isBlue = state.buoyType == BuoyEntity.TYPE_BLUE_CYLINDRICAL;
		this.redBuoy.visible = !isBlue;
		this.blueBuoy.visible = isBlue;

		// Gentle oceanic rocking on the water
		float waveTicks = state.ageInTicks * 0.08F + (state.id * 1.7F);
		this.root.zRot = Mth.sin(waveTicks) * 0.05F;
		this.root.xRot = Mth.cos(waveTicks * 0.85F) * 0.04F;
	}
}
