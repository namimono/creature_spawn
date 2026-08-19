package com.namimono.creaturespawn;

import com.namimono.creaturespawn.command.SpawnCommands;
import com.namimono.creaturespawn.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatureSpawn implements ModInitializer {
	public static final String MOD_ID = "creature_spawn";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModNetworking.register();
		SpawnCommands.register();
		LOGGER.info("Creature Spawn initialized");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
