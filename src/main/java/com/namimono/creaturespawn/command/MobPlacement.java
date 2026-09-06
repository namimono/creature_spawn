package com.namimono.creaturespawn.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 把选中的生物摆到准星点中的方块面上，规则与摆方块相同。
 */
public final class MobPlacement {
	private MobPlacement() {
	}

	public static BlockPos cell(BlockPos hitBlock, Direction face) {
		return SpawnPlacement.forBlockHit(hitBlock, face);
	}

	public static List<SpawnEntry> queue(List<SpawnEntry> entries, SpawnQuantity quantity) {
		List<SpawnEntry> queue = new ArrayList<>(Math.multiplyExact(entries.size(), quantity.value()));
		for (SpawnEntry entry : entries) {
			for (int index = 0; index < quantity.value(); index++) {
				queue.add(entry);
			}
		}
		return queue;
	}

	public static Vec3 feet(BlockPos cell) {
		return new Vec3(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5);
	}

	public static boolean move(Mob mob, BlockPos cell, float yRot) {
		if (!ServerLevel.isInSpawnableBounds(cell)) {
			return false;
		}
		Vec3 feet = feet(cell);
		mob.moveTo(feet.x, feet.y, feet.z, yRot, 0.0F);
		if (mob.getAttached(SpawnPoseLocks.TYPE) != null) {
			SpawnPoseLocks.lock(mob);
		}
		return true;
	}
}
