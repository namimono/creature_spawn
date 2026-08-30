package com.namimono.creaturespawn.client;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;

/**
 * 可选对接 Flashback：不把该模组写进硬依赖。
 * 回放会重放「打开图鉴」的自定义包，页面会一直挡在画面上。
 */
public final class FlashbackReplay {
	private static final String REPLAY_SERVER_CLASS = "com.moulberry.flashback.playback.ReplayServer";
	private static final Method IS_IN_REPLAY = findIsInReplay();

	private FlashbackReplay() {
	}

	public static boolean isActive() {
		if (IS_IN_REPLAY != null) {
			try {
				return (boolean) IS_IN_REPLAY.invoke(null);
			} catch (ReflectiveOperationException ignored) {
				// 版本对不上时退回看本机是否正在跑回放服。
			}
		}
		return replayServerRunning();
	}

	private static boolean replayServerRunning() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return false;
		}
		IntegratedServer server = client.getSingleplayerServer();
		return server != null && REPLAY_SERVER_CLASS.equals(server.getClass().getName());
	}

	private static Method findIsInReplay() {
		try {
			return Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			return null;
		}
	}
}
