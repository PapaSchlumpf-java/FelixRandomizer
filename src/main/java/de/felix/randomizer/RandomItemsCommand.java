package de.felix.randomizer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class RandomItemsCommand {
	private RandomItemsCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("randomitems")
			.then(Commands.literal("start")
				.executes(context -> start(context.getSource())))
			.then(Commands.literal("stop")
				.executes(context -> stop(context.getSource())))
			.then(Commands.literal("interval")
				.then(Commands.argument(
					"seconds",
					IntegerArgumentType.integer(1, RandomizerManager.MAX_INTERVAL_SECONDS)
				).executes(context -> setInterval(
					context.getSource(),
					IntegerArgumentType.getInteger(context, "seconds")
				))))
			.then(Commands.literal("status")
				.executes(context -> status(context.getSource()))));
	}

	private static int start(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		RandomizerManager.start(server);
		int interval = RandomizerManager.state(server).intervalSeconds();
		source.sendSuccess(
			() -> Component.literal("Randomizer gestartet. Intervall: " + duration(interval) + "."),
			true
		);
		return 1;
	}

	private static int stop(CommandSourceStack source) {
		RandomizerManager.stop(source.getServer());
		source.sendSuccess(() -> Component.literal("Randomizer gestoppt."), true);
		return 1;
	}

	private static int setInterval(CommandSourceStack source, int seconds) {
		RandomizerManager.setInterval(source.getServer(), seconds);
		source.sendSuccess(
			() -> Component.literal("Randomizer-Intervall: " + duration(seconds) + "."),
			true
		);
		return 1;
	}

	private static int status(CommandSourceStack source) {
		RandomizerManager.State state = RandomizerManager.state(source.getServer());
		String status = state.enabled() ? "aktiv" : "inaktiv";
		String next = state.enabled() ? ", nächste Vergabe in " + duration(state.secondsRemaining()) : "";
		source.sendSuccess(
			() -> Component.literal(
				"Randomizer ist " + status + ". Intervall: " + duration(state.intervalSeconds()) + next + "."
			),
			false
		);
		return 1;
	}

	private static String duration(int seconds) {
		return seconds + (seconds == 1 ? " Sekunde" : " Sekunden");
	}
}
