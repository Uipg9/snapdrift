package dev.brian.snapdrift.core;

/** Straight, level, constant-speed flight along the start yaw. Same path every run. */
public final class BenchPath {
	private final BenchStart start;
	private final double dx;
	private final double dz;

	public BenchPath(BenchStart start, double blocksPerSecond) {
		this.start = start;
		double rad = Math.toRadians(start.yaw());
		// Minecraft: yaw 0 faces +Z (south), yaw 90 faces -X (west).
		this.dx = -Math.sin(rad) * blocksPerSecond;
		this.dz = Math.cos(rad) * blocksPerSecond;
	}

	public BenchStart start() {
		return start;
	}

	public double[] positionAt(double seconds) {
		return new double[] { start.x() + dx * seconds, start.y(), start.z() + dz * seconds };
	}
}
