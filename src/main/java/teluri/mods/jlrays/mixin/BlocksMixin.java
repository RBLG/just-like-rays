package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Blocks;

/**
 * entrypoint for initializing block config before blocks are initialized
 * 
 * @author RBLG
 * @since v0.2.0
 */
@Mixin(Blocks.class)
public class BlocksMixin {

	@Inject(method = "<clinit>*", at = @At(value = "HEAD"))
	private static void staticBlockMixin(CallbackInfo info) {
		FabricLoader.getInstance().invokeEntrypoints("jlr-config", ModInitializer.class, (entry) -> entry.onInitialize());
	}
}
