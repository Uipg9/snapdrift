package dev.brian.snapdrift.core;

import java.util.Locale;

/** Fixed start point and view direction of the benchmark flight. Yaw/pitch use Minecraft degrees. */
public record BenchStart(double x, double y, double z, float yaw, float pitch) {
	public String serialize() {
		return String.format(Locale.ROOT, "%s %s %s %s %s", x, y, z, yaw, pitch);
	}

	/** Returns null when the text is not a serialized start. */
	public static BenchStart parse(String text) {
		if (text == null) return null;
		String[] p = text.trim().split("\\s+");
		if (p.length != 5) return null;
		try {
			return new BenchStart(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
				Float.parseFloat(p[3]), Float.parseFloat(p[4]));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
