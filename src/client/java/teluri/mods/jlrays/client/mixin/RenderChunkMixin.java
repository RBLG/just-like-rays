package teluri.mods.jlrays.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import teluri.mods.jlrays.misc.ICoolerBlockGetter;

@Mixin(targets = "net.minecraft.client.renderer.chunk.RenderChunk")
public class RenderChunkMixin implements ICoolerBlockGetter {

	@Shadow
	private final List<PalettedContainer<BlockState>> sections;
	@Shadow
	private final boolean debug;
	@Shadow
	private final LevelChunk wrapped;

	public BlockState getBlockState(int x,int y,int z) {
		if (this.debug) {
			BlockState blockState = null;
			if (y == 60) {
				blockState = Blocks.BARRIER.defaultBlockState();
			}

			if (y == 70) {
				blockState = DebugLevelSource.getBlockStateFor(x, z);
			}

			return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
		} else if (this.sections == null) {
			return Blocks.AIR.defaultBlockState();
		} else {
			try {
				int l = this.wrapped.getSectionIndex(y);
				if (l >= 0 && l < this.sections.size()) {
					PalettedContainer<BlockState> palettedContainer = (PalettedContainer<BlockState>) this.sections.get(l);
					if (palettedContainer != null) {
						return (BlockState) palettedContainer.get(x & 15, y & 15, z & 15);
					}
				}

				return Blocks.AIR.defaultBlockState();
			} catch (Throwable var8) {
				CrashReport crashReport = CrashReport.forThrowable(var8, "Getting block state");
				CrashReportCategory crashReportCategory = crashReport.addCategory("Block being got");
				crashReportCategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this.wrapped, x, y, z));
				throw new ReportedException(crashReport);
			}
		}
	}

	/**
	 * fake constructor
	 */
	protected RenderChunkMixin() {
		this.sections = null;
		this.debug = false;
		this.wrapped = null;

	}
}
