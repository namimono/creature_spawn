package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.CreatureSpawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端打开或关闭摆放模式。 */
public record PlacementModeC2SPayload(boolean active) implements CustomPacketPayload {
	public static final Type<PlacementModeC2SPayload> TYPE = new Type<>(CreatureSpawn.id("placement_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PlacementModeC2SPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.BOOL,
			PlacementModeC2SPayload::active,
			PlacementModeC2SPayload::new
		);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
