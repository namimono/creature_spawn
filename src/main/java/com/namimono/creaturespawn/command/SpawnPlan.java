package com.namimono.creaturespawn.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/** 客户端图鉴请求通过服务端校验后得到的不可变刷怪批次。 */
public record SpawnPlan(List<SpawnEntry> entries, SpawnQuantity quantity) {
	public SpawnPlan {
		entries = List.copyOf(entries);
	}

	public List<EntityType<?>> types() {
		return entries.stream().map(SpawnEntry::type).toList();
	}

	public static Optional<SpawnPlan> prepare(
		Collection<ResourceLocation> selectedIds,
		SpawnQuantity quantity
	) {
		if (selectedIds.isEmpty()) {
			return Optional.empty();
		}

		List<SpawnEntry> entries = new ArrayList<>(selectedIds.size());
		for (ResourceLocation id : new LinkedHashSet<>(selectedIds)) {
			Optional<SpawnEntry> entry = SpawnCatalog.find(id);
			if (entry.isEmpty()) {
				return Optional.empty();
			}
			entries.add(entry.orElseThrow());
		}
		return Optional.of(new SpawnPlan(entries, quantity));
	}
}
