package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.misc.ShinyBlockPos;

/**
 * @author RBLG
 * @since v0.0.1
 */
@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin extends ChunkAccess {

	/**
	 * hide a ShinyBlockPos in the chain of checkBlock calls to be caught by the custom light engine
	 */
	@WrapOperation(method = "setBlockState*", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/world/level/lighting/LevelLightEngine.checkBlock(Lnet/minecraft/core/BlockPos;)V"))
	public void hijackRemoveCheckNode(LevelLightEngine instance, BlockPos pos, Operation<Void> original, @Local(ordinal = 1) BlockState blockState,
			@Local(ordinal = 0) BlockState state) {
		original.call(instance, new ShinyBlockPos(pos, blockState, state)); // removed and replaced in the other mixin
	}

	/////////////////////////////////////
	/**
	 * fake constructor to satisfy java compiler
	 */
	protected ProtoChunkMixin() {
		super(null, null, null, null, 0, null, null);
	}
}
