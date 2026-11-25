package dev.brian.snapdrift.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import dev.brian.snapdrift.Snapdrift;
import dev.brian.snapdrift.core.BenchPath;
import dev.brian.snapdrift.core.BenchReport;
import dev.brian.snapdrift.core.BenchStart;
import dev.brian.snapdrift.core.EnvironmentCheck;
import dev.brian.snapdrift.core.FrameStats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

/**
 * Runs one benchmark: settle at the route start, then fly the fixed route at constant speed while
 * recording every frame time, then append a CSV row and print a summary.
 */
public final class BenchController {
	/** Flight speed in blocks per second (about creative sprint-flying). */
	public static final double SPEED = 10.0;
	/** Seconds spent parked at the start before recording, so the teleport's chunk burst is not measured. */
	public static final double SETTLE_SECONDS = 3.0;

	/** Mod ids worth recording in each row so a CSV row says which setup produced it. */
	static final List<String> PERF_MODS = List.of("sodium", "lithium", "ferritecore", "immediatelyfast",
		"entityculling", "moreculling", "dynamic_fps", "c2me", "scalablelux", "distanthorizons", "voxy",
		"sodium-extra", "reeses-sodium-options", "badoptimizations", "iris", "krypton", "bobby");

	private static Run current;
	private static Path lastResultFile;
	private static BenchReport lastReport;

	private BenchController() {}

	private static final class Run {
		final String name;
		final int seconds;
		final BenchPath path;
		final GameType previousMode;
		final FrameStats stats = new FrameStats();
		long startNanos = -1;
		long lastFrameNanos = -1;

		Run(String name, int seconds, BenchPath path, GameType previousMode) {
			this.name = name;
			this.seconds = seconds;
			this.path = path;
			this.previousMode = previousMode;
		}
	}

	public static boolean isRunning() {
		return current != null;
	}

	public static BenchReport lastReport() {
		return lastReport;
	}

	public static Path lastResultFile() {
		return lastResultFile;
	}

	public static Path dataDir() {
		return FabricLoader.getInstance().getGameDir().resolve("snapdrift");
	}

	/** Saved route start, or null. */
	public static BenchStart loadStart() {
		try {
			Path f = dataDir().resolve("route.txt");
			return Files.exists(f) ? BenchStart.parse(Files.readString(f)) : null;
		} catch (IOException e) {
			return null;
		}
	}

