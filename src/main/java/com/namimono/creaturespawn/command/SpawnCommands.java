package com.namimono.creaturespawn.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.namimono.creaturespawn.network.OpenSpawnCatalogS2CPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * 模组指令：打开图鉴或直接刷一种原版活体。
 */
public final class SpawnCommands {
	private static final DynamicCommandExceptionType UNKNOWN_ENTITY = new DynamicCommandExceptionType(
		token -> Component.translatable("commands.creature_spawn.unknown_entity", token)
	);
	private static final SimpleCommandExceptionType SPAWN_FAILED = new SimpleCommandExceptionType(
		Component.translatable("commands.creature_spawn.failed")
	);
	private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (ctx, builder) ->
		SharedSuggestionProvider.suggestResource(
			SpawnCatalog.entries().stream().map(EntityType::getKey),
			builder
		);

	private SpawnCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		register(dispatcher, SpawnCommands::spawnInWorld, SpawnCommands::openCatalogForPlayer);
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher, SpawnAction spawnAction) {
		register(dispatcher, spawnAction, SpawnCommands::openCatalogForPlayer);
	}

	static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		SpawnAction spawnAction,
		OpenCatalogAction openCatalogAction
	) {
		dispatcher.register(Commands.literal("creature_spawn")
			.requires(source -> source.hasPermission(2))
			.executes(ctx -> openCatalogAction.open(ctx.getSource()))
			.then(Commands.argument("entity", ResourceLocationArgument.id())
				.suggests(ENTITY_SUGGESTIONS)
				.executes(ctx -> spawn(ctx, null, spawnAction))
				.then(Commands.argument(
					"quantity",
					IntegerArgumentType.integer(SpawnQuantity.MIN, SpawnQuantity.MAX)
				).executes(ctx -> spawn(
					ctx,
					IntegerArgumentType.getInteger(ctx, "quantity"),
					spawnAction
				)))));
	}

	private static int spawn(
		CommandContext<CommandSourceStack> ctx,
		Integer requestedQuantity,
		SpawnAction spawnAction
	)
		throws CommandSyntaxException {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, "entity");
		EntityType<?> type = SpawnCatalog.find(id)
			.orElseThrow(() -> UNKNOWN_ENTITY.create(id));
		SpawnQuantity quantity = SpawnQuantity.fromNullable(requestedQuantity);
		int spawned = spawnAction.spawn(ctx.getSource(), type, quantity);
		if (spawned == 0) {
			throw SPAWN_FAILED.create();
		}
		ctx.getSource().sendSuccess(
			() -> Component.translatable(
				"commands.creature_spawn.success",
				spawned,
				type.getDescription()
			),
			true
		);
		return spawned;
	}

	private static int spawnInWorld(CommandSourceStack source, EntityType<?> type, SpawnQuantity quantity)
		throws CommandSyntaxException {
		return LivingSpawner.spawn(source.getPlayerOrException(), type, quantity);
	}

	private static int openCatalogForPlayer(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayNetworking.send(source.getPlayerOrException(), new OpenSpawnCatalogS2CPayload());
		return Command.SINGLE_SUCCESS;
	}

	@FunctionalInterface
	interface SpawnAction {
		int spawn(CommandSourceStack source, EntityType<?> type, SpawnQuantity quantity)
			throws CommandSyntaxException;
	}

	@FunctionalInterface
	interface OpenCatalogAction {
		int open(CommandSourceStack source) throws CommandSyntaxException;
	}
}
