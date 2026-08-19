package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Seam: 图鉴请求交给共享 LivingSpawner 前的批次计划。 */
class SpawnPlanTest {
	@BeforeAll
	static void bootstrapRegistries() {
		MinecraftTestBootstrap.ensureInitialized();
	}

	@Test
	void selectedTypesEachReceiveTheRequestedQuantity() {
		SpawnPlan plan = SpawnPlan.prepare(
			List.of(ResourceLocation.parse("zombie"), ResourceLocation.parse("skeleton")),
			new SpawnQuantity(5)
		).orElseThrow();

		assertEquals(List.of(EntityType.ZOMBIE, EntityType.SKELETON), plan.types());
		assertEquals(new SpawnQuantity(5), plan.quantity());
	}

	@Test
	void emptySelectionDoesNotCreateASpawnPlan() {
		assertTrue(SpawnPlan.prepare(List.of(), new SpawnQuantity(5)).isEmpty());
	}

	@Test
	void invalidEntityRejectsTheWholePlan() {
		assertTrue(SpawnPlan.prepare(
			List.of(ResourceLocation.parse("zombie"), ResourceLocation.parse("boat")),
			new SpawnQuantity(5)
		).isEmpty());
	}

	@Test
	void duplicateSelectionIsPlannedOnce() {
		SpawnPlan plan = SpawnPlan.prepare(
			List.of(ResourceLocation.parse("zombie"), ResourceLocation.parse("zombie")),
			new SpawnQuantity(2)
		).orElseThrow();

		assertEquals(List.of(EntityType.ZOMBIE), plan.types());
	}
}
