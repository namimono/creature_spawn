package com.namimono.creaturespawn.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Seam: 单种活体生成数量的缺省值与合法范围。
 */
class SpawnQuantityTest {

	@Test
	void omittedQuantityDefaultsToOne() {
		assertEquals(1, SpawnQuantity.fromNullable(null).value());
	}

	@Test
	void quantityBelowOneIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> SpawnQuantity.fromNullable(0));
	}

	@Test
	void quantityAboveSixteenIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> SpawnQuantity.fromNullable(17));
	}

	@Test
	void boundaryValuesAreAccepted() {
		assertEquals(1, SpawnQuantity.fromNullable(1).value());
		assertEquals(16, SpawnQuantity.fromNullable(16).value());
	}
}
