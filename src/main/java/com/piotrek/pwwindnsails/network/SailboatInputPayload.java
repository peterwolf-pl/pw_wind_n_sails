package com.piotrek.pwwindnsails.network;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SailboatInputPayload(
	int boatEntityId,
	float rudderInput,
	float sheetInput,
	boolean toggleSail,
	int targetHikeMode
) implements CustomPacketPayload {
	public static final Type<SailboatInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WindAndSailsMod.MOD_ID, "boat_input"));

	public SailboatInputPayload(int boatEntityId, float rudderInput, float sheetInput, boolean toggleSail) {
		this(boatEntityId, rudderInput, sheetInput, toggleSail, -1);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, SailboatInputPayload> CODEC = new StreamCodec<>() {
		@Override
		public SailboatInputPayload decode(RegistryFriendlyByteBuf buf) {
			return new SailboatInputPayload(
				buf.readVarInt(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readBoolean(),
				buf.readByte()
			);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, SailboatInputPayload p) {
			buf.writeVarInt(p.boatEntityId);
			buf.writeFloat(p.rudderInput);
			buf.writeFloat(p.sheetInput);
			buf.writeBoolean(p.toggleSail);
			buf.writeByte(p.targetHikeMode);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
