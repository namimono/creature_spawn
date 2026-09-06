package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Seam: 固定位置与朝向的持久化格式。
 */
class SpawnPoseLockTest {
	@BeforeAll
	static void bootstrapRegistries() {
		MinecraftTestBootstrap.ensureInitialized();
	}

	@Test
	void codecRoundTripsLockedPose() {
		SpawnPoseLock lock = new SpawnPoseLock(1.5, 64.0, -8.25, 90.0F, 0.0F);

		var encoded = SpawnPoseLock.CODEC.encodeStart(NbtOps.INSTANCE, lock).getOrThrow();
		SpawnPoseLock decoded = SpawnPoseLock.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

		assertEquals(lock, decoded);
	}
}
