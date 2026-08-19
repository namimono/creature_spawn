package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.CreatureSpawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 服务端验权后通知客户端打开刷怪图鉴。 */
public record OpenSpawnCatalogS2CPayload() implements CustomPacketPayload {
	public static final Type<OpenSpawnCatalogS2CPayload> TYPE =
		new Type<>(CreatureSpawn.id("open_spawn_catalog"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpawnCatalogS2CPayload> STREAM_CODEC =
		StreamCodec.unit(new OpenSpawnCatalogS2CPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
