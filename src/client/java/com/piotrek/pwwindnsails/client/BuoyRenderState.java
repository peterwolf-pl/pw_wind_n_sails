package com.piotrek.pwwindnsails.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class BuoyRenderState extends EntityRenderState {
	public int buoyType = 0;
	public int id = 0;
	public boolean hasAnchor = false;
	@Nullable
	public BlockPos anchorBedPos = null;
	public Identifier texture;
}
