package com.namimono.creaturespawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Seam: 回放时不要打开刷怪图鉴，避免挡住画面。
 */
class SpawnCatalogOpenPolicyTest {
	@Test
	void normalPlayOpensTheCatalogEvenIfOneIsAlreadyVisible() {
		assertEquals(SpawnCatalogOpenPolicy.Decision.OPEN, SpawnCatalogOpenPolicy.decide(false, false));
		assertEquals(SpawnCatalogOpenPolicy.Decision.OPEN, SpawnCatalogOpenPolicy.decide(false, true));
		assertTrue(SpawnCatalogOpenPolicy.shouldRender(false));
	}

	@Test
	void replayLeavesTheCatalogClosed() {
		assertEquals(SpawnCatalogOpenPolicy.Decision.LEAVE_CLOSED, SpawnCatalogOpenPolicy.decide(true, false));
		assertFalse(SpawnCatalogOpenPolicy.shouldRender(true));
	}

	@Test
	void replayClosesAnAlreadyVisibleCatalog() {
		assertEquals(SpawnCatalogOpenPolicy.Decision.CLOSE, SpawnCatalogOpenPolicy.decide(true, true));
	}
}
