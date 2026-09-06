package com.namimono.creaturespawn.command;

import com.mojang.serialization.Codec;
import com.namimono.creaturespawn.CreatureSpawn;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * 本模组生成的活体会打上标记，摆放模式只动这些。
 */
public final class SpawnedMobs {
	public static final String TAG = "creature_spawn.spawned";
	public static final AttachmentType<Boolean> TYPE = AttachmentRegistry.create(
		CreatureSpawn.id("spawned"),
		builder -> builder
			.persistent(Codec.BOOL)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
	);

	private SpawnedMobs() {
	}

	public static void bootstrap() {
		// 触发附件注册，保证进世界前类型已存在。
	}

	public static void mark(Entity entity) {
		entity.addTag(TAG);
		entity.setAttached(TYPE, true);
	}

	public static boolean isMarked(Entity entity) {
		return entity instanceof Mob
			&& (Boolean.TRUE.equals(entity.getAttached(TYPE)) || entity.getTags().contains(TAG));
	}
}
