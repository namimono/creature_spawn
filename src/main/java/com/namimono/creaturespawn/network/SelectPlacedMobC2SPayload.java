package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.CreatureSpawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 摆放模式左键：选中已放下的生物，或 -1 取消。 */
public record SelectPlacedMobC2SPayload(int entityId) implements CustomPacketPayload {
	public static final Type<SelectPlacedMobC2SPayload> TYPE = new Type<>(CreatureSpawn.id("select_placed_mob"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SelectPlacedMobC2SPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			SelectPlacedMobC2SPayload::entityId,
			SelectPlacedMobC2SPayload::new
		);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
