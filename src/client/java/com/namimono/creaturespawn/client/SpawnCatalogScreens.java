package com.namimono.creaturespawn.client;

import com.namimono.creaturespawn.SpawnCatalogOpenPolicy;
import net.minecraft.client.Minecraft;

/** 按回放状态打开或收起刷怪图鉴。 */
public final class SpawnCatalogScreens {
	private SpawnCatalogScreens() {
	}

	public static void openFromServer(Minecraft client) {
		boolean catalogVisible = client.screen instanceof SpawnCatalogScreen;
		switch (SpawnCatalogOpenPolicy.decide(FlashbackReplay.isActive(), catalogVisible)) {
			case OPEN -> client.setScreen(new SpawnCatalogScreen());
			case CLOSE -> client.setScreen(null);
			case LEAVE_CLOSED -> {
			}
		}
	}

	public static void hideDuringReplay(Minecraft client) {
		if (client.screen instanceof SpawnCatalogScreen
			&& !SpawnCatalogOpenPolicy.shouldRender(FlashbackReplay.isActive())) {
			client.setScreen(null);
		}
	}
}
