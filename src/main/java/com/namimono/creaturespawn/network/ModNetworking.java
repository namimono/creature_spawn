package com.namimono.creaturespawn.network;

import com.namimono.creaturespawn.command.MobPlacementSessions;
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
			if (payload.manualPlacement()) {
				MobPlacementSessions.begin(
					context.player(),
					payload.entityIds(),
					payload.quantity(),
					payload.lockPose()
				);
				return;
			}
			SpawnCatalogSpawner.spawn(
				context.player(),
				payload.entityIds(),
				payload.quantity(),
				payload.lockPose()
			);
		});

		PayloadTypeRegistry.playC2S().register(PlacementModeC2SPayload.TYPE, PlacementModeC2SPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PlacementModeC2SPayload.TYPE, (payload, context) -> {
			MobPlacementSessions.setActive(context.player(), payload.active());
		});

		PayloadTypeRegistry.playC2S().register(PlaceMobC2SPayload.TYPE, PlaceMobC2SPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PlaceMobC2SPayload.TYPE, (payload, context) -> {
			MobPlacementSessions.placeAt(context.player(), payload.hitBlock(), payload.face());
		});

		PayloadTypeRegistry.playC2S().register(SelectPlacedMobC2SPayload.TYPE, SelectPlacedMobC2SPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectPlacedMobC2SPayload.TYPE, (payload, context) -> {
			MobPlacementSessions.selectPlaced(context.player(), payload.entityId());
		});
	}
}
