package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Seam: 服务端 `/creature_spawn` 命令树。
 */
class SpawnCommandsTest {
	@BeforeAll
	static void bootstrapRegistries() {
		MinecraftTestBootstrap.ensureInitialized();
	}

	@Test
	void spawnCommandExistsOnlyForPermissionLevelTwo() {
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher();
		CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("creature_spawn");

		assertNotNull(root);
		assertFalse(root.canUse(sourceWithPermission(1)));
		assertTrue(root.canUse(sourceWithPermission(2)));
	}

	@Test
	void entityIdAcceptsOmittedQuantityAndSixteen() {
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher();

		assertFullyParsed(dispatcher.parse(
			"creature_spawn minecraft:zombie",
			sourceWithPermission(2)
		));
		assertFullyParsed(dispatcher.parse(
			"creature_spawn minecraft:zombie 16",
			sourceWithPermission(2)
		));
	}

	@Test
	void quantityOutsideOneToSixteenDoesNotReachAnExecutableCommand() {
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher();

		assertRejected(dispatcher.parse(
			"creature_spawn minecraft:zombie 0",
			sourceWithPermission(2)
		));
		assertRejected(dispatcher.parse(
			"creature_spawn minecraft:zombie 17",
			sourceWithPermission(2)
		));
	}

	@Test
	void permissionFailureDoesNotInvokeTheWorldSpawnBoundary() {
		AtomicInteger attempts = new AtomicInteger();
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher(countingAction(attempts));

		assertThrows(
			CommandSyntaxException.class,
			() -> dispatcher.execute("creature_spawn minecraft:zombie", sourceWithPermission(1))
		);
		assertEquals(0, attempts.get());
	}

	@Test
	void spawnWithoutArgumentsOpensCatalogOnlyForPermissionLevelTwo() throws CommandSyntaxException {
		AtomicInteger opens = new AtomicInteger();
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		SpawnCommands.register(
			dispatcher,
			(source, type, quantity) -> quantity.value(),
			source -> opens.incrementAndGet()
		);

		assertThrows(
			CommandSyntaxException.class,
			() -> dispatcher.execute("creature_spawn", sourceWithPermission(1))
		);
		assertEquals(0, opens.get());

		assertEquals(1, dispatcher.execute("creature_spawn", sourceWithPermission(2)));
		assertEquals(1, opens.get());

		assertEquals(
			1,
			dispatcher.execute("creature_spawn minecraft:zombie", sourceWithPermission(2))
		);
		assertEquals(1, opens.get());
	}

	@Test
	void catalogFailureDoesNotInvokeTheWorldSpawnBoundary() {
		AtomicInteger attempts = new AtomicInteger();
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher(countingAction(attempts));

		assertThrows(
			CommandSyntaxException.class,
			() -> dispatcher.execute("creature_spawn minecraft:boat", sourceWithPermission(2))
		);
		assertEquals(0, attempts.get());
	}

	@Test
	void legalEntityAndQuantityInvokeTheWorldSpawnBoundary() throws CommandSyntaxException {
		AtomicReference<EntityType<?>> requestedType = new AtomicReference<>();
		AtomicReference<SpawnQuantity> requestedQuantity = new AtomicReference<>();
		CommandDispatcher<CommandSourceStack> dispatcher = registeredDispatcher((source, type, quantity) -> {
			requestedType.set(type);
			requestedQuantity.set(quantity);
			return quantity.value();
		});

		int result = dispatcher.execute(
			"creature_spawn minecraft:zombie 3",
			sourceWithPermission(2)
		);

		assertEquals(3, result);
		assertSame(EntityType.ZOMBIE, requestedType.get());
		assertEquals(new SpawnQuantity(3), requestedQuantity.get());
	}

	private static void assertFullyParsed(ParseResults<CommandSourceStack> result) {
		assertFalse(result.getReader().canRead(), () -> "remaining: " + result.getReader().getRemaining());
		assertTrue(result.getExceptions().isEmpty());
		assertNotNull(result.getContext().getCommand());
	}

	private static void assertRejected(ParseResults<CommandSourceStack> result) {
		assertTrue(result.getReader().canRead() || !result.getExceptions().isEmpty());
	}

	private static CommandDispatcher<CommandSourceStack> registeredDispatcher() {
		return registeredDispatcher((source, type, quantity) -> quantity.value());
	}

	private static CommandDispatcher<CommandSourceStack> registeredDispatcher(SpawnCommands.SpawnAction action) {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		SpawnCommands.register(dispatcher, action);
		return dispatcher;
	}

	private static SpawnCommands.SpawnAction countingAction(AtomicInteger attempts) {
		return (source, type, quantity) -> {
			attempts.incrementAndGet();
			return quantity.value();
		};
	}

	private static CommandSourceStack sourceWithPermission(int permission) {
		return new CommandSourceStack(
			CommandSource.NULL,
			Vec3.ZERO,
			Vec2.ZERO,
			null,
			permission,
			"test",
			Component.literal("test"),
			null,
			null
		);
	}
}
