package com.piotrek.pwwindnsails.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class AnchorModel extends EntityModel<AnchorRenderState> {
	private final ModelPart root;

	public AnchorModel(ModelPart root) {
		super(root);
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Vertical central shank (Y from -12 to 0)
		root.addOrReplaceChild("shank", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F)
			// Stock (horizontal crossbar near top)
			.texOffs(8, 0).addBox(-6.0F, -11.0F, -1.0F, 12.0F, 2.0F, 2.0F)
			// Shackle ring at top
			.texOffs(36, 0).addBox(-2.0F, -15.0F, -0.5F, 4.0F, 3.0F, 1.0F)
			// Crown (bottom base)
			.texOffs(0, 16).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 2.0F, 4.0F)
			// Left arm curving upward
			.texOffs(16, 16).addBox(-6.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F)
			// Left fluke blade
			.texOffs(24, 16).addBox(-7.0F, -7.0F, -1.5F, 3.0F, 2.0F, 3.0F)
			// Right arm curving upward
			.texOffs(16, 16).addBox(4.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F)
			// Right fluke blade
			.texOffs(24, 16).addBox(4.0F, -7.0F, -1.5F, 3.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AnchorRenderState state) {
		super.setupAnim(state);
		if (state.anchored) {
			this.root.zRot = 0.35F; // resting tilted naturally on seabed
		} else {
			this.root.zRot = 0.0F;
		}
	}
}
