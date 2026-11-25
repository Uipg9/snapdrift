package dev.brian.snapdrift.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchReportTest {
	private static BenchReport sample(String name, String device) {
		FrameStats stats = new FrameStats();
		for (int i = 0; i < 99; i++) stats.add(10_000_000L);
		stats.add(40_000_000L);
		return new BenchReport("2026-11-25T12:00:00", name, 60, stats, 12, 6, false, 260,
			device, "OpenGL", "aarch64", 6144, List.of("sodium", "lithium"),
			new BenchStart(0, 100, 0, 0f, 0f));
	}

	@Test
	void rowHasSameColumnCountAsHeader() {
		String header = BenchReport.csvHeader();
		String row = sample("core", "D3D12 (Qualcomm Adreno X1-85)").csvRow();
		assertEquals(header.split(",", -1).length, row.split(",", -1).length);
	}

	@Test
	void fieldsWithCommasOrQuotesAreQuoted() {
		String row = sample("a,b", "GPU \"X\"").csvRow();
		assertTrue(row.contains("\"a,b\""), row);
		assertTrue(row.contains("\"GPU \"\"X\"\"\""), row);
	}

	@Test
	void modsAreSemicolonJoinedAndNumbersUseDotDecimals() {
		String row = sample("core", "gpu").csvRow();
		assertTrue(row.contains(",sodium;lithium,"), row);
		assertTrue(row.contains(",100,"), row); // frame count
		assertTrue(row.matches(".*,\\d+\\.\\d,.*"), row); // dot decimals, never a locale comma
	}

	@Test
	void summaryMentionsFpsAndLows() {
		String summary = sample("core", "gpu").summary();
		assertTrue(summary.contains("core"), summary);
		assertTrue(summary.contains("avg"), summary);
		assertTrue(summary.contains("1% low"), summary);
	}
}
