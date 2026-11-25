# Snapdrift

Client-side Fabric mod for **Minecraft 26.3** that makes performance tuning measurable: a repeatable
benchmark flight that saves results to CSV, and a check for setups that quietly cost FPS on
Windows-on-ARM laptops (Snapdragon X / Adreno).

It is the measuring tool behind the Snapdrift performance pack.

## In game

| Command | What it does |
|---|---|
| `/snapdrift setstart` | Saves your current position and view direction as the route start |
| `/snapdrift bench <name> [seconds]` | Switches to spectator, parks at the start for 3 s, then flies straight at 10 blocks/s for `seconds` (default 60) while recording every frame time. Restores your game mode at the end |
| `/snapdrift env` | Lists environment problems (emulated x64 Java, GL-on-D3D12 translation, software renderer, heap size) |

Results are appended to `<game dir>\snapdrift\bench\results.csv`, one row per run: average FPS, 1% low,
p99 and max frame time, render and simulation distance, VSync, FPS limit, GPU device, backend, Java arch,
heap size, loaded performance mods and the route start. Every row says which setup produced it.

### Unattended runs (autobench)
Start the game with these JVM properties and it opens the world from the title screen, runs every step, and quits:

| Property | Meaning |
|---|---|
| `snapdrift.autobench.world` | save folder to open (enables autobench) |
| `snapdrift.autobench.plan` | steps `name[@renderDistance][xRepeats]`, comma-separated, e.g. `warmup@20,rd20@20x3`. `@renderDistance` only checks the current value: changing render distance mid-session does not fully apply, so set it in options.txt before launching |
| `snapdrift.autobench.seconds` | flight length per run (default 60) |
| `snapdrift.autobench.settle` | seconds to wait after joining or changing render distance (default 30) |

Pause-on-focus-loss and the AFK frame limiter are switched off for the session, so the run needs nobody at the keyboard.

Environment warnings are always written to the log. They appear in chat once, and again only when they change.

### Benchmark controls
For numbers you can compare: plug in, use the Best Performance power mode, turn VSync off and set the framerate limit to
Unlimited (the bench warns about both), keep the window focused, wait for LOD mods to finish building, do 3 runs per
setup and use the median.

## Development (PowerShell)

| Task | Command |
|---|---|
| Unit tests | `.\gradlew.bat test` |
| Everything (build, tests, client gametest) | `.\scripts\verify.ps1` |

- Pure logic (`FrameStats`, `BenchPath`, `BenchStart`, `BenchReport`, `EnvironmentCheck`) lives in `src/main/.../core` and is unit-tested.
- The client gametest joins a world, runs a real 5-second benchmark and checks the camera moved and the CSV row was written.
- The frame clock is a single `@Inject` at the head of `Minecraft.renderFrame`. It does nothing unless a benchmark is running.

## License

CC0-1.0
