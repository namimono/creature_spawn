package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Seam: 刷怪工具共用的原版活体名单。
 */
class SpawnCatalogTest {
	@BeforeAll
	static void bootstrapRegistries() {
		MinecraftTestBootstrap.ensureInitialized();
	}

	@Test
	void includesOrdinaryLivingMobAndRequiredBosses() {
		List<EntityType<?>> allowed = List.of(
			EntityType.ZOMBIE,
			EntityType.GIANT,
			EntityType.ILLUSIONER,
			EntityType.WARDEN,
			EntityType.WITHER,
			EntityType.ENDER_DRAGON,
			EntityType.ELDER_GUARDIAN
		);

		for (EntityType<?> type : allowed) {
			assertTrue(
				SpawnCatalog.allows(EntityType.getKey(type), type),
				() -> "expected catalog to include " + EntityType.getKey(type)
			);
		}
	}

	@Test
	void excludesPlayersAndNonLivingEntities() {
		List<EntityType<?>> excluded = List.of(
			EntityType.PLAYER,
			EntityType.BOAT,
			EntityType.MINECART,
			EntityType.PAINTING,
			EntityType.ITEM,
			EntityType.ARMOR_STAND
		);

		for (EntityType<?> type : excluded) {
			assertFalse(
				SpawnCatalog.allows(EntityType.getKey(type), type),
				() -> "expected catalog to exclude " + EntityType.getKey(type)
			);
		}
	}

	@Test
	void excludesOtherModIdsEvenWhenTheirRuntimeTypeWouldBeLiving() {
		assertFalse(SpawnCatalog.allows(
			ResourceLocation.fromNamespaceAndPath("other_mod", "zombie_like"),
			EntityType.ZOMBIE
		));
	}

	@Test
	void resolvesVanillaIdsAndRejectsUnknownIds() {
		assertEquals(EntityType.ZOMBIE, SpawnCatalog.find(ResourceLocation.parse("zombie")).orElseThrow());
		assertTrue(SpawnCatalog.find(ResourceLocation.parse("minecraft:not_a_real_mob")).isEmpty());
	}

	@Test
	void groupsCatalogEntriesForBrowsing() {
		assertEquals(SpawnGroup.HOSTILE, SpawnCatalog.group(EntityType.ZOMBIE));
		assertEquals(SpawnGroup.NEUTRAL, SpawnCatalog.group(EntityType.ENDERMAN));
		assertEquals(SpawnGroup.PASSIVE, SpawnCatalog.group(EntityType.COW));

		List<EntityType<?>> bosses = List.of(
			EntityType.ENDER_DRAGON,
			EntityType.WITHER,
			EntityType.WARDEN,
			EntityType.ELDER_GUARDIAN
		);
		for (EntityType<?> boss : bosses) {
			assertEquals(SpawnGroup.BOSS, SpawnCatalog.group(boss));
		}
	}

	@Test
	void everyCatalogEntryBelongsToOneOfTheFourVisibleGroups() {
		Set<SpawnGroup> groups = SpawnCatalog.entries().stream()
			.map(SpawnCatalog::group)
			.collect(Collectors.toSet());

		assertEquals(Set.of(SpawnGroup.values()), groups);
	}
}
