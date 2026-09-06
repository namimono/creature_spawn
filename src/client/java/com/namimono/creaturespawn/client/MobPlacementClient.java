package com.namimono.creaturespawn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.namimono.creaturespawn.command.MobPlacement;
import com.namimono.creaturespawn.command.SpawnCatalog;
import com.namimono.creaturespawn.command.SpawnEntry;
import com.namimono.creaturespawn.command.SpawnQuantity;
import com.namimono.creaturespawn.command.SpawnedMobs;
import com.namimono.creaturespawn.network.PlaceMobC2SPayload;
import com.namimono.creaturespawn.network.PlacementModeC2SPayload;
import com.namimono.creaturespawn.network.SelectPlacedMobC2SPayload;
import com.namimono.creaturespawn.network.SpawnCatalogC2SPayload;
import java.util.ArrayDeque;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** 客户端摆放：图鉴队列预览右键放下，左键改选已放下的再挪。 */
public final class MobPlacementClient {
	private static KeyMapping toggleKey;
	private static boolean active;
	private static boolean lockPose;
	private static final ArrayDeque<SpawnEntry> queue = new ArrayDeque<>();
	private static int relocatingId = -1;
	private static Entity preview;
	private static ResourceLocation previewId;
	private static long lastActionTick = Long.MIN_VALUE;

	private MobPlacementClient() {
	}

