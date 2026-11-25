package dev.brian.snapdrift.client;

import dev.brian.snapdrift.Snapdrift;
import dev.brian.snapdrift.core.BenchReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Boots the client, joins a world, runs a real 5-second benchmark and checks it produced a CSV row.
 * Mixin check: the mixin config uses {@code "defaultRequire": 1}, so a mixin that fails to apply crashes
 * the boot, which fails this test.
 */
public class SnapdriftClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded(Snapdrift.MOD_ID)) {
			throw new AssertionError(Snapdrift.MOD_ID + " is not loaded");
		}
		context.waitForScreen(TitleScreen.class);
		context.waitTicks(20);
		context.takeScreenshot("title_screen");

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(100);

			List<String> warnings = context.computeOnClient(mc -> EnvironmentNotice.currentWarnings());
			Snapdrift.LOGGER.info("[gametest] environment warnings: {}", warnings);

			double[] before = context.computeOnClient(mc -> {
				try {
					BenchController.saveStart(mc.player);
					BenchController.start(mc, "gametest", 5);
				} catch (IOException e) {
					throw new AssertionError("benchmark failed to start", e);
				}
				return new double[] { mc.player.getX(), mc.player.getZ() };
			});
			context.waitTicks(40);
			context.takeScreenshot("bench_running");

			int waited = 0;
			while (context.computeOnClient(mc -> BenchController.isRunning())) {
				context.waitTicks(20);
				waited += 20;
				if (waited > 20 * 30) throw new AssertionError("benchmark did not finish within 30 s");
			}

			BenchReport report = context.computeOnClient(mc -> BenchController.lastReport());
			if (report == null || report.stats().count() < 10) {
				throw new AssertionError("benchmark recorded too few frames: " + (report == null ? "no report" : report.summary()));
			}
			double[] after = context.computeOnClient(mc -> new double[] { mc.player.getX(), mc.player.getZ() });
			double moved = Math.hypot(after[0] - before[0], after[1] - before[1]);
			if (moved < 30) {
				throw new AssertionError("camera should fly ~50 blocks along the route, moved " + moved);
			}

			Path csv = context.computeOnClient(mc -> BenchController.lastResultFile());
			try {
				List<String> lines = Files.readAllLines(csv);
				if (lines.size() < 2 || !lines.get(0).equals(BenchReport.csvHeader()) || !lines.get(lines.size() - 1).contains("gametest")) {
					throw new AssertionError("unexpected CSV contents in " + csv + ": " + lines);
				}
			} catch (IOException e) {
				throw new AssertionError("could not read " + csv, e);
			}
			Snapdrift.LOGGER.info("[gametest] {}", report.summary());
			context.takeScreenshot("bench_done");
		}
	}
}
