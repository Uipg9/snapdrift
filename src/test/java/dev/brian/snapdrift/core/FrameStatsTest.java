package dev.brian.snapdrift.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FrameStatsTest {
	private static final long MS = 1_000_000L;

	@Test
	void steadyFramesGiveMatchingAverageAndLows() {
		FrameStats stats = new FrameStats();
		for (int i = 0; i < 600; i++) stats.add(10 * MS);
		assertEquals(600, stats.count());
		assertEquals(100.0, stats.avgFps(), 1e-9);
		assertEquals(100.0, stats.onePercentLowFps(), 1e-9);
		assertEquals(10.0, stats.p99FrameMs(), 1e-9);
		assertEquals(10.0, stats.maxFrameMs(), 1e-9);
	}

	@Test
	void onePercentLowIsAverageOfSlowestOnePercentOfFrames() {
		FrameStats stats = new FrameStats();
		for (int i = 0; i < 990; i++) stats.add(10 * MS); // 100 fps
		for (int i = 0; i < 10; i++) stats.add(50 * MS); // 20 fps stutters
		// total time = 9.9s + 0.5s = 10.4s over 1000 frames
		assertEquals(1000 / 10.4, stats.avgFps(), 1e-9);
		assertEquals(20.0, stats.onePercentLowFps(), 1e-9);
		assertEquals(50.0, stats.maxFrameMs(), 1e-9);
	}

	@Test
	void smallSamplesUseAtLeastOneFrameForLows() {
		FrameStats stats = new FrameStats();
		stats.add(10 * MS);
		stats.add(40 * MS);
		assertEquals(25.0, stats.onePercentLowFps(), 1e-9);
		assertEquals(40.0, stats.p99FrameMs(), 1e-9);
	}

	@Test
	void growsPastInitialCapacity() {
		FrameStats stats = new FrameStats();
		for (int i = 0; i < 100_000; i++) stats.add(5 * MS);
		assertEquals(100_000, stats.count());
		assertEquals(200.0, stats.avgFps(), 1e-9);
	}

	@Test
	void emptyStatsReportZero() {
		FrameStats stats = new FrameStats();
		assertEquals(0, stats.count());
		assertEquals(0.0, stats.avgFps());
		assertEquals(0.0, stats.onePercentLowFps());
		assertEquals(0.0, stats.p99FrameMs());
	}

	@Test
	void rejectsNonPositiveFrameTimes() {
		FrameStats stats = new FrameStats();
		assertThrows(IllegalArgumentException.class, () -> stats.add(0));
		assertThrows(IllegalArgumentException.class, () -> stats.add(-5));
	}
}
