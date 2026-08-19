package com.namimono.creaturespawn.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/** 客户端图鉴请求通过服务端校验后得到的不可变刷怪批次。 */
public record SpawnPlan(List<EntityType<?>> types, SpawnQuantity quantity) {
	public SpawnPlan {
		types = List.copyOf(types);
	}

	public static Optional<SpawnPlan> prepare(
		Collection<ResourceLocation> selectedIds,
		SpawnQuantity quantity
	) {
		if (selectedIds.isEmpty()) {
			return Optional.empty();
		}

		List<EntityType<?>> types = new ArrayList<>(selectedIds.size());
		for (ResourceLocation id : new LinkedHashSet<>(selectedIds)) {
			Optional<EntityType<?>> type = SpawnCatalog.find(id);
			if (type.isEmpty()) {
				return Optional.empty();
			}
			types.add(type.orElseThrow());
		}
		return Optional.of(new SpawnPlan(types, quantity));
	}
}
