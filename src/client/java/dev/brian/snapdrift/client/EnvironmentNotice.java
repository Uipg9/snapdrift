package dev.brian.snapdrift.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import dev.brian.snapdrift.Snapdrift;
import dev.brian.snapdrift.core.EnvironmentCheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Logs environment warnings every launch; shows them in chat only when they changed since last time. */
public final class EnvironmentNotice {
	private static boolean checkedThisSession;

	private EnvironmentNotice() {}

	public static List<String> currentWarnings() {
		DeviceInfo info = RenderSystem.getDevice().getDeviceInfo();
		return EnvironmentCheck.check(System.getProperty("os.arch"), System.getenv("PROCESSOR_IDENTIFIER"),
			info.name(), info.backendName(), Runtime.getRuntime().maxMemory());
	}

	public static void onJoin(Minecraft mc) {
		if (checkedThisSession || mc.player == null) return;
		checkedThisSession = true;
		List<String> warnings = currentWarnings();
		warnings.forEach(w -> Snapdrift.LOGGER.warn("[env] {}", w));
		if (warnings.isEmpty()) return;

		Path seen = BenchController.dataDir().resolve("envcheck.txt");
		String fingerprint = EnvironmentCheck.fingerprint(warnings);
		try {
			if (Files.exists(seen) && Files.readString(seen).trim().equals(fingerprint)) return;
			Files.createDirectories(seen.getParent());
			Files.writeString(seen, fingerprint);
		} catch (IOException e) {
			Snapdrift.LOGGER.warn("Could not record environment check", e);
		}
		mc.player.sendSystemMessage(Component.literal("[Snapdrift] Performance notes (shown once, see /snapdrift env):")
			.withStyle(ChatFormatting.GOLD));
		warnings.forEach(w -> mc.player.sendSystemMessage(Component.literal(" - " + w).withStyle(ChatFormatting.YELLOW)));
	}
}
