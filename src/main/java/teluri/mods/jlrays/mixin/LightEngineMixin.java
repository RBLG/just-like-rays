package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.misc.IHasEmitProperties;

/**
 * replace static function
 * 
 * @author RBLG
 * @since v0.2.0
 */
@Mixin(LightEngine.class)
public class LightEngineMixin {

	@ModifyReturnValue( //
			method = "hasDifferentLightProperties(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z", //
			at = @At("RETURN")//
	)
	private static boolean hasDifferentLightProperties(boolean original, BlockState state1, BlockState state2) {
		if (state2 == state1) {
			return original;
		}
		return original || hasEmitProperties(state1) || hasEmitProperties(state2);
	}

	private static boolean hasEmitProperties(BlockState state) {
		return ((IHasEmitProperties) state).hasEmitProperties();
	}
}
