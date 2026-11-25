package dev.brian.snapdrift.client;

import dev.brian.snapdrift.Snapdrift;
import dev.brian.snapdrift.core.AutoBenchPlan;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Unattended benchmarking, enabled by JVM system properties:
 * <ul>
 *   <li>{@code snapdrift.autobench.world}: save folder name to open from the title screen</li>
 *   <li>{@code snapdrift.autobench.plan}: steps, see {@link AutoBenchPlan}</li>
 *   <li>{@code snapdrift.autobench.seconds} (default 60), {@code snapdrift.autobench.settle} (default 30):
 *       flight length, and seconds to wait after joining or changing render distance</li>
 * </ul>
 * The game quits when the plan is done. Pause-on-focus-loss and the AFK frame limiter are turned off for the
 * session, since nobody is at the keyboard.
 */
public final class AutoBench {
	private enum Phase { WAIT_TITLE, JOINING, SETTLING, RUNNING, DONE }

	private static Deque<AutoBenchPlan.Step> steps;
	private static String world;
	private static int seconds;
	private static int settleTicks;
	private static Phase phase;
	private static int timer;

	private AutoBench() {}

	public static void init() {
		world = System.getProperty("snapdrift.autobench.world");
		if (world == null || world.isBlank()) return;
		steps = new ArrayDeque<>(AutoBenchPlan.parse(System.getProperty("snapdrift.autobench.plan", "runx3")));
		seconds = Integer.getInteger("snapdrift.autobench.seconds", 60);
		settleTicks = 20 * Integer.getInteger("snapdrift.autobench.settle", 30);
		phase = Phase.WAIT_TITLE;
		Snapdrift.LOGGER.info("[autobench] world '{}', {} steps", world, steps.size());
	}

	public static void onTick(Minecraft mc) {
		if (phase == null || phase == Phase.DONE) return;
		switch (phase) {
			case WAIT_TITLE -> {
				if (mc.gui.screen() instanceof TitleScreen && ++timer > 40) {
					mc.options.pauseOnLostFocus = false;
					mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
					Snapdrift.LOGGER.info("[autobench] opening '{}'", world);
					phase = Phase.JOINING;
					mc.createWorldOpenFlows().openWorld(world, () -> fail(mc, "could not open world '" + world + "'"));
				}
			}
			case JOINING -> {
				if (mc.player != null && mc.level != null) {
					nextStep(mc);
				}
			}
			case SETTLING -> {
				if (mc.gui.screen() != null) mc.gui.setScreen(null); // pause menus would stop the integrated server
				if (--timer <= 0) {
					AutoBenchPlan.Step step = steps.peek();
					try {
						BenchController.start(mc, step.name(), seconds)
							.forEach(line -> Snapdrift.LOGGER.info("[autobench] {}", line.getString()));
						phase = Phase.RUNNING;
					} catch (IOException e) {
						fail(mc, "could not start benchmark: " + e.getMessage());
					}
				}
			}
			case RUNNING -> {
				if (mc.gui.screen() != null) mc.gui.setScreen(null);
				if (!BenchController.isRunning()) {
					steps.poll();
					nextStep(mc);
				}
			}
			default -> {}
		}
	}

	private static void nextStep(Minecraft mc) {
		AutoBenchPlan.Step step = steps.peek();
		if (step == null) {
			Snapdrift.LOGGER.info("[autobench] plan finished, quitting");
			phase = Phase.DONE;
			mc.stop();
			return;
		}
		// Changing render distance mid-session does not fully apply (measured: RD 12 -> 32 at runtime gave
		// ~220 fps, a fresh launch at 32 gives ~41). So "@rd" is only an assertion: set it in options.txt.
		if (step.renderDistance() > 0 && mc.options.renderDistance().get() != step.renderDistance()) {
			fail(mc, "step '" + step.name() + "' expects render distance " + step.renderDistance() + " but the game is at "
				+ mc.options.renderDistance().get() + ": set renderDistance in options.txt and relaunch");
			return;
		}
		timer = settleTicks;
		phase = Phase.SETTLING;
	}

	private static void fail(Minecraft mc, String why) {
		Snapdrift.LOGGER.error("[autobench] {}", why);
		phase = Phase.DONE;
		mc.stop();
	}
}
