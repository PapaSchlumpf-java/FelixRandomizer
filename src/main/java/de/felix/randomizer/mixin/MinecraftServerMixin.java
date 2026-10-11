package de.felix.randomizer.mixin;

import de.felix.randomizer.RandomizerManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@Inject(method = "tickServer", at = @At("TAIL"))
	private void felixRandomizer$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		RandomizerManager.tick((MinecraftServer) (Object) this);
	}
}
