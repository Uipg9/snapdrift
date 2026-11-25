package dev.brian.snapdrift.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unattended benchmark plan, e.g. {@code "warmup20@20,rd20@20x3"}: comma-separated
 * {@code name[@renderDistance][xRepeats]} steps, expanded in order. Render distance -1 = leave as is.
 */
public final class AutoBenchPlan {
	public record Step(String name, int renderDistance) {}

	private static final Pattern STEP = Pattern.compile("([A-Za-z0-9_-]+?)(?:@(\\d+))?(?:x(\\d+))?");

	private AutoBenchPlan() {}

	public static List<Step> parse(String plan) {
		List<Step> steps = new ArrayList<>();
		if (plan == null || plan.isBlank()) return steps;
		for (String raw : plan.split(",")) {
			String token = raw.trim();
			Matcher m = STEP.matcher(token);
			if (!m.matches()) throw new IllegalArgumentException("Bad autobench step: '" + token + "'");
			int rd = m.group(2) == null ? -1 : Integer.parseInt(m.group(2));
			if (rd != -1 && (rd < 2 || rd > 32)) {
				throw new IllegalArgumentException("Render distance must be 2-32 in step '" + token + "'");
			}
			int repeats = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
			if (repeats < 1) throw new IllegalArgumentException("Repeats must be at least 1 in step '" + token + "'");
			for (int i = 0; i < repeats; i++) steps.add(new Step(m.group(1), rd));
		}
		return steps;
	}
}
