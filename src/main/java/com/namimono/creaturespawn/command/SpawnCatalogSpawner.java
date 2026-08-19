package com.namimono.creaturespawn.command;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 服务端图鉴批次入口：权限与请求校验后复用单种活体生成入口。 */
public final class SpawnCatalogSpawner {
	private SpawnCatalogSpawner() {
	}

	public static int spawn(
		ServerPlayer player,
		Collection<ResourceLocation> selectedIds,
		SpawnQuantity quantity
	) {
		if (!player.hasPermissions(2)) {
			return 0;
		}

		SpawnPlan plan = SpawnPlan.prepare(selectedIds, quantity).orElse(null);
		if (plan == null) {
			return 0;
		}

		return LivingSpawner.spawn(player, plan.types(), plan.quantity());
	}
}
