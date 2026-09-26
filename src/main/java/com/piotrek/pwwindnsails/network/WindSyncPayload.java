package com.piotrek.pwwindnsails.network;

import com.piotrek.pwwindnsails.WindAndSailsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WindSyncPayload(
	float baseDirection,
	float baseStrength,
	float targetDirection,
	float targetStrength,
	boolean gustActive,
	long gustStartTick,
	int gustDurationTicks,
	float gustShiftDeg,
	float gustStrengthMult,
	long gameTick
) implements CustomPacketPayload {
	public static final Type<WindSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WindAndSailsMod.MOD_ID, "wind_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WindSyncPayload> CODEC = new StreamCodec<>() {
		@Override
		public WindSyncPayload decode(RegistryFriendlyByteBuf buf) {
			return new WindSyncPayload(
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readBoolean(),
				buf.readLong(),
				buf.readInt(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readLong()
			);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, WindSyncPayload p) {
			buf.writeFloat(p.baseDirection);
			buf.writeFloat(p.baseStrength);
			buf.writeFloat(p.targetDirection);
			buf.writeFloat(p.targetStrength);
			buf.writeBoolean(p.gustActive);
			buf.writeLong(p.gustStartTick);
			buf.writeInt(p.gustDurationTicks);
			buf.writeFloat(p.gustShiftDeg);
			buf.writeFloat(p.gustStrengthMult);
			buf.writeLong(p.gameTick);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
