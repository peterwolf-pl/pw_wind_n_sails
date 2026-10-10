package com.piotrek.pwwindnsails.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class SailboatRenderState extends EntityRenderState {
	public float yaw;
	public float heel;
	public float boomAngle;
	public float rudderAngle;
	public float speed;
	public boolean sailFurled;
	public float hurtTime;
	public float damage;
	public final boolean[] cleatTied = new boolean[4];
	public boolean holdingLead;
	public int targetedCleat = -1;
	/** White fenders hung on a side tied to the dock by both cleats. */
	public boolean fenderPort;
	public boolean fenderStarboard;
}
