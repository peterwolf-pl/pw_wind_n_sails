package com.piotrek.pwwindnsails.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class FlagpoleRenderState extends BlockEntityRenderState {
	public float windDirectionDeg;
	public float windStrength;
	public float gameTime;
	public boolean isTopPart;
	public int tint = -1;

	public float phaseOffset;
	public float speedMultiplier = 1.0F;
	public float flutterOffset;
	public float amplitudeScale = 1.0F;
}
