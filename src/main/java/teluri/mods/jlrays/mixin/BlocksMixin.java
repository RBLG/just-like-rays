package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Blocks;

@Mixin(Blocks.class)
public class BlocksMixin {

	@Inject(method = "<clinit>*", at = @At(value = "HEAD"))
	private static void staticBlockMixin(CallbackInfo info) {
		FabricLoader.getInstance().invokeEntrypoints("jlr-settings", ModInitializer.class, (entry) -> entry.onInitialize());
	}
}
