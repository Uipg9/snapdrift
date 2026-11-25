# Snapdrift: working notes for Claude

- Minecraft 26.3 Fabric mod, client-only. Java 25. Unobfuscated: use Mojang names, never add mappings/yarn.
- TDD: write a failing JUnit test in `src/test` first, then implement. Pure logic goes in `src/main/.../core`.
  In-game checks go in `src/client/.../SnapdriftClientGameTest.java`.
- Shell is Windows PowerShell: use `.\gradlew.bat`. Filter Gradle output to errors only.
- Verify with `.\scripts\verify.ps1` (build + unit tests + client gametest). Never kill the client; it exits by itself.
- 26.3 renderer API: `RenderSystem.getDevice().getDeviceInfo()` gives `name()` / `backendName()`.
  On a Surface Laptop 7 the device is "D3D12 (Qualcomm(R) Adreno(TM) X1-85 GPU)" (GL-on-D3D12 translation).
- Mod Menu is dev-only (`localRuntime`); do not make it a hard dependency.
