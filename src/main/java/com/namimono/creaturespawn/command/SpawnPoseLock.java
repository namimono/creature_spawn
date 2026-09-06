package com.namimono.creaturespawn.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 生成时记下的位置与朝向；每拍写回实体，但不关掉 AI。
 */
public record SpawnPoseLock(double x, double y, double z, float yRot, float xRot) {
	public static final Codec<SpawnPoseLock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.DOUBLE.fieldOf("x").forGetter(SpawnPoseLock::x),
		Codec.DOUBLE.fieldOf("y").forGetter(SpawnPoseLock::y),
		Codec.DOUBLE.fieldOf("z").forGetter(SpawnPoseLock::z),
		Codec.FLOAT.fieldOf("y_rot").forGetter(SpawnPoseLock::yRot),
		Codec.FLOAT.fieldOf("x_rot").forGetter(SpawnPoseLock::xRot)
	).apply(instance, SpawnPoseLock::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnPoseLock> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE,
		SpawnPoseLock::x,
		ByteBufCodecs.DOUBLE,
		SpawnPoseLock::y,
		ByteBufCodecs.DOUBLE,
		SpawnPoseLock::z,
		ByteBufCodecs.FLOAT,
		SpawnPoseLock::yRot,
		ByteBufCodecs.FLOAT,
		SpawnPoseLock::xRot,
		SpawnPoseLock::new
	);

	public static SpawnPoseLock of(Entity entity) {
		return new SpawnPoseLock(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
	}

	public void apply(Entity entity) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setPos(x, y, z);
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.setYHeadRot(yRot);
		entity.xo = x;
		entity.yo = y;
		entity.zo = z;
		entity.yRotO = yRot;
		entity.xRotO = xRot;
		entity.fallDistance = 0.0F;
		entity.setOnGround(true);
		entity.hasImpulse = true;
		if (entity instanceof LivingEntity living) {
			living.setYBodyRot(yRot);
			living.yBodyRotO = yRot;
			living.yHeadRotO = yRot;
		}
	}
}
