package com.namimono.creaturespawn.command;

/**
 * 单种活体生成数量；命令与图鉴共用同一范围。
 */
public record SpawnQuantity(int value) {
	public static final int MIN = 1;
	public static final int MAX = 16;
	public static final int DEFAULT = 1;

	public SpawnQuantity {
		if (value < MIN || value > MAX) {
			throw new IllegalArgumentException("quantity must be between " + MIN + " and " + MAX);
		}
	}

	public static SpawnQuantity fromNullable(Integer requested) {
		return new SpawnQuantity(requested == null ? DEFAULT : requested);
	}
}
