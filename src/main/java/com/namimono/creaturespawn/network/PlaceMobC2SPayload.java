package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.CreatureSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 摆放模式右键：在该方块面放下或挪过去。 */
public record PlaceMobC2SPayload(BlockPos hitBlock, Direction face) implements CustomPacketPayload {
	public static final Type<PlaceMobC2SPayload> TYPE = new Type<>(CreatureSpawn.id("place_mob"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceMobC2SPayload> STREAM_CODEC =
		StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			PlaceMobC2SPayload::hitBlock,
			Direction.STREAM_CODEC,
			PlaceMobC2SPayload::face,
			PlaceMobC2SPayload::new
		);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
