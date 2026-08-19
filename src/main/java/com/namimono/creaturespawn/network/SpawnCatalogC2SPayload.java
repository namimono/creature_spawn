package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.CreatureSpawn;
import com.namimono.creaturespawn.command.SpawnCatalog;
import com.namimono.creaturespawn.command.SpawnQuantity;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端图鉴提交的已选实体 id 与每种生成数量。 */
public record SpawnCatalogC2SPayload(List<ResourceLocation> entityIds, SpawnQuantity quantity)
	implements CustomPacketPayload {
	public static final Type<SpawnCatalogC2SPayload> TYPE =
		new Type<>(CreatureSpawn.id("spawn_catalog"));
	private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS_CODEC =
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(SpawnCatalog.entries().size()));
	private static final StreamCodec<ByteBuf, SpawnQuantity> QUANTITY_CODEC =
		ByteBufCodecs.VAR_INT.map(SpawnQuantity::new, SpawnQuantity::value);
	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnCatalogC2SPayload> STREAM_CODEC =
		StreamCodec.composite(
			IDS_CODEC,
			SpawnCatalogC2SPayload::entityIds,
			QUANTITY_CODEC,
			SpawnCatalogC2SPayload::quantity,
			SpawnCatalogC2SPayload::new
		);

	public SpawnCatalogC2SPayload {
		entityIds = List.copyOf(entityIds);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
