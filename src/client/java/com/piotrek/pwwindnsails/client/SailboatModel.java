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
 * steerable stern rudder, and toggleable furled/unfurled sail states.
 */
public final class SailboatModel extends EntityModel<SailboatRenderState> {
	private final ModelPart hull;
	private final ModelPart boom;
	private final ModelPart rudder;
	private final ModelPart sailOpen;
	private final ModelPart sailFurled;
	private final ModelPart cleatKnot0;
	private final ModelPart cleatKnot1;
	private final ModelPart cleatKnot2;
	private final ModelPart cleatKnot3;
	private final ModelPart fendersPort;
	private final ModelPart fendersStarboard;

	public SailboatModel(ModelPart root) {
		super(root);
		this.hull = root.getChild("hull");
		this.boom = root.getChild("boom");
		this.rudder = root.getChild("rudder");
		this.sailOpen = this.boom.getChild("sail_open");
		this.sailFurled = this.boom.getChild("sail_furled");
		this.cleatKnot0 = root.getChild("cleat_knot_0");
		this.cleatKnot1 = root.getChild("cleat_knot_1");
		this.cleatKnot2 = root.getChild("cleat_knot_2");
		this.cleatKnot3 = root.getChild("cleat_knot_3");
		this.fendersPort = root.getChild("fenders_port");
		this.fendersStarboard = root.getChild("fenders_starboard");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// --- 1. Hull, Benches, and Mast ---
		root.addOrReplaceChild("hull", CubeListBuilder.create()
			// Thin bottom skin. Water inside the cockpit is cut out by the water mask, not by a solid bilge.
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
			.texOffs(118, 0).addBox(-2.0F, -72.0F, -13.0F, 4.0F, 2.0F, 4.0F)
			// --- 4 Galvanized Metal Mooring Cleats (Knagi) ---
			// 1. Bow Port (Dziób lewa burta)
			.texOffs(190, 8).addBox(-12.0F, -11.0F, -17.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(-12.0F, -12.0F, -19.0F, 2.0F, 1.0F, 6.0F)
			// 2. Bow Starboard (Dziób prawa burta)
			.texOffs(190, 8).addBox(10.0F, -11.0F, -17.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(10.0F, -12.0F, -19.0F, 2.0F, 1.0F, 6.0F)
			// 3. Stern Port (Rufa lewa burta)
			.texOffs(190, 8).addBox(-12.0F, -11.0F, 15.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(-12.0F, -12.0F, 13.0F, 2.0F, 1.0F, 6.0F)
			// 4. Stern Starboard (Rufa prawa burta)
			.texOffs(190, 8).addBox(10.0F, -11.0F, 15.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(10.0F, -12.0F, 13.0F, 2.0F, 1.0F, 6.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		// --- 2. Boom and Mainsail (hinged at the mast: X=0, Y=14, Z=-11) ---
		PartDefinition boom = root.addOrReplaceChild("boom", CubeListBuilder.create()
			// Horizontal Boom spar extending aft 33 units
			.texOffs(0, 140).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 33.0F)
			// Boom gooseneck fitting at mast
			.texOffs(72, 140).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 14.0F, -11.0F)
		);

		// Open unfurled mainsail (triangular cloth rig)
		boom.addOrReplaceChild("sail_open", CubeListBuilder.create()
			.texOffs(0, 178).addBox(-0.5F, -18.0F, 0.5F, 1.0F, 18.0F, 31.0F)
			.texOffs(66, 178).addBox(-0.5F, -34.0F, 0.5F, 1.0F, 16.0F, 22.0F)
			.texOffs(130, 178).addBox(-0.5F, -48.0F, 0.5F, 1.0F, 14.0F, 13.0F)
			.texOffs(184, 178).addBox(-0.5F, -60.0F, 0.5F, 1.0F, 12.0F, 5.0F),
			PartPose.ZERO
		);

		// Furled mainsail: neatly strapped canvas bundle along the boom
		boom.addOrReplaceChild("sail_furled", CubeListBuilder.create()
			.texOffs(0, 178).addBox(-1.5F, -3.0F, 1.0F, 3.0F, 2.0F, 31.0F)
			.texOffs(72, 140).addBox(-2.0F, -3.5F, 6.0F, 4.0F, 3.0F, 1.0F)
			.texOffs(72, 140).addBox(-2.0F, -3.5F, 16.0F, 4.0F, 3.0F, 1.0F)
			.texOffs(72, 140).addBox(-2.0F, -3.5F, 26.0F, 4.0F, 3.0F, 1.0F),
			PartPose.ZERO
		);

		// --- 3. Stern Rudder and Tiller (hinged at stern: X=0, Y=15, Z=23) ---
		root.addOrReplaceChild("rudder", CubeListBuilder.create()
			// Rudder post attached to stern
			.texOffs(90, 104).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 15.0F, 2.0F)
			// Rudder blade. Stops at the hull bottom so it does not dig into the shore.
			.texOffs(100, 104).addBox(-0.5F, 1.0F, 2.0F, 1.0F, 8.0F, 8.0F)
			// Tiller arm extending forward into cockpit for helmsman
			.texOffs(90, 124).addBox(-1.0F, -5.0F, -10.0F, 2.0F, 2.0F, 11.0F)
			// Wooden tiller handle
			.texOffs(90, 138).addBox(-1.5F, -5.5F, -12.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 15.0F, 23.0F)
		);

		// --- 4. 3D Mooring Rope Knots (Rendered when cleat is tied) ---
		root.addOrReplaceChild("cleat_knot_0", CubeListBuilder.create()
			.texOffs(190, 16).addBox(-12.8F, -12.5F, -18.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_1", CubeListBuilder.create()
			.texOffs(190, 16).addBox(9.2F, -12.5F, -18.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_2", CubeListBuilder.create()
			.texOffs(190, 16).addBox(-12.8F, -12.5F, 13.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_3", CubeListBuilder.create()
			.texOffs(190, 16).addBox(9.2F, -12.5F, 13.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		// White cylindrical fenders hung outside the gunwale. Each extends ~0.5 blocks off the hull.
		root.addOrReplaceChild("fenders_port", CubeListBuilder.create()
			.texOffs(220, 220).addBox(-19.0F, -6.0F, -9.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(-16.2F, -11.0F, -7.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(-19.0F, -6.0F, 4.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(-16.2F, -11.0F, 5.8F, 1.2F, 6.0F, 1.2F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("fenders_starboard", CubeListBuilder.create()
			.texOffs(220, 220).addBox(13.0F, -6.0F, -9.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(15.0F, -11.0F, -7.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(13.0F, -6.0F, 4.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(15.0F, -11.0F, 5.8F, 1.2F, 6.0F, 1.2F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	/** Invisible sheet that punches the water surface out of the cockpit, same trick as vanilla boats. */
	public static LayerDefinition createWaterPatch() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild(
			"water_patch",
			CubeListBuilder.create().texOffs(0, 0).addBox(-9.5F, -1.0F, -21.0F, 19.0F, 0.6F, 42.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);
		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public void setupAnim(SailboatRenderState state) {
		super.setupAnim(state);
		// Rudder steering: negative rudder angle turns port, positive turns starboard
		this.rudder.yRot = -state.rudderAngle * Mth.DEG_TO_RAD;
		// Boom and mainsail swing to leeward (strona zawietrzna)
		this.boom.yRot = -state.boomAngle * Mth.DEG_TO_RAD;

		// Toggle open sail vs furled canvas bundle
		this.sailOpen.visible = !state.sailFurled;
		this.sailFurled.visible = state.sailFurled;

		// 3D Mooring rope knots on cleats
		this.cleatKnot0.visible = state.cleatTied[0];
		this.cleatKnot1.visible = state.cleatTied[1];
		this.cleatKnot2.visible = state.cleatTied[2];
		this.cleatKnot3.visible = state.cleatTied[3];
		this.fendersPort.visible = state.fenderPort;
		this.fendersStarboard.visible = state.fenderStarboard;
	}
}
