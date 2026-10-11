package de.felix.randomizer;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class FelixRandomizerPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
	private static final int DEFAULT_INTERVAL_SECONDS = 60;
	private static final int MAX_INTERVAL_SECONDS = 86_400;
	private static final double OTHER_ITEM_CHANCE = 0.25;
	private static final double ENCHANTMENT_CHANCE = 0.60;

	private List<Material> preferredItems;
	private List<Material> otherItems;
	private boolean enabled;
	private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;
	private int ticksRemaining = secondsToTicks(DEFAULT_INTERVAL_SECONDS);

	@Override
	public void onEnable() {
		buildItemPools();

		PluginCommand command = Objects.requireNonNull(
			getCommand("randomitems"),
			"Command randomitems is missing from plugin.yml"
		);
		command.setExecutor(this);
		command.setTabCompleter(this);

		getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
		getLogger().info(
			"Loaded with " + preferredItems.size() + " preferred and " + otherItems.size() + " other items; disabled by default."
		);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			return showStatus(sender);
		}

		return switch (args[0].toLowerCase(Locale.ROOT)) {
			case "start" -> start(sender, args);
			case "stop" -> stop(sender, args);
			case "interval" -> setInterval(sender, args);
			case "status" -> showStatus(sender);
			default -> {
				sender.sendMessage("Verwendung: /randomitems <start|stop|interval|status>");
				yield true;
			}
		};
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			String prefix = args[0].toLowerCase(Locale.ROOT);
			return List.of("start", "stop", "interval", "status").stream()
				.filter(value -> value.startsWith(prefix))
				.toList();
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("interval")) {
			return List.of("60");
		}
		return List.of();
	}

	private boolean start(CommandSender sender, String[] args) {
		if (args.length != 1) {
			return invalidUsage(sender);
		}
		enabled = true;
		ticksRemaining = secondsToTicks(intervalSeconds);
		sender.sendMessage("Randomizer gestartet. Intervall: " + duration(intervalSeconds) + ".");
		return true;
	}

	private boolean stop(CommandSender sender, String[] args) {
		if (args.length != 1) {
			return invalidUsage(sender);
		}
		enabled = false;
		sender.sendMessage("Randomizer gestoppt.");
		return true;
	}

	private boolean setInterval(CommandSender sender, String[] args) {
		if (args.length != 2) {
			sender.sendMessage("Verwendung: /randomitems interval <Sekunden>");
			return true;
		}

		int seconds;
		try {
			seconds = Integer.parseInt(args[1]);
		} catch (NumberFormatException exception) {
			sender.sendMessage("Das Intervall muss eine ganze Zahl sein.");
			return true;
		}

		if (seconds < 1 || seconds > MAX_INTERVAL_SECONDS) {
			sender.sendMessage("Das Intervall muss zwischen 1 und 86.400 Sekunden liegen.");
			return true;
		}

		intervalSeconds = seconds;
		ticksRemaining = secondsToTicks(seconds);
		sender.sendMessage("Randomizer-Intervall: " + duration(seconds) + ".");
		return true;
	}

	private boolean showStatus(CommandSender sender) {
		String status = enabled ? "aktiv" : "inaktiv";
		String next = enabled ? ", nächste Vergabe in " + duration(secondsRemaining()) : "";
		sender.sendMessage(
			"Randomizer ist " + status + ". Intervall: " + duration(intervalSeconds) + next + "."
		);
		return true;
	}

	private boolean invalidUsage(CommandSender sender) {
		sender.sendMessage("Verwendung: /randomitems <start|stop|interval|status>");
		return true;
	}

	private void tick() {
		if (!enabled) {
			return;
		}

		ticksRemaining--;
		if (ticksRemaining > 0) {
			return;
		}

		ticksRemaining = secondsToTicks(intervalSeconds);
		for (Player player : getServer().getOnlinePlayers()) {
			giveRandomItem(player);
		}
	}

	private void giveRandomItem(Player player) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		boolean preferred = random.nextDouble() >= OTHER_ITEM_CHANCE;
		List<Material> pool = preferred ? preferredItems : otherItems;
		ItemStack stack = new ItemStack(pool.get(random.nextInt(pool.size())));

		if (preferred && random.nextDouble() < ENCHANTMENT_CHANCE) {
			enchant(stack, random);
		}

		ItemStack notificationStack = stack.clone();
		Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
		for (ItemStack leftover : leftovers.values()) {
			player.getWorld().dropItemNaturally(player.getLocation(), leftover);
		}

		player.sendMessage(
			Component.text("[Randomizer] Du erhältst: ").append(notificationStack.displayName())
		);
	}

	private void enchant(ItemStack stack, ThreadLocalRandom random) {
		List<Enchantment> compatible = new ArrayList<>(
			RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).stream()
				.filter(enchantment -> !enchantment.isCursed())
				.filter(enchantment -> enchantment.canEnchantItem(stack))
				.toList()
		);
		Collections.shuffle(compatible, random);

		List<Enchantment> selected = new ArrayList<>();
		int targetCount = random.nextInt(1, 4);
		for (Enchantment enchantment : compatible) {
			boolean conflicts = selected.stream().anyMatch(existing ->
				existing.conflictsWith(enchantment) || enchantment.conflictsWith(existing)
			);
			if (conflicts) {
				continue;
			}

			int level = random.nextInt(enchantment.getStartLevel(), enchantment.getMaxLevel() + 1);
			stack.addEnchantment(enchantment, level);
			selected.add(enchantment);
			if (selected.size() >= targetCount) {
				break;
			}
		}
	}

	private void buildItemPools() {
		preferredItems = Arrays.stream(Material.values())
			.filter(FelixRandomizerPlugin::isUsableItem)
			.filter(FelixRandomizerPlugin::isPreferred)
			.toList();
		otherItems = Arrays.stream(Material.values())
			.filter(FelixRandomizerPlugin::isUsableItem)
			.filter(material -> !isPreferred(material))
			.toList();

		if (preferredItems.isEmpty() || otherItems.isEmpty()) {
			throw new IllegalStateException("FelixRandomizer could not build its item pools");
		}
	}

	private static boolean isUsableItem(Material material) {
		return material.isItem() && !material.isAir() && !material.isLegacy();
	}

	private static boolean isPreferred(Material material) {
		String name = material.name();
		return name.endsWith("_SWORD")
			|| name.endsWith("_AXE")
			|| name.endsWith("_HELMET")
			|| name.endsWith("_CHESTPLATE")
			|| name.endsWith("_LEGGINGS")
			|| name.endsWith("_BOOTS");
	}

	private int secondsRemaining() {
		return Math.max(0, (ticksRemaining + 19) / 20);
	}

	private static int secondsToTicks(int seconds) {
		return seconds * 20;
	}

	private static String duration(int seconds) {
		return seconds + (seconds == 1 ? " Sekunde" : " Sekunden");
	}
}
