package dev.brian.snapdrift.client;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.brian.snapdrift.core.BenchStart;
import java.io.IOException;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SnapdriftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(BenchController::onTick);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> mc.execute(() -> EnvironmentNotice.onJoin(mc)));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
			literal("snapdrift")
				.then(literal("setstart").executes(SnapdriftClient::setStart))
				.then(literal("env").executes(SnapdriftClient::env))
				.then(literal("bench")
					.then(argument("name", StringArgumentType.word())
						.executes(ctx -> bench(ctx, 60))
						.then(argument("seconds", IntegerArgumentType.integer(5, 600))
							.executes(ctx -> bench(ctx, IntegerArgumentType.getInteger(ctx, "seconds"))))))));
	}

	private static int setStart(CommandContext<FabricClientCommandSource> ctx) {
		try {
			BenchStart start = BenchController.saveStart(ctx.getSource().getPlayer());
			ctx.getSource().sendFeedback(Component.literal("Route start saved: " + start.serialize())
				.withStyle(ChatFormatting.GREEN));
			return 1;
		} catch (IOException e) {
			ctx.getSource().sendError(Component.literal("Could not save route start: " + e.getMessage()));
			return 0;
		}
	}

	private static int env(CommandContext<FabricClientCommandSource> ctx) {
		List<String> warnings = EnvironmentNotice.currentWarnings();
		if (warnings.isEmpty()) {
			ctx.getSource().sendFeedback(Component.literal("No environment problems found.").withStyle(ChatFormatting.GREEN));
		}
		warnings.forEach(w -> ctx.getSource().sendFeedback(Component.literal(" - " + w).withStyle(ChatFormatting.YELLOW)));
		return 1;
	}

	private static int bench(CommandContext<FabricClientCommandSource> ctx, int seconds) {
		String name = StringArgumentType.getString(ctx, "name");
		try {
			BenchController.start(ctx.getSource().getClient(), name, seconds).forEach(ctx.getSource()::sendFeedback);
			return 1;
		} catch (IOException e) {
			ctx.getSource().sendError(Component.literal("Could not start benchmark: " + e.getMessage()));
			return 0;
		}
	}
}
