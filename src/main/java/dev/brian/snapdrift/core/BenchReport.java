package dev.brian.snapdrift.core;

import java.util.List;
import java.util.Locale;

/** One benchmark run, self-describing so runs from different setups can be compared from the CSV alone. */
public record BenchReport(
	String timestamp,
	String name,
	int seconds,
	FrameStats stats,
	int renderDistance,
	int simulationDistance,
	boolean vsync,
	int fpsLimit,
	String device,
	String backend,
	String javaArch,
	long maxHeapMb,
	List<String> perfMods,
	BenchStart start) {

	public static String csvHeader() {
		return "timestamp,name,seconds,frames,avg_fps,low1_fps,p99_ms,max_ms,render_distance,simulation_distance,"
			+ "vsync,fps_limit,device,backend,java_arch,max_heap_mb,perf_mods,start";
	}

	public String csvRow() {
		return String.join(",",
			csv(timestamp), csv(name), Integer.toString(seconds), Integer.toString(stats.count()),
			num(stats.avgFps()), num(stats.onePercentLowFps()), num(stats.p99FrameMs()), num(stats.maxFrameMs()),
			Integer.toString(renderDistance), Integer.toString(simulationDistance),
			Boolean.toString(vsync), Integer.toString(fpsLimit),
			csv(device), csv(backend), csv(javaArch), Long.toString(maxHeapMb),
			csv(String.join(";", perfMods)), csv(start.serialize()));
	}

	public String summary() {
		return String.format(Locale.ROOT, "%s: avg %.1f fps, 1%% low %.1f fps, p99 %.1f ms (%d frames, RD %d)",
			name, stats.avgFps(), stats.onePercentLowFps(), stats.p99FrameMs(), stats.count(), renderDistance);
	}

	private static String num(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	private static String csv(String s) {
		if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}
