package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Seam: 摆放模式把生物落到被点击方块的那一面。
 */
class MobPlacementTest {
	@BeforeAll
	static void bootstrapRegistries() {
		MinecraftTestBootstrap.ensureInitialized();
	}

	@Test
	void placesOnTheClickedFaceLikeABlock() {
		BlockPos cell = MobPlacement.cell(new BlockPos(10, 64, -3), Direction.UP);

		assertEquals(new BlockPos(10, 65, -3), cell);
		assertEquals(new Vec3(10.5, 65.0, -2.5), MobPlacement.feet(cell));
	}

	@Test
	void sideFaceShiftsTheCellOntoThatNeighbor() {
		assertEquals(
			new BlockPos(11, 64, -3),
			MobPlacement.cell(new BlockPos(10, 64, -3), Direction.EAST)
		);
	}

	@Test
	void catalogQueueRepeatsEachSelectedTypeByQuantity() {
		SpawnEntry zombie = new SpawnEntry(ResourceLocation.parse("minecraft:zombie"), EntityType.ZOMBIE);
		SpawnEntry skeleton = new SpawnEntry(ResourceLocation.parse("minecraft:skeleton"), EntityType.SKELETON);

		assertEquals(
			List.of(zombie, zombie, skeleton, skeleton),
			MobPlacement.queue(List.of(zombie, skeleton), new SpawnQuantity(2))
		);
	}
}
