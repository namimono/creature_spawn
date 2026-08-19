package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.command.SpawnCatalogSpawner;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(
			OpenSpawnCatalogS2CPayload.TYPE,
			OpenSpawnCatalogS2CPayload.STREAM_CODEC
		);

		PayloadTypeRegistry.playC2S().register(SpawnCatalogC2SPayload.TYPE, SpawnCatalogC2SPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SpawnCatalogC2SPayload.TYPE, (payload, context) -> {
			SpawnCatalogSpawner.spawn(context.player(), payload.entityIds(), payload.quantity());
		});
	}
}
