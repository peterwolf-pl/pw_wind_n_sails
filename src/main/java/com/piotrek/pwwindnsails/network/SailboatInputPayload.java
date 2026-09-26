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
	boolean toggleSail
) implements CustomPacketPayload {
	public static final Type<SailboatInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WindAndSailsMod.MOD_ID, "boat_input"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SailboatInputPayload> CODEC = new StreamCodec<>() {
		@Override
		public SailboatInputPayload decode(RegistryFriendlyByteBuf buf) {
			return new SailboatInputPayload(
				buf.readVarInt(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readBoolean()
			);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, SailboatInputPayload p) {
			buf.writeVarInt(p.boatEntityId);
			buf.writeFloat(p.rudderInput);
			buf.writeFloat(p.sheetInput);
			buf.writeBoolean(p.toggleSail);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
