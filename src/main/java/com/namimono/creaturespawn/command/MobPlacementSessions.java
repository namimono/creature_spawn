package com.namimono.creaturespawn.command;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 服务端摆放会话：右键方块放下队列中的下一只，或挪开已选中的。
 */
public final class MobPlacementSessions {
	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

	private MobPlacementSessions() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide) {
				return InteractionResult.PASS;
			}
			return isActive(player) ? InteractionResult.FAIL : InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			return useBlock(serverPlayer, level, hand, hit);
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide) {
				return InteractionResult.PASS;
			}
			return attackEntity(player, hand, entity);
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (level.isClientSide) {
				return InteractionResult.PASS;
			}
			return attackBlock(player, hand);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.getPlayer()));
	}

	public static boolean begin(
		ServerPlayer player,
		Collection<net.minecraft.resources.ResourceLocation> selectedIds,
		SpawnQuantity quantity,
		boolean lockPose
	) {
		if (!player.hasPermissions(2)) {
			return false;
		}
		SpawnPlan plan = SpawnPlan.prepare(selectedIds, quantity).orElse(null);
		if (plan == null) {
			return false;
		}
		SESSIONS.put(player.getUUID(), new Session(
			new ArrayDeque<>(MobPlacement.queue(plan.entries(), plan.quantity())),
			lockPose
		));
		return true;
	}

	public static void setActive(Player player, boolean active) {
		if (!active || !player.hasPermissions(2)) {
			SESSIONS.remove(player.getUUID());
			return;
		}
		SESSIONS.putIfAbsent(player.getUUID(), new Session(new ArrayDeque<>(), false));
	}

	public static boolean isActive(Player player) {
		return SESSIONS.containsKey(player.getUUID());
	}

	public static boolean placeAt(ServerPlayer player, BlockPos hitBlock, Direction face) {
		return placeAt(player, MobPlacement.cell(hitBlock, face));
	}

	public static boolean selectPlaced(Player player, int entityId) {
		Session session = SESSIONS.get(player.getUUID());
		if (session == null || !player.hasPermissions(2)) {
			return false;
		}
		if (entityId < 0) {
			session.relocatingId = -1;
			return true;
		}
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		Entity entity = serverLevel.getEntity(entityId);
		if (!SpawnedMobs.isMarked(entity)) {
			return false;
		}
		session.relocatingId = entityId;
		return true;
	}

	static InteractionResult useBlock(ServerPlayer player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (!isActive(player)) {
			return InteractionResult.PASS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.FAIL;
		}
		return placeAt(player, MobPlacement.cell(hit.getBlockPos(), hit.getDirection()))
			? InteractionResult.SUCCESS
			: InteractionResult.FAIL;
	}

	static InteractionResult attackEntity(Player player, InteractionHand hand, Entity entity) {
		if (!isActive(player)) {
			return InteractionResult.PASS;
		}
		if (hand == InteractionHand.MAIN_HAND) {
			selectPlaced(player, SpawnedMobs.isMarked(entity) ? entity.getId() : -1);
		}
		return InteractionResult.FAIL;
	}

	static InteractionResult attackBlock(Player player, InteractionHand hand) {
		if (!isActive(player)) {
			return InteractionResult.PASS;
		}
		if (hand == InteractionHand.MAIN_HAND) {
			selectPlaced(player, -1);
		}
		return InteractionResult.FAIL;
	}

	private static boolean placeAt(ServerPlayer player, BlockPos cell) {
		Session session = SESSIONS.get(player.getUUID());
		if (session == null || !(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		long gameTime = serverLevel.getGameTime();
		if (session.lastActionTick == gameTime) {
			return true;
		}
		if (session.relocatingId >= 0) {
			Entity entity = serverLevel.getEntity(session.relocatingId);
			if (!(entity instanceof Mob mob) || !SpawnedMobs.isMarked(mob)) {
				session.relocatingId = -1;
				return false;
			}
			if (!MobPlacement.move(mob, cell, player.getYRot())) {
				return false;
			}
			session.relocatingId = -1;
			session.lastActionTick = gameTime;
			return true;
		}
		SpawnEntry next = session.queue.poll();
		if (next == null) {
			return false;
		}
		if (!LivingSpawner.place(player, next, cell, player.getYRot(), session.lockPose)) {
			session.queue.addFirst(next);
			return false;
		}
		session.lastActionTick = gameTime;
		return true;
	}

	private static void clear(Player player) {
		SESSIONS.remove(player.getUUID());
	}

	private static final class Session {
		private final ArrayDeque<SpawnEntry> queue;
		private final boolean lockPose;
		private int relocatingId = -1;
		private long lastActionTick = Long.MIN_VALUE;

		private Session(ArrayDeque<SpawnEntry> queue, boolean lockPose) {
			this.queue = queue;
			this.lockPose = lockPose;
		}
	}
}
