package teluri.mods.jlrays.mixin;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin extends LightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {

	/*@WrapOperation(method = { //
			"getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I", //
			"propagateLightSources(Lnet/minecraft/world/level/ChunkPos;)V" }, //
			at = @At(value = "INVOKE", target = "net/minecraft/world/level/block/state/BlockState.getLightEmission()I"))
	private int getLightEmissionForBlockLightEngine(BlockState instance, Operation<Integer> original) {
		return (instance.getBlock() == Blocks.SEA_LANTERN) ? 0 : original.call(instance);
	}*/

	//////////////////////////////////////////// yeet /////////////

	protected BlockLightEngineMixin(LightChunkGetter lightChunkGetter, BlockLightSectionStorage layerLightSectionStorage) {
		super(lightChunkGetter, layerLightSectionStorage);
	}
}