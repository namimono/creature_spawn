package com.namimono.creaturespawn.command;

import com.namimono.creaturespawn.CreatureSpawn;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Entity;

/**
 * 把姿态锁挂到实体上，并在每拍结束时写回位置与朝向。
 */
public final class SpawnPoseLocks {
	public static final AttachmentType<SpawnPoseLock> TYPE = AttachmentRegistry.create(
		CreatureSpawn.id("pose_lock"),
		builder -> builder
			.persistent(SpawnPoseLock.CODEC)
			.syncWith(SpawnPoseLock.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	private SpawnPoseLocks() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(level -> applyAll(level.getAllEntities()));
	}

	public static void lock(Entity entity) {
		entity.setNoGravity(true);
		entity.setAttached(TYPE, SpawnPoseLock.of(entity));
		applyIfPresent(entity);
	}

	public static void applyAll(Iterable<Entity> entities) {
		for (Entity entity : entities) {
			applyIfPresent(entity);
		}
	}

	public static void applyIfPresent(Entity entity) {
		SpawnPoseLock lock = entity.getAttached(TYPE);
		if (lock != null) {
			lock.apply(entity);
		}
	}
}
