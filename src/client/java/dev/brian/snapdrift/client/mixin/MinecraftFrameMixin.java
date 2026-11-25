package dev.brian.snapdrift.client.mixin;

import dev.brian.snapdrift.client.BenchController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Frame clock for benchmarks: one call per rendered frame. Does nothing unless a benchmark is running. */
@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void snapdrift$onFrame(boolean advanceGameTime, CallbackInfo ci) {
		BenchController.onFrame();
	}
}