	public static BenchStart saveStart(LocalPlayer player) throws IOException {
		BenchStart start = new BenchStart(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		Files.createDirectories(dataDir());
		Files.writeString(dataDir().resolve("route.txt"), start.serialize());
		return start;
	}

	/** Starts a run. Returns the chat lines to show (warnings first). */
	public static List<Component> start(Minecraft mc, String name, int seconds) throws IOException {
		List<Component> out = new ArrayList<>();
		if (current != null) {
			out.add(Component.literal("A benchmark is already running.").withStyle(ChatFormatting.RED));
			return out;
		}
		LocalPlayer player = mc.player;
		BenchStart start = loadStart();
		if (start == null) {
			start = saveStart(player);
			out.add(Component.literal("No route start saved yet: using and saving your current position.")
				.withStyle(ChatFormatting.YELLOW));
		}
		for (String w : EnvironmentCheck.benchWarnings(mc.options.enableVsync().get(), mc.options.framerateLimit().get(),
			FabricLoader.getInstance().isModLoaded("dynamic_fps"))) {
			out.add(Component.literal("[bench] " + w).withStyle(ChatFormatting.YELLOW));
		}
		GameType previous = mc.gameMode != null ? mc.gameMode.getPlayerMode() : null;
		if (previous != GameType.SPECTATOR) {
			if (canChangeGameMode(player)) {
				player.connection.sendCommand("gamemode spectator");
			} else {
				previous = null; // nothing to restore
				out.add(Component.literal("[bench] No /gamemode permission: flying in your current mode (mobs and "
					+ "collisions may interfere). Enable cheats for cleaner runs.").withStyle(ChatFormatting.YELLOW));
			}
		}
		current = new Run(name, seconds, new BenchPath(start, SPEED), previous);
		out.add(Component.literal(String.format("Benchmark '%s': %.0fs settle, then %ds flight. Hands off!",
			name, SETTLE_SECONDS, seconds)).withStyle(ChatFormatting.AQUA));
		return out;
	}

	/** The server only sends command nodes the player may use, so a missing node means no permission. */
	private static boolean canChangeGameMode(LocalPlayer player) {
		return player.connection.getCommands().getRoot().getChild("gamemode") != null;
	}

	/** Called at the start of every rendered frame. */
	public static void onFrame() {
		Run run = current;
		if (run == null) return;
		long now = System.nanoTime();
		if (run.startNanos >= 0 && run.lastFrameNanos >= 0 && now > run.lastFrameNanos) {
			double elapsed = (now - run.startNanos) / 1e9;
			if (elapsed > SETTLE_SECONDS) {
				run.stats.add(now - run.lastFrameNanos);
			}
		}
		run.lastFrameNanos = now;
	}

	/** Called at the end of every client tick: moves the camera along the route and ends the run. */
	public static void onTick(Minecraft mc) {
		Run run = current;
		if (run == null) return;
		LocalPlayer player = mc.player;
		if (player == null) {
			current = null;
			Snapdrift.LOGGER.warn("Benchmark '{}' aborted: left the world", run.name);
			return;
		}
		long now = System.nanoTime();
		if (run.startNanos < 0) run.startNanos = now;
		double elapsed = (now - run.startNanos) / 1e9;
		double flight = Math.max(0.0, elapsed - SETTLE_SECONDS);

		double[] p = run.path.positionAt(flight);
		BenchStart s = run.path.start();
		player.setPos(p[0], p[1], p[2]);
		player.setDeltaMovement(0, 0, 0);
		player.setYRot(s.yaw());
		player.setXRot(s.pitch());
		player.yRotO = s.yaw();
		player.xRotO = s.pitch();
		if (flight <= 0.0) {
			player.setOldPosAndRot(); // parked: no interpolation smear from the teleport
		}

		if (flight >= run.seconds) {
			current = null;
			finish(mc, run);
		}
	}

	private static void finish(Minecraft mc, Run run) {
		DeviceInfo info = RenderSystem.getDevice().getDeviceInfo();
		List<String> mods = PERF_MODS.stream().filter(FabricLoader.getInstance()::isModLoaded).toList();
		BenchReport report = new BenchReport(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
			run.name, run.seconds, run.stats, mc.options.renderDistance().get(), mc.options.simulationDistance().get(),
			mc.options.enableVsync().get(), mc.options.framerateLimit().get(), info.name(), info.backendName(),
			System.getProperty("os.arch"), Runtime.getRuntime().maxMemory() / (1024 * 1024), mods, run.path.start());
		lastReport = report;
		try {
			Path dir = dataDir().resolve("bench");
			Files.createDirectories(dir);
			Path file = dir.resolve("results.csv");
			if (!Files.exists(file)) {
				Files.writeString(file, BenchReport.csvHeader() + System.lineSeparator(), StandardCharsets.UTF_8);
			}
			Files.writeString(file, report.csvRow() + System.lineSeparator(), StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
			lastResultFile = file;
		} catch (IOException e) {
			Snapdrift.LOGGER.error("Could not write benchmark results", e);
		}
		Snapdrift.LOGGER.info("[bench] {}", report.summary());
		LocalPlayer player = mc.player;
		if (player != null) {
			player.sendSystemMessage(Component.literal(report.summary()).withStyle(ChatFormatting.GREEN));
			if (run.previousMode != null && run.previousMode != GameType.SPECTATOR && canChangeGameMode(player)) {
				player.connection.sendCommand("gamemode " + run.previousMode.getName());
			}
		}
	}
}
