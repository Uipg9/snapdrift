package dev.brian.snapdrift.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BenchPathTest {
	@Test
	void yawZeroFliesTowardPositiveZ() {
		// Minecraft yaw 0 = south = +Z
		BenchPath path = new BenchPath(new BenchStart(0, 100, 0, 0f, 10f), 10.0);
		double[] p = path.positionAt(3.0);
		assertEquals(0.0, p[0], 1e-9);
		assertEquals(100.0, p[1], 1e-9);
		assertEquals(30.0, p[2], 1e-9);
	}

	@Test
	void yawNinetyFliesTowardNegativeX() {
		// Minecraft yaw 90 = west = -X
		BenchPath path = new BenchPath(new BenchStart(5, 80, 5, 90f, 0f), 10.0);
		double[] p = path.positionAt(2.0);
		assertEquals(-15.0, p[0], 1e-9);
		assertEquals(80.0, p[1], 1e-9);
		assertEquals(5.0, p[2], 1e-9);
	}

	@Test
	void timeZeroIsTheStart() {
		BenchPath path = new BenchPath(new BenchStart(1.5, 70, -2.5, 33f, 0f), 10.0);
		double[] p = path.positionAt(0.0);
		assertEquals(1.5, p[0], 1e-9);
		assertEquals(70.0, p[1], 1e-9);
		assertEquals(-2.5, p[2], 1e-9);
	}

	@Test
	void startRoundTripsThroughText() {
		BenchStart start = new BenchStart(12.25, 96.0, -340.5, 135.5f, 12.0f);
		assertEquals(start, BenchStart.parse(start.serialize()));
	}

	@Test
	void parseRejectsGarbage() {
		assertNull(BenchStart.parse("not a start"));
		assertNull(BenchStart.parse(""));
		assertNull(BenchStart.parse(null));
	}
}
