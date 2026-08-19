package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Seam: 准星方块、空气回退与多只网格的生成格计算。
 */
class SpawnPlacementTest {

	@Test
	void blockHitUsesSpawnEggSideOfTargetBlock() {
		BlockPos target = SpawnPlacement.forBlockHit(new BlockPos(10, 64, -3), Direction.UP);

		assertEquals(new BlockPos(10, 65, -3), target);
	}

	@Test
	void airFallbackProbesEightBlocksAlongTheViewVector() {
		Vec3 probe = SpawnPlacement.fallbackProbe(new Vec3(0.0, 64.0, 0.0), new Vec3(3.0, 0.0, 4.0));

		assertEquals(new Vec3(4.8, 64.0, 6.4), probe);
	}

	@Test
	void airFallbackLandsAboveTheFirstSolidBlockBelowTheProbe() {
		BlockPos target = SpawnPlacement.findGround(
			new Vec3(4.8, 64.7, 6.4),
			-64,
			pos -> pos.getY() == 60
		).orElseThrow();

		assertEquals(new BlockPos(4, 61, 6), target);
	}

	@Test
	void multipleEntitiesUseDistinctCenteredGridCells() {
		List<BlockPos> cells = SpawnPlacement.grid(new BlockPos(10, 65, -3), new SpawnQuantity(4));

		assertEquals(List.of(
			new BlockPos(9, 65, -4),
			new BlockPos(11, 65, -4),
			new BlockPos(9, 65, -2),
			new BlockPos(11, 65, -2)
		), cells);
	}

	@Test
	void oneEntityUsesTheExactTargetCell() {
		BlockPos origin = new BlockPos(10, 65, -3);

		assertEquals(List.of(origin), SpawnPlacement.grid(origin, new SpawnQuantity(1)));
	}

	@Test
	void maximumGridContainsSixteenDistinctCells() {
		List<BlockPos> cells = SpawnPlacement.grid(BlockPos.ZERO, new SpawnQuantity(16));

		assertEquals(16, cells.size());
		assertEquals(16, new HashSet<>(cells).size());
	}

	@Test
	void multiTypeBatchUsesOneDistinctGridForItsTotalEntityCount() {
		List<BlockPos> cells = SpawnPlacement.grid(BlockPos.ZERO, 32);

		assertEquals(32, cells.size());
		assertEquals(32, new HashSet<>(cells).size());
	}

	@Test
	void airFallbackFailsCleanlyWhenThereIsNoGround() {
		assertTrue(SpawnPlacement.findGround(Vec3.ZERO, -64, pos -> false).isEmpty());
	}
}
