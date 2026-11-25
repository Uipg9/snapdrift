package dev.brian.snapdrift.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Spots setups that silently cost FPS on Windows-on-ARM laptops. Pure logic: inputs are plain strings. */
public final class EnvironmentCheck {
	static final long MIN_HEAP_MB = 3 * 1024;
	static final long MAX_HEAP_MB = 12 * 1024;
	/** Vanilla's "Unlimited" framerate setting. */
	public static final int UNLIMITED_FPS = 260;

	private EnvironmentCheck() {}

	/**
	 * @param javaArch      {@code os.arch} of the running JVM
	 * @param cpuIdentifier {@code PROCESSOR_IDENTIFIER} env var (names the real CPU even under emulation)
	 * @param deviceName    GPU device name reported by the renderer
	 * @param backendName   renderer backend (OpenGL / Vulkan)
	 * @param maxHeapBytes  {@code Runtime.maxMemory()}
	 */
	public static List<String> check(String javaArch, String cpuIdentifier, String deviceName, String backendName,
		long maxHeapBytes) {
		List<String> warnings = new ArrayList<>();
		String arch = lower(javaArch);
		String cpu = lower(cpuIdentifier);
		String device = lower(deviceName);

		boolean armCpu = cpu.contains("arm") || cpu.contains("qualcomm") || cpu.contains("snapdragon");
		boolean x64Java = arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x86");
		if (armCpu && x64Java) {
			warnings.add("Java is x64 running under emulation on an ARM CPU: use a native ARM64 Java 25 (large CPU cost).");
		}

		if (device.contains("basic render driver") || device.contains("llvmpipe") || device.contains("software")) {
			warnings.add("Rendering on a software device (" + deviceName + "): install or update the GPU driver.");
		} else if (device.startsWith("d3d12")) {
			warnings.add("OpenGL runs through Microsoft's GL-on-D3D12 translation (" + deviceName
				+ "). Keep the Adreno driver and the OpenCL/OpenGL Compatibility Pack updated.");
		}

		long heapMb = maxHeapBytes / (1024 * 1024);
		if (heapMb < MIN_HEAP_MB) {
			warnings.add("Only " + heapMb + " MB heap: set -Xmx4G to -Xmx6G in the launcher JVM arguments.");
		} else if (heapMb > MAX_HEAP_MB) {
			warnings.add(heapMb + " MB heap steals unified memory from the GPU: use -Xmx4G to -Xmx6G.");
		}
		return warnings;
	}

	/** Settings that make a benchmark measure something other than raw rendering speed. */
	public static List<String> benchWarnings(boolean vsync, int fpsLimit, boolean dynamicFpsLoaded) {
		List<String> warnings = new ArrayList<>();
		if (vsync) warnings.add("VSync is on: results are capped at the display refresh rate.");
		if (fpsLimit < UNLIMITED_FPS) warnings.add("Max framerate limit is " + fpsLimit + ": set it to Unlimited.");
		if (dynamicFpsLoaded) warnings.add("Dynamic FPS is loaded: keep the window focused or remove it for benchmarks.");
		return warnings;
	}

	/** Stable id of a warning set, used to show the same warnings in chat only once. */
	public static String fingerprint(List<String> warnings) {
		return Integer.toHexString(String.join("\n", warnings).hashCode());
	}

	private static String lower(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT);
	}
}
