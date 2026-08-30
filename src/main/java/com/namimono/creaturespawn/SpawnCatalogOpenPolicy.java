package com.namimono.creaturespawn;

/**
 * 回放时刷怪图鉴会挡住画面：收到打开请求时该不该开，已经开着要不要关。
 */
public final class SpawnCatalogOpenPolicy {
	public enum Decision {
		OPEN,
		CLOSE,
		LEAVE_CLOSED
	}

	private SpawnCatalogOpenPolicy() {
	}

	public static Decision decide(boolean replayActive, boolean catalogVisible) {
		if (!replayActive) {
			return Decision.OPEN;
		}
		return catalogVisible ? Decision.CLOSE : Decision.LEAVE_CLOSED;
	}

	public static boolean shouldRender(boolean replayActive) {
		return !replayActive;
	}
}
