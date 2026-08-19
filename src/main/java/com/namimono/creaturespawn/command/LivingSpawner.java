package com.namimono.creaturespawn.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 服务端共用的活体生成入口；命令与图鉴都通过这里生成。
 */
public final class LivingSpawner {
	private LivingSpawner() {
	}

	/**
	 * @return 实际生成数量；失败时为 0，且不会留下本次已加入世界的实体
	 */
	public static int spawn(ServerPlayer player, EntityType<?> type, SpawnQuantity quantity) {
		return spawn(player, List.of(type), quantity);
	}

	/**
	 * 为每种活体生成相同数量，并让整个批次共用一次落点和一张不重叠网格。
	 *
	 * @return 实际生成数量；失败时为 0，且不会留下本次已加入世界的实体
	 */
	public static int spawn(
		ServerPlayer player,
		List<EntityType<?>> types,
		SpawnQuantity quantity
	) {
		if (types.isEmpty() || types.stream().anyMatch(type ->
			!SpawnCatalog.allows(EntityType.getKey(type), type)
		)) {
			return 0;
		}
		ServerLevel level = player.serverLevel();
		Optional<BlockPos> target = target(player, level);
		if (target.isEmpty()) {
			return 0;
		}

		int totalCount = Math.multiplyExact(types.size(), quantity.value());
		List<BlockPos> cells = SpawnPlacement.grid(target.orElseThrow(), totalCount);
		if (cells.stream().anyMatch(pos -> !ServerLevel.isInSpawnableBounds(pos))) {
			return 0;
		}

		List<Mob> prepared = new ArrayList<>(cells.size());
		int cellIndex = 0;
		for (EntityType<?> type : types) {
			for (int index = 0; index < quantity.value(); index++) {
				BlockPos cell = cells.get(cellIndex++);
				Entity entity = type.create(level);
				if (!(entity instanceof Mob mob)) {
					return 0;
				}
				mob.moveTo(
					cell.getX() + 0.5,
					cell.getY(),
					cell.getZ() + 0.5,
					player.getYRot(),
					0.0F
				);
				prepared.add(mob);
			}
		}

		List<Mob> added = new ArrayList<>(prepared.size());
		for (Mob mob : prepared) {
			if (!level.tryAddFreshEntityWithPassengers(mob)) {
				added.forEach(Entity::discard);
				return 0;
			}
			added.add(mob);
		}
		return added.size();
	}

	private static Optional<BlockPos> target(ServerPlayer player, ServerLevel level) {
		HitResult hit = player.pick(SpawnPlacement.AIR_FALLBACK_DISTANCE, 1.0F, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return Optional.of(SpawnPlacement.forBlockHit(blockHit.getBlockPos(), blockHit.getDirection()));
		}

		Vec3 probe = SpawnPlacement.fallbackProbe(player.getEyePosition(), player.getViewVector(1.0F));
		return SpawnPlacement.findGround(
			probe,
			level.getMinBuildHeight(),
			pos -> level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)
		);
	}
}
