package com.namimono.creaturespawn.command;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

/**
 * 刷怪工具可用的实体名单：仅原版、可召唤的生物。
 */
public final class SpawnCatalog {
	public static final ResourceLocation CHARGED_CREEPER =
		ResourceLocation.fromNamespaceAndPath("minecraft", "charged_creeper");
	private static final Set<EntityType<?>> VANILLA_MOBS = discoverVanillaMobs();
	private static final Map<ResourceLocation, SpawnEntry> ENTRIES = discoverEntries();
	private static final Set<EntityType<?>> BOSSES = Set.of(
		EntityType.ENDER_DRAGON,
		EntityType.WITHER,
		EntityType.WARDEN,
		EntityType.ELDER_GUARDIAN
	);
	private static final Set<EntityType<?>> NEUTRAL_MOBS = Set.of(
		EntityType.BEE,
		EntityType.CAVE_SPIDER,
		EntityType.DOLPHIN,
		EntityType.ENDERMAN,
		EntityType.GOAT,
		EntityType.IRON_GOLEM,
		EntityType.LLAMA,
		EntityType.PANDA,
		EntityType.PIGLIN,
		EntityType.POLAR_BEAR,
		EntityType.SPIDER,
		EntityType.TRADER_LLAMA,
		EntityType.WOLF,
		EntityType.ZOMBIFIED_PIGLIN
	);

	private SpawnCatalog() {
	}

	public static boolean allows(ResourceLocation id, EntityType<?> type) {
		return find(id).filter(entry -> entry.type() == type).isPresent();
	}

	public static Set<SpawnEntry> entries() {
		return Set.copyOf(ENTRIES.values());
	}

	public static Optional<SpawnEntry> find(ResourceLocation id) {
		return Optional.ofNullable(ENTRIES.get(id));
	}

	public static SpawnGroup group(EntityType<?> type) {
		if (BOSSES.contains(type)) {
			return SpawnGroup.BOSS;
		}
		if (NEUTRAL_MOBS.contains(type)) {
			return SpawnGroup.NEUTRAL;
		}
		return type.getCategory() == MobCategory.MONSTER
			? SpawnGroup.HOSTILE
			: SpawnGroup.PASSIVE;
	}

	private static Set<EntityType<?>> discoverVanillaMobs() {
		Set<EntityType<?>> result = new LinkedHashSet<>();
		for (Field field : EntityType.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || !isMobEntityType(field.getGenericType())) {
				continue;
			}
			try {
				EntityType<?> type = (EntityType<?>) field.get(null);
				if (type.canSummon()) {
					result.add(type);
				}
			} catch (IllegalAccessException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	private static Map<ResourceLocation, SpawnEntry> discoverEntries() {
		Map<ResourceLocation, SpawnEntry> result = new LinkedHashMap<>();
		for (EntityType<?> type : VANILLA_MOBS) {
			SpawnEntry entry = new SpawnEntry(EntityType.getKey(type), type);
			result.put(entry.id(), entry);
		}
		result.putIfAbsent(CHARGED_CREEPER, new SpawnEntry(CHARGED_CREEPER, EntityType.CREEPER));
		return Collections.unmodifiableMap(result);
	}

	private static boolean isMobEntityType(Type genericType) {
		if (!(genericType instanceof ParameterizedType parameterized)) {
			return false;
		}
		Type[] arguments = parameterized.getActualTypeArguments();
		return arguments.length == 1
			&& arguments[0] instanceof Class<?> entityClass
			&& Mob.class.isAssignableFrom(entityClass);
	}
}
