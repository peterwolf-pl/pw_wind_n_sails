package com.piotrek.pwwindnsails.network;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client reports the highlighted cleat or fence post under the crosshair. */
public record MooringClickPayload(boolean cleat, int boatId, int cleatIndex, BlockPos fence) implements CustomPacketPayload {
	public static final Type<MooringClickPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WindAndSailsMod.MOD_ID, "mooring_click"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MooringClickPayload> CODEC = new StreamCodec<>() {
		@Override
		public MooringClickPayload decode(RegistryFriendlyByteBuf buf) {
			return new MooringClickPayload(buf.readBoolean(), buf.readVarInt(), buf.readByte(), buf.readBlockPos());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, MooringClickPayload payload) {
			buf.writeBoolean(payload.cleat);
			buf.writeVarInt(payload.boatId);
			buf.writeByte(payload.cleatIndex);
			buf.writeBlockPos(payload.fence);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