	public static void register() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.creature_spawn.place_mode",
			GLFW.GLFW_KEY_R,
			"key.categories.creature_spawn"
		));
		ClientTickEvents.END_CLIENT_TICK.register(MobPlacementClient::tick);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			active ? InteractionResult.FAIL : InteractionResult.PASS
		);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!active) {
				return InteractionResult.PASS;
			}
			if (hand == InteractionHand.MAIN_HAND && level.isClientSide) {
				tryPlace(Minecraft.getInstance());
			}
			return InteractionResult.FAIL;
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!active) {
				return InteractionResult.PASS;
			}
			if (hand == InteractionHand.MAIN_HAND && level.isClientSide) {
				trySelect(Minecraft.getInstance());
			}
			return InteractionResult.FAIL;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!active) {
				return InteractionResult.PASS;
			}
			if (hand == InteractionHand.MAIN_HAND && level.isClientSide) {
				clearRelocate();
			}
			return InteractionResult.FAIL;
		});
		WorldRenderEvents.AFTER_ENTITIES.register(MobPlacementClient::renderGhost);
		HudRenderCallback.EVENT.register(MobPlacementClient::renderHud);
	}

	public static void begin(List<ResourceLocation> entityIds, SpawnQuantity quantity, boolean lockPose) {
		Minecraft client = Minecraft.getInstance();
		if (FlashbackReplay.isActive()
			|| client.player == null
			|| !client.player.hasPermissions(2)
			|| !ClientPlayNetworking.canSend(SpawnCatalogC2SPayload.TYPE)) {
			return;
		}
		List<SpawnEntry> entries = entityIds.stream()
			.map(SpawnCatalog::find)
			.flatMap(java.util.Optional::stream)
			.toList();
		if (entries.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new SpawnCatalogC2SPayload(entityIds, quantity, lockPose, true));
		queue.clear();
		queue.addAll(MobPlacement.queue(entries, quantity));
		MobPlacementClient.lockPose = lockPose;
		relocatingId = -1;
		preview = null;
		previewId = null;
		active = true;
	}

	public static void enable() {
		Minecraft client = Minecraft.getInstance();
		if (FlashbackReplay.isActive()
			|| client.player == null
			|| !client.player.hasPermissions(2)
			|| !ClientPlayNetworking.canSend(PlacementModeC2SPayload.TYPE)) {
			return;
		}
		active = true;
		ClientPlayNetworking.send(new PlacementModeC2SPayload(true));
	}

	public static void disable() {
		if (!active) {
			clearLocal();
			return;
		}
		active = false;
		clearLocal();
		if (ClientPlayNetworking.canSend(PlacementModeC2SPayload.TYPE)) {
			ClientPlayNetworking.send(new PlacementModeC2SPayload(false));
		}
	}

	private static void clearLocal() {
		queue.clear();
		relocatingId = -1;
		preview = null;
		previewId = null;
		lockPose = false;
	}

	private static void tick(Minecraft client) {
		if (FlashbackReplay.isActive() && active) {
			disable();
			return;
		}
		if (client.player == null || client.level == null || client.screen != null) {
			return;
		}
		if (toggleKey.consumeClick()) {
			if (active) {
				disable();
			} else {
				enable();
			}
		}
		if (!active) {
			return;
		}
		if (client.options.keyAttack.consumeClick()
			&& !(client.hitResult instanceof EntityHitResult)
			&& !(client.hitResult instanceof BlockHitResult && client.hitResult.getType() == HitResult.Type.BLOCK)) {
			clearRelocate();
		}
	}

	private static void clearRelocate() {
		if (relocatingId < 0 || !ClientPlayNetworking.canSend(SelectPlacedMobC2SPayload.TYPE)) {
			relocatingId = -1;
			return;
		}
		relocatingId = -1;
		ClientPlayNetworking.send(new SelectPlacedMobC2SPayload(-1));
	}

	private static void tryPlace(Minecraft client) {
		if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		if (!canAct(client) || !ClientPlayNetworking.canSend(PlaceMobC2SPayload.TYPE)) {
			return;
		}
		if (relocatingId < 0 && queue.isEmpty()) {
			return;
		}
		ClientPlayNetworking.send(new PlaceMobC2SPayload(hit.getBlockPos(), hit.getDirection()));
		if (relocatingId >= 0) {
			relocatingId = -1;
		} else {
			queue.poll();
			preview = null;
			previewId = null;
		}
		lastActionTick = client.level.getGameTime();
	}

	private static void trySelect(Minecraft client) {
		if (!canAct(client) || !ClientPlayNetworking.canSend(SelectPlacedMobC2SPayload.TYPE)) {
			return;
		}
		int entityId = -1;
		if (client.hitResult instanceof EntityHitResult hit && SpawnedMobs.isMarked(hit.getEntity())) {
			entityId = hit.getEntity().getId();
		}
		relocatingId = entityId;
		ClientPlayNetworking.send(new SelectPlacedMobC2SPayload(entityId));
		lastActionTick = client.level.getGameTime();
	}

	private static boolean canAct(Minecraft client) {
		long gameTime = client.level.getGameTime();
		if (lastActionTick == gameTime) {
			return false;
		}
		return true;
	}

	private static void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (!active) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Component hint;
		if (relocatingId >= 0) {
			hint = Component.translatable(
				"hud.creature_spawn.place_mode.move",
				toggleKey.getTranslatedKeyMessage()
			);
		} else if (!queue.isEmpty()) {
			hint = Component.translatable(
				"hud.creature_spawn.place_mode.place",
				queue.peek().description(),
				queue.size(),
				toggleKey.getTranslatedKeyMessage()
			);
		} else {
			hint = Component.translatable(
				"hud.creature_spawn.place_mode.select",
				toggleKey.getTranslatedKeyMessage()
			);
		}
		graphics.drawCenteredString(
			client.font,
			hint,
			client.getWindow().getGuiScaledWidth() / 2,
			client.getWindow().getGuiScaledHeight() - 56,
			0xFFFFFF00
		);
	}

	private static void renderGhost(WorldRenderContext context) {
		if (!active) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		Entity ghost = currentGhost(client);
		if (ghost == null) {
			return;
		}

		BlockPos cell = MobPlacement.cell(hit.getBlockPos(), hit.getDirection());
		Vec3 feet = MobPlacement.feet(cell);
		float yRot = client.player.getYRot();
		ghost.moveTo(feet.x, feet.y, feet.z, yRot, 0.0F);
		ghost.setYHeadRot(yRot);

		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		dispatcher.render(
			ghost,
			ghost.getX(),
			ghost.getY(),
			ghost.getZ(),
			yRot,
			partialTick,
			context.matrixStack(),
			context.consumers(),
			LightTexture.FULL_BRIGHT
		);
		dispatcher.setRenderShadow(true);

		Vec3 camera = context.camera().getPosition();
		AABB box = new AABB(cell);
		PoseStack pose = context.matrixStack();
		pose.pushPose();
		LevelRenderer.renderLineBox(
			pose,
			context.consumers().getBuffer(RenderType.lines()),
			box.minX - camera.x,
			box.minY - camera.y,
			box.minZ - camera.z,
			box.maxX - camera.x,
			box.maxY - camera.y,
			box.maxZ - camera.z,
			1.0F,
			0.85F,
			0.2F,
			0.9F
		);
		pose.popPose();
	}

	private static Entity currentGhost(Minecraft client) {
		if (relocatingId >= 0) {
			Entity selected = client.level.getEntity(relocatingId);
			if (selected instanceof Mob mob) {
				return previewOf(mob);
			}
			relocatingId = -1;
		}
		SpawnEntry next = queue.peek();
		if (next == null || client.level == null) {
			return null;
		}
		return previewOf(next);
	}

	private static Entity previewOf(Mob source) {
		if (preview == null || preview.getType() != source.getType() || preview.level() != source.level()) {
			preview = source.getType().create(source.level());
			previewId = null;
		}
		if (preview instanceof Creeper ghost && source instanceof Creeper live && live.isPowered()) {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("powered", true);
			ghost.readAdditionalSaveData(tag);
		}
		return preview;
	}

	private static Entity previewOf(SpawnEntry entry) {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? null : previewOf(entry, client.level);
	}

	private static Entity previewOf(SpawnEntry entry, net.minecraft.world.level.Level level) {
		if (preview == null || previewId == null || !previewId.equals(entry.id()) || preview.level() != level) {
			preview = entry.type().create(level);
			previewId = entry.id();
			if (preview instanceof Mob mob) {
				entry.prepare(mob);
			}
		}
		return preview;
	}
}
