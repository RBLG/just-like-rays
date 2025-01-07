package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;

@Debug
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
	// @Shadow
	// final Level level;

	@WrapOperation(method = "setBlockState*", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/world/level/lighting/LevelLightEngine.checkBlock(Lnet/minecraft/core/BlockPos;)V"))
	public void HijackRemoveCheckNode(LevelLightEngine instance, BlockPos pos, Operation<Void> original, @Local(ordinal = 1) BlockState blockState, @Local(ordinal = 0) BlockState state) {
		original.call(instance, new ShinyBlockPos(pos, blockState, state));
	}

	// @WrapOperation(method = "setBlockState*", //
	// at = @At(value = "INVOKE", //
	// target =
	// "net/minecraft/world/level/lighting/LightEngine.hasDifferentLightProperties(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	// public boolean HijackReAddCheckNode(BlockState state1, BlockState state2, Operation<Boolean> original) {
	// boolean rtn = original.call(state1, state2);
	// if (rtn) {
	// this.level.getChunkSource().getLightEngine().checkBlock(new ShinyBlockPos(pos, state1, state2));
	// }
	// return rtn;
	// }

	//////////////////////////////////////////////////////////////////////////
	public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime,
			LevelChunkSection[] sections, BlendingData blendingData, Level nlevel) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
		// level = nlevel;
	}

}
