package com.piotrek.pwwindnsails.block.entity;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FlagpoleBlockEntity extends BlockEntity {
	public FlagpoleBlockEntity(BlockPos pos, BlockState state) {
		super(WindAndSailsMod.FLAGPOLE_BLOCK_ENTITY, pos, state);
	}
}
