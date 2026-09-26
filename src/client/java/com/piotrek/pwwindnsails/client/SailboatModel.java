package com.piotrek.pwwindnsails.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 3.0m x 1.5m single-mast sailing boat model.
 * Features articulated boom and mainsail responding to apparent wind and mainsheet,
 * as well as a steerable stern rudder with cockpit tiller.
 */
public final class SailboatModel extends EntityModel<SailboatRenderState> {
	private final ModelPart hull;
	private final ModelPart boom;
	private final ModelPart rudder;

	public SailboatModel(ModelPart root) {
		super(root);
		this.hull = root.getChild("hull");
		this.boom = root.getChild("boom");
		this.rudder = root.getChild("rudder");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// --- 1. Hull, Benches, and Mast ---
		PartDefinition hull = root.addOrReplaceChild("hull", CubeListBuilder.create()
			// Bottom floor (20 wide, 2 high, 44 long)
			.texOffs(0, 0).addBox(-10.0F, -2.0F, -22.0F, 20.0F, 2.0F, 44.0F)
			// Port (left) side wall
			.texOffs(0, 48).addBox(-12.0F, -10.0F, -23.0F, 2.0F, 8.0F, 46.0F)
			// Starboard (right) side wall
			.texOffs(52, 48).addBox(10.0F, -10.0F, -23.0F, 2.0F, 8.0F, 46.0F)
			// Bow stem (front wall)
			.texOffs(0, 104).addBox(-10.0F, -11.0F, -24.0F, 20.0F, 9.0F, 2.0F)
			// Stern transom (rear wall)
			.texOffs(46, 104).addBox(-10.0F, -11.0F, 22.0F, 20.0F, 9.0F, 2.0F)
			// Forward deck & mast step
			.texOffs(0, 117).addBox(-10.0F, -9.0F, -14.0F, 20.0F, 2.0F, 6.0F)
			// Mid thwart / bench
			.texOffs(54, 117).addBox(-10.0F, -6.0F, -2.0F, 20.0F, 2.0F, 4.0F)
			// Aft helmsman seat bench
			.texOffs(0, 127).addBox(-10.0F, -6.0F, 12.0F, 20.0F, 2.0F, 6.0F)
			// Single Mast (extends from deck up to Y=-70, length ~60 units = 3.75 blocks)
			.texOffs(104, 0).addBox(-1.5F, -70.0F, -12.5F, 3.0F, 62.0F, 3.0F)
			// Mast truck / top cap
			.texOffs(118, 0).addBox(-2.0F, -72.0F, -13.0F, 4.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		// --- 2. Boom and Mainsail (hinged at the mast: X=0, Y=10, Z=-11) ---
		PartDefinition boom = root.addOrReplaceChild("boom", CubeListBuilder.create()
			// Horizontal Boom spar extending aft 33 units
			.texOffs(0, 140).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 33.0F)
			// Boom gooseneck fitting at mast
			.texOffs(72, 140).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 2.0F)
			// Mainsail cloth panels (triangular rig, ~5m² surface area)
			// Lower foot panel
			.texOffs(0, 178).addBox(-0.5F, -18.0F, 0.5F, 1.0F, 18.0F, 31.0F)
			// Mid panel
			.texOffs(66, 178).addBox(-0.5F, -34.0F, 0.5F, 1.0F, 16.0F, 22.0F)
			// Upper panel
			.texOffs(130, 178).addBox(-0.5F, -48.0F, 0.5F, 1.0F, 14.0F, 13.0F)
			// Top head panel
			.texOffs(184, 178).addBox(-0.5F, -60.0F, 0.5F, 1.0F, 12.0F, 5.0F),
			PartPose.offset(0.0F, 14.0F, -11.0F)
		);

		// --- 3. Stern Rudder and Tiller (hinged at stern: X=0, Y=14, Z=23) ---
		PartDefinition rudder = root.addOrReplaceChild("rudder", CubeListBuilder.create()
			// Rudder post attached to stern
			.texOffs(90, 104).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 15.0F, 2.0F)
			// Underwater rudder blade (dipping down into water)
			.texOffs(100, 104).addBox(-0.5F, 1.0F, 2.0F, 1.0F, 10.0F, 8.0F)
			// Tiller arm extending forward into cockpit for helmsman
			.texOffs(90, 124).addBox(-1.0F, -5.0F, -10.0F, 2.0F, 2.0F, 11.0F)
			// Wooden tiller handle
			.texOffs(90, 138).addBox(-1.5F, -5.5F, -12.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 15.0F, 23.0F)
		);

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(SailboatRenderState state) {
		super.setupAnim(state);
		// Rudder steering: negative rudder angle turns port, positive turns starboard
		this.rudder.yRot = -state.rudderAngle * Mth.DEG_TO_RAD;
		// Boom and mainsail swing to leeward (strona zawietrzna)
		this.boom.yRot = -state.boomAngle * Mth.DEG_TO_RAD;
	}
}
