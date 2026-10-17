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
 * 4.5m x 1.5m high-performance sport sailboat model (Sloop rig with mainsail and jib).
 * Same width as standard sailboat, 1.5 blocks longer, sleek white racing hull with bowsprit,
 * light aluminium mast extended by 2 blocks without mast truck, boom mounted half a block higher,
 * articulated mainsail, steerable stern rudder, and responsive headsail (jib).
 */
public final class SportSailboatModel extends EntityModel<SailboatRenderState> {
	private final ModelPart hull;
	private final ModelPart boom;
	private final ModelPart rudder;
	private final ModelPart sailOpen;
	private final ModelPart sailFurled;
	private final ModelPart jib;
	private final ModelPart cleatKnot0;
	private final ModelPart cleatKnot1;
	private final ModelPart cleatKnot2;
	private final ModelPart cleatKnot3;
	private final ModelPart fendersPort;
	private final ModelPart fendersStarboard;

	public SportSailboatModel(ModelPart root) {
		super(root);
		this.hull = root.getChild("hull");
		this.boom = root.getChild("boom");
		this.rudder = root.getChild("rudder");
		this.sailOpen = this.boom.getChild("sail_open");
		this.sailFurled = this.boom.getChild("sail_furled");
		this.jib = root.getChild("jib");
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

		// --- 1. Sleek Racing Hull, Decks, and Tall Aluminium Mast ---
		root.addOrReplaceChild("hull", CubeListBuilder.create()
			// Hull bottom skin (72 units total length = 4.5 blocks)
			.texOffs(0, 0).addBox(-10.0F, -2.0F, -34.0F, 20.0F, 2.0F, 68.0F)
			// Port (left) side wall
			.texOffs(0, 72).addBox(-12.0F, -10.0F, -35.0F, 2.0F, 8.0F, 70.0F)
			// Starboard (right) side wall - perfectly identical & symmetrical UV
			.texOffs(0, 72).addBox(10.0F, -10.0F, -35.0F, 2.0F, 8.0F, 70.0F)
			// Wave-piercing bow stem (front wall)
			.texOffs(0, 96).addBox(-10.0F, -11.0F, -36.0F, 20.0F, 9.0F, 2.0F)
			// Bowsprit spar extending forward for headsail tack
			.texOffs(46, 96).addBox(-1.0F, -10.5F, -42.0F, 2.0F, 2.0F, 6.0F)
			// Open racing stern transom
			.texOffs(64, 96).addBox(-10.0F, -9.0F, 34.0F, 20.0F, 7.0F, 2.0F)
			// Aerodynamic covered foredeck
			.texOffs(0, 108).addBox(-10.0F, -10.0F, -34.0F, 20.0F, 2.0F, 18.0F)
			// Mid thwart / cockpit bench
			.texOffs(80, 108).addBox(-10.0F, -6.0F, -2.0F, 20.0F, 2.0F, 4.0F)
			// Helmsman aft seat
			.texOffs(0, 130).addBox(-10.0F, -6.0F, 18.0F, 20.0F, 2.0F, 8.0F)
			// Light aluminium mast (2x2 units thin, extended by +3 blocks total = 110 units tall up to Y=-118, no top cap)
			.texOffs(240, 0).addBox(-1.0F, -118.0F, -17.0F, 2.0F, 110.0F, 2.0F)
			// --- 4 Galvanized Metal Mooring Cleats (Knagi) ---
			// 1. Bow Port (Dziób lewa burta)
			.texOffs(190, 8).addBox(-12.0F, -11.0F, -27.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(-12.0F, -12.0F, -29.0F, 2.0F, 1.0F, 6.0F)
			// 2. Bow Starboard (Dziób prawa burta)
			.texOffs(190, 8).addBox(10.0F, -11.0F, -27.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(10.0F, -12.0F, -29.0F, 2.0F, 1.0F, 6.0F)
			// 3. Stern Port (Rufa lewa burta)
			.texOffs(190, 8).addBox(-12.0F, -11.0F, 25.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(-12.0F, -12.0F, 23.0F, 2.0F, 1.0F, 6.0F)
			// 4. Stern Starboard (Rufa prawa burta)
			.texOffs(190, 8).addBox(10.0F, -11.0F, 25.0F, 2.0F, 1.0F, 2.0F)
			.texOffs(190, 0).addBox(10.0F, -12.0F, 23.0F, 2.0F, 1.0F, 6.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);

		// --- 2. Boom and Mainsail (hinged at Y=6.0F, half a block higher than classic sailboat) ---
		PartDefinition boom = root.addOrReplaceChild("boom", CubeListBuilder.create()
			// Horizontal Boom spar extending aft 42 units
			.texOffs(0, 142).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 42.0F)
			// Gooseneck fitting at mast
			.texOffs(86, 142).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 2.0F),
			PartPose.offset(0.0F, 6.0F, -16.0F)
		);

		// Open unfurled high-aspect racing mainsail (fat-head / square-top extended to taller mast)
		boom.addOrReplaceChild("sail_open", CubeListBuilder.create()
			.texOffs(0, 160).addBox(-0.5F, -30.0F, 0.5F, 1.0F, 30.0F, 40.0F)
			.texOffs(82, 160).addBox(-0.5F, -60.0F, 0.5F, 1.0F, 30.0F, 30.0F)
			.texOffs(144, 160).addBox(-0.5F, -88.0F, 0.5F, 1.0F, 28.0F, 20.0F)
			.texOffs(186, 160).addBox(-0.5F, -114.0F, 0.5F, 1.0F, 26.0F, 10.0F),
			PartPose.ZERO
		);

		// Furled mainsail bundle along boom
		boom.addOrReplaceChild("sail_furled", CubeListBuilder.create()
			.texOffs(0, 160).addBox(-1.5F, -3.0F, 1.0F, 3.0F, 2.0F, 40.0F)
			.texOffs(86, 142).addBox(-2.0F, -3.5F, 8.0F, 4.0F, 3.0F, 1.0F)
			.texOffs(86, 142).addBox(-2.0F, -3.5F, 20.0F, 4.0F, 3.0F, 1.0F)
			.texOffs(86, 142).addBox(-2.0F, -3.5F, 32.0F, 4.0F, 3.0F, 1.0F),
			PartPose.ZERO
		);

		// --- 3. Foresail / Jib (hinged at the bow forestay tack: Z=-36.0F, Y=14.0F, reaching higher up the mast) ---
		PartDefinition jib = root.addOrReplaceChild("jib", CubeListBuilder.create()
			.texOffs(100, 200).addBox(-0.5F, -30.0F, 0.0F, 1.0F, 30.0F, 19.0F)
			.texOffs(142, 200).addBox(-0.5F, -64.0F, 4.0F, 1.0F, 34.0F, 14.0F)
			.texOffs(172, 200).addBox(-0.5F, -94.0F, 8.0F, 1.0F, 30.0F, 9.0F),
			PartPose.offset(0.0F, 14.0F, -36.0F)
		);

		// --- 4. Deep Racing Rudder & Tiller ---
		root.addOrReplaceChild("rudder", CubeListBuilder.create()
			// Rudder post on stern
			.texOffs(200, 50).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 15.0F, 2.0F)
			// Deep high-aspect spade rudder blade
			.texOffs(210, 50).addBox(-0.5F, 2.0F, 1.5F, 1.0F, 9.0F, 8.0F)
			// Tiller extending forward into cockpit
			.texOffs(200, 70).addBox(-1.0F, -5.0F, -12.0F, 2.0F, 2.0F, 13.0F)
			// Tiller extension grip
			.texOffs(200, 86).addBox(-1.5F, -5.5F, -15.0F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 15.0F, 35.0F)
		);

		// --- 5. 3D Mooring Rope Knots (Rendered when cleat is tied) ---
		root.addOrReplaceChild("cleat_knot_0", CubeListBuilder.create()
			.texOffs(190, 16).addBox(-12.8F, -12.5F, -28.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_1", CubeListBuilder.create()
			.texOffs(190, 16).addBox(9.2F, -12.5F, -28.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_2", CubeListBuilder.create()
			.texOffs(190, 16).addBox(-12.8F, -12.5F, 23.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("cleat_knot_3", CubeListBuilder.create()
			.texOffs(190, 16).addBox(9.2F, -12.5F, 23.5F, 3.6F, 2.5F, 5.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		root.addOrReplaceChild("fenders_port", CubeListBuilder.create()
			.texOffs(220, 220).addBox(-19.0F, -6.0F, -16.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(-16.2F, -11.0F, -14.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(-19.0F, -6.0F, -2.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(-16.2F, -11.0F, -0.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(-19.0F, -6.0F, 12.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(-16.2F, -11.0F, 13.8F, 1.2F, 6.0F, 1.2F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		root.addOrReplaceChild("fenders_starboard", CubeListBuilder.create()
			.texOffs(220, 220).addBox(13.0F, -6.0F, -16.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(15.0F, -11.0F, -14.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(13.0F, -6.0F, -2.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(15.0F, -11.0F, -0.2F, 1.2F, 6.0F, 1.2F)
			.texOffs(220, 220).addBox(13.0F, -6.0F, 12.0F, 6.0F, 14.0F, 5.0F)
			.texOffs(220, 220).addBox(15.0F, -11.0F, 13.8F, 1.2F, 6.0F, 1.2F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	public static LayerDefinition createWaterPatch() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild(
			"water_patch",
			CubeListBuilder.create().texOffs(0, 0).addBox(-9.5F, -1.0F, -16.0F, 19.0F, 0.6F, 50.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F)
		);
		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public void setupAnim(SailboatRenderState state) {
		super.setupAnim(state);
		// Rudder
		this.rudder.yRot = -state.rudderAngle * Mth.DEG_TO_RAD;

		// Mainsail boom swings to leeward
		this.boom.yRot = -state.boomAngle * Mth.DEG_TO_RAD;

		// Jib swings to the same leeward side (at ~65% of boom angle, tightly sheeted slot)
		this.jib.yRot = -state.boomAngle * 0.65F * Mth.DEG_TO_RAD;

		// Toggle open mainsail vs furled bundle
		this.sailOpen.visible = !state.sailFurled;
		this.sailFurled.visible = state.sailFurled;

		// When sails are furled, jib is completely removed/invisible (no second mast clutter!)
		this.jib.visible = !state.sailFurled;

		// 3D Mooring rope knots on cleats
		this.cleatKnot0.visible = state.cleatTied[0];
		this.cleatKnot1.visible = state.cleatTied[1];
		this.cleatKnot2.visible = state.cleatTied[2];
		this.cleatKnot3.visible = state.cleatTied[3];
		this.fendersPort.visible = state.fenderPort;
		this.fendersStarboard.visible = state.fenderStarboard;
	}
}
