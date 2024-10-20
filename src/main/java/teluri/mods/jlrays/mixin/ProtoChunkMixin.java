package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin extends ChunkAccess {
	@Shadow
	private volatile LevelLightEngine lightEngine;

	@WrapOperation(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/world/level/lighting/LevelLightEngine.checkBlock(Lnet/minecraft/core/BlockPos;)V"))
	public void HijackRemoveCheckNode(LevelLightEngine instance, BlockPos pos, Operation<Void> original) {
		// original.call(instance, new ShinyBlockPos(pos, null, null)); //removed and replaced in the other mixin
	}

	@WrapOperation(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/world/level/lighting/LightEngine.hasDifferentLightProperties(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	public boolean HijackReAddCheckNode(BlockGetter level, BlockPos pos, BlockState state1, BlockState state2, Operation<Boolean> original) {
		boolean rtn = original.call(level, pos, state1, state2);
		if (rtn) {
			this.lightEngine.checkBlock(new ShinyBlockPos(pos, state1, state2));
		}
		return rtn;
	}
	

	public ProtoChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime,
			LevelChunkSection[] sections, BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}
}
