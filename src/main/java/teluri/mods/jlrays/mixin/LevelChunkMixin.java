package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.misc.ICoolerBlockGetter;
import teluri.mods.jlrays.misc.ShinyBlockPos;

/**
 * @author RBLG
 * @since v0.0.1
 */
@Debug
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess implements ICoolerBlockGetter {

	@Shadow
	final Level level;

	/**
	 * hide a ShinyBlockPos in the chain of checkBlock calls to be caught by the custom light engine
	 */
	@WrapOperation(method = "setBlockState*", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/world/level/lighting/LevelLightEngine.checkBlock(Lnet/minecraft/core/BlockPos;)V"))
	public void HijackRemoveCheckNode(LevelLightEngine instance, BlockPos pos, Operation<Void> original, @Local(ordinal = 1) BlockState blockState,
			@Local(ordinal = 0) BlockState state) {
		original.call(instance, new ShinyBlockPos(pos, blockState, state));
	}

	public BlockState getBlockState(int x, int y, int z) {
		if (this.level.isDebug()) {
			BlockState blockState = null;
			if (y == 60) {
				blockState = Blocks.BARRIER.defaultBlockState();
			}
			if (y == 70) {
				blockState = DebugLevelSource.getBlockStateFor(x, z);
			}
			return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
		} else {
			try {
				int l = this.getSectionIndex(y);
				if (l >= 0 && l < this.sections.length) {
					LevelChunkSection levelChunkSection = this.sections[l];
					if (!levelChunkSection.hasOnlyAir()) {
						return levelChunkSection.getBlockState(x & 15, y & 15, z & 15);
					}
				}

				return Blocks.AIR.defaultBlockState();
			} catch (Throwable var8) {
				CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
				CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
				crashReportCategory.setDetail("Location", (CrashReportDetail<String>) (() -> CrashReportCategory.formatLocation(this, x, y, z)));
				throw new ReportedException(crashReport);
			}
		}
	}

	/////////////////////////////////////
	/**
	 * fake constructor to satisfy java compiler
	 */
	public LevelChunkMixin() {
		super(null, null, null, null, 0, null, null);
		level = null;
	}

}
