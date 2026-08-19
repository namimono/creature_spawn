package com.namimono.creaturespawn.client;

import net.fabricmc.api.ClientModInitializer;

public class CreatureSpawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpawnCatalogClientNetworking.register();
	}
}
