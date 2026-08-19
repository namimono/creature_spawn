package com.namimono.creaturespawn.command;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

final class MinecraftTestBootstrap {
	private MinecraftTestBootstrap() {
	}

	static void ensureInitialized() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
}
