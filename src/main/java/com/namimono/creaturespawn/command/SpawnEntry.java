package com.namimono.creaturespawn.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;

/**
 * 图鉴里的一条可选项：原版实体，或同一实体的已知变体。
 */
public record SpawnEntry(ResourceLocation id, EntityType<?> type) {
	public void prepare(Mob mob) {
		if (SpawnCatalog.CHARGED_CREEPER.equals(id) && mob instanceof Creeper creeper) {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("powered", true);
			creeper.readAdditionalSaveData(tag);
		}
	}

	public Component description() {
		if (SpawnCatalog.CHARGED_CREEPER.equals(id)) {
			return Component.translatable("entity.creature_spawn.charged_creeper");
		}
		return type.getDescription();
	}
}
