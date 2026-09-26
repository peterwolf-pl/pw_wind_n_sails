package com.piotrek.pwwindnsails.block.entity;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FlagpoleBlockEntity extends BlockEntity {
	private int colorId = DyeColor.WHITE.getId();

	public FlagpoleBlockEntity(BlockPos pos, BlockState state) {
		super(WindAndSailsMod.FLAGPOLE_BLOCK_ENTITY, pos, state);
	}

	public DyeColor getColor() {
		return DyeColor.byId(this.colorId);
	}

	public void setColor(DyeColor color) {
		this.colorId = color.getId();
		this.setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("Color", this.colorId);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.colorId = input.getInt("Color").orElse(DyeColor.WHITE.getId());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}
}
