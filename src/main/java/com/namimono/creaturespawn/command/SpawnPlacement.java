package com.namimono.creaturespawn.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 刷怪落点与网格的纯位置规则。
 */
public final class SpawnPlacement {
	public static final double AIR_FALLBACK_DISTANCE = 8.0;
	public static final int GRID_SPACING = 2;

	private SpawnPlacement() {
	}

	public static BlockPos forBlockHit(BlockPos block, Direction face) {
		return block.relative(face);
	}

	public static Vec3 fallbackProbe(Vec3 eye, Vec3 view) {
		return eye.add(view.normalize().scale(AIR_FALLBACK_DISTANCE));
	}

	public static Optional<BlockPos> findGround(Vec3 probe, int minimumY, Predicate<BlockPos> isSolid) {
		BlockPos probeBlock = BlockPos.containing(probe);
		for (int y = probeBlock.getY(); y >= minimumY; y--) {
			BlockPos candidate = new BlockPos(probeBlock.getX(), y, probeBlock.getZ());
			if (isSolid.test(candidate)) {
				return Optional.of(candidate.above());
			}
		}
		return Optional.empty();
	}

	public static List<BlockPos> grid(BlockPos origin, SpawnQuantity quantity) {
		return grid(origin, quantity.value());
	}

	public static List<BlockPos> grid(BlockPos origin, int count) {
		if (count < 1) {
			throw new IllegalArgumentException("grid count must be positive");
		}
		int columns = (int) Math.ceil(Math.sqrt(count));
		int rows = (count + columns - 1) / columns;
		int startX = -((columns - 1) * GRID_SPACING) / 2;
		int startZ = -((rows - 1) * GRID_SPACING) / 2;
		List<BlockPos> cells = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			int column = index % columns;
			int row = index / columns;
			cells.add(origin.offset(
				startX + column * GRID_SPACING,
				0,
				startZ + row * GRID_SPACING
			));
		}
		return List.copyOf(cells);
	}
}
