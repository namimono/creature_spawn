package com.namimono.creaturespawn.client;

import com.namimono.creaturespawn.command.SpawnPoseLocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class CreatureSpawnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpawnCatalogClientNetworking.register();
		MobPlacementClient.register();
		ClientTickEvents.END_CLIENT_TICK.register(SpawnCatalogScreens::hideDuringReplay);
		ClientTickEvents.END_WORLD_TICK.register(level -> SpawnPoseLocks.applyAll(level.entitiesForRendering()));
	}
}
