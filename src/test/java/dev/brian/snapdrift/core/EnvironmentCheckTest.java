package dev.brian.snapdrift.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnvironmentCheckTest {
	private static final long GB = 1024L * 1024 * 1024;
	private static final String SNAPDRAGON = "ARMv8 (64-bit) Family 8 Model 1 Revision 201, Qualcomm Technologies Inc";

	@Test
	void healthyNativeSetupOnRealDriverHasNoWarnings() {
		List<String> w = EnvironmentCheck.check("aarch64", SNAPDRAGON, "Adreno X1-85", "Vulkan", 6 * GB);
		assertEquals(List.of(), w);
	}

	@Test
	void x64JavaOnArmCpuIsEmulated() {
		List<String> w = EnvironmentCheck.check("amd64", SNAPDRAGON, "Adreno X1-85", "Vulkan", 6 * GB);
		assertTrue(w.stream().anyMatch(s -> s.contains("emulat")), w.toString());
	}

	@Test
	void x64JavaOnIntelIsFine() {
		List<String> w = EnvironmentCheck.check("amd64", "Intel64 Family 6 Model 154", "Intel Iris Xe", "OpenGL", 6 * GB);
		assertEquals(List.of(), w);
	}

	@Test
	void glOn12TranslationIsFlagged() {
		List<String> w = EnvironmentCheck.check("aarch64", SNAPDRAGON,
			"D3D12 (Qualcomm(R) Adreno(TM) X1-85 GPU)", "OpenGL", 6 * GB);
		assertEquals(1, w.size(), w.toString());
		assertTrue(w.get(0).contains("D3D12"), w.toString());
	}

	@Test
	void softwareRendererIsFlagged() {
		List<String> w = EnvironmentCheck.check("aarch64", SNAPDRAGON,
			"D3D12 (Microsoft Basic Render Driver)", "OpenGL", 6 * GB);
		assertTrue(w.stream().anyMatch(s -> s.contains("software")), w.toString());
	}

	@Test
	void heapOutsideRecommendedRangeIsFlagged() {
		assertTrue(EnvironmentCheck.check("aarch64", SNAPDRAGON, "Adreno", "Vulkan", 2 * GB)
			.stream().anyMatch(s -> s.contains("-Xmx")));
		assertTrue(EnvironmentCheck.check("aarch64", SNAPDRAGON, "Adreno", "Vulkan", 16 * GB)
			.stream().anyMatch(s -> s.contains("-Xmx")));
	}

	@Test
	void fingerprintChangesOnlyWhenWarningsChange() {
		List<String> a = List.of("one", "two");
		assertEquals(EnvironmentCheck.fingerprint(a), EnvironmentCheck.fingerprint(List.of("one", "two")));
		assertNotEquals(EnvironmentCheck.fingerprint(a), EnvironmentCheck.fingerprint(List.of("one")));
	}

	@Test
	void benchWarningsFlagUncontrolledSettings() {
		assertEquals(List.of(), EnvironmentCheck.benchWarnings(false, 260, false));
		assertTrue(EnvironmentCheck.benchWarnings(true, 260, false).stream().anyMatch(s -> s.contains("VSync")));
		assertTrue(EnvironmentCheck.benchWarnings(false, 120, false).stream().anyMatch(s -> s.contains("limit")));
		assertTrue(EnvironmentCheck.benchWarnings(false, 260, true).stream().anyMatch(s -> s.contains("Dynamic FPS")));
	}
}
