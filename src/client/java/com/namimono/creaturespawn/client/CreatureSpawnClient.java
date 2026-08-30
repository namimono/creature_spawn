package com.namimono.creaturespawn.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class CreatureSpawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpawnCatalogClientNetworking.register();
		ClientTickEvents.END_CLIENT_TICK.register(SpawnCatalogScreens::hideDuringReplay);
	}
}
