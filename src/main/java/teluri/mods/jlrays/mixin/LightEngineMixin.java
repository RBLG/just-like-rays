package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements LayerLightEventListener {

	/*@WrapOperation(method = {
			"hasDifferentLightProperties(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z" }, //
			at = @At(value = "INVOKE", target = "net/minecraft/world/level/block/state/BlockState.getLightEmission()I"))
	private static int getLightEmissionForLightEngine(BlockState instance, Operation<Integer> original) {
		return (instance.getBlock() == Blocks.SEA_LANTERN) ? 0 : original.call(instance);
	}*/

}
