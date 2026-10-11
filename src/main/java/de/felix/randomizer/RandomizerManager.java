package de.felix.randomizer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RandomizerManager {
	public static final int DEFAULT_INTERVAL_SECONDS = 60;
	public static final int MAX_INTERVAL_SECONDS = 86_400;
	private static final float OTHER_ITEM_CHANCE = 0.25F;
	private static final float ENCHANTMENT_CHANCE = 0.60F;

	private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();
	private static List<Item> preferredItems;
	private static List<Item> otherItems;

	private RandomizerManager() {
	}

	public static State state(MinecraftServer server) {
		return STATES.computeIfAbsent(server, ignored -> new State());
	}

	public static void start(MinecraftServer server) {
		State state = state(server);
		state.enabled = true;
		state.ticksRemaining = secondsToTicks(state.intervalSeconds);
	}

	public static void stop(MinecraftServer server) {
		state(server).enabled = false;
	}

	public static void setInterval(MinecraftServer server, int seconds) {
		State state = state(server);
		state.intervalSeconds = seconds;
		state.ticksRemaining = secondsToTicks(seconds);
	}

	public static void tick(MinecraftServer server) {
		State state = state(server);
		if (!state.enabled) {
			return;
		}

		state.ticksRemaining--;
		if (state.ticksRemaining > 0) {
			return;
		}

		state.ticksRemaining = secondsToTicks(state.intervalSeconds);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			giveRandomItem(server, player);
		}
	}

	private static void giveRandomItem(MinecraftServer server, ServerPlayer player) {
		RandomSource random = server.overworld().getRandom();
		Item item = pickRandomItem(random);
		ItemStack stack = new ItemStack(item);

		if (isPreferred(item) && random.nextFloat() < ENCHANTMENT_CHANCE) {
			int enchantmentLevel = random.nextIntBetweenInclusive(5, 30);
			stack = EnchantmentHelper.enchantItem(
				random,
				stack,
				enchantmentLevel,
				server.registryAccess(),
				java.util.Optional.empty()
			);
		}

		ItemStack notificationStack = stack.copy();
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			player.drop(stack, false);
		}

		player.sendSystemMessage(
			Component.literal("[Randomizer] Du erhältst: ").append(notificationStack.getDisplayName())
		);
	}

	private static Item pickRandomItem(RandomSource random) {
		ensureItemPools();
		List<Item> pool = random.nextFloat() < OTHER_ITEM_CHANCE ? otherItems : preferredItems;
		return pool.get(random.nextInt(pool.size()));
	}

	private static void ensureItemPools() {
		if (preferredItems != null) {
			return;
		}

		preferredItems = BuiltInRegistries.ITEM.stream()
			.filter(RandomizerManager::isPreferred)
			.toList();
		otherItems = BuiltInRegistries.ITEM.stream()
			.filter(item -> item != Items.AIR)
			.filter(item -> !isPreferred(item))
			.toList();

		if (preferredItems.isEmpty() || otherItems.isEmpty()) {
			throw new IllegalStateException("FelixRandomizer could not build its item pools");
		}
	}

	private static boolean isPreferred(Item item) {
		return item.builtInRegistryHolder().is(ItemTags.SWORDS)
			|| item.builtInRegistryHolder().is(ItemTags.AXES)
			|| item.builtInRegistryHolder().is(ItemTags.HEAD_ARMOR)
			|| item.builtInRegistryHolder().is(ItemTags.CHEST_ARMOR)
			|| item.builtInRegistryHolder().is(ItemTags.LEG_ARMOR)
			|| item.builtInRegistryHolder().is(ItemTags.FOOT_ARMOR);
	}

	private static int secondsToTicks(int seconds) {
		return seconds * 20;
	}

	public static final class State {
		private boolean enabled;
		private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;
		private int ticksRemaining = secondsToTicks(DEFAULT_INTERVAL_SECONDS);

		public boolean enabled() {
			return enabled;
		}

		public int intervalSeconds() {
			return intervalSeconds;
		}

		public int secondsRemaining() {
			return Math.max(0, (ticksRemaining + 19) / 20);
		}
	}
}
