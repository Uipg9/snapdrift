package dev.brian.snapdrift.core;

import java.util.Arrays;

/**
 * Collects per-frame times (nanoseconds) and derives the numbers a benchmark reports.
 * Lows come from individual frame times, not from averaged FPS samples.
 */
public final class FrameStats {
	private long[] frames = new long[4096];
	private int count;
	private long totalNanos;

	public void add(long frameNanos) {
		if (frameNanos <= 0) {
			throw new IllegalArgumentException("frame time must be positive: " + frameNanos);
		}
		if (count == frames.length) {
			frames = Arrays.copyOf(frames, frames.length * 2);
		}
		frames[count++] = frameNanos;
		totalNanos += frameNanos;
	}

	public int count() {
		return count;
	}

	public double avgFps() {
		return count == 0 ? 0.0 : count * 1e9 / totalNanos;
	}

	/** FPS equivalent of the average of the slowest 1% of frames (at least one frame). */
	public double onePercentLowFps() {
		if (count == 0) return 0.0;
		long[] sorted = sorted();
		int n = Math.max(1, count / 100);
		long sum = 0;
		for (int i = count - n; i < count; i++) sum += sorted[i];
		return n * 1e9 / sum;
	}

	/** 99th percentile frame time in milliseconds (nearest-rank). */
	public double p99FrameMs() {
		if (count == 0) return 0.0;
		long[] sorted = sorted();
		int rank = (int) Math.ceil(0.99 * count) - 1;
		return sorted[Math.max(0, rank)] / 1e6;
	}

	public double maxFrameMs() {
		long max = 0;
		for (int i = 0; i < count; i++) max = Math.max(max, frames[i]);
		return max / 1e6;
	}

	private long[] sorted() {
		long[] copy = Arrays.copyOf(frames, count);
		Arrays.sort(copy);
		return copy;
	}
}
