package com.namimono.creaturespawn.client;

import com.namimono.creaturespawn.network.OpenSpawnCatalogS2CPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** 客户端刷怪图鉴网络入口。 */
public final class SpawnCatalogClientNetworking {
	private SpawnCatalogClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(OpenSpawnCatalogS2CPayload.TYPE, (payload, context) -> {
			SpawnCatalogScreens.openFromServer(context.client());
		});
	}
}
