package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import teluri.mods.jlrays.misc.ICoolerBlockGetter;

@Mixin(ImposterProtoChunk.class)
public abstract class ImposterProtoChunkMixin extends ProtoChunkMixin {

	@Shadow
	private final LevelChunk wrapped;

	public BlockState getBlockState(int x, int y, int z) {
		return ((ICoolerBlockGetter) this.wrapped).getBlockState(x, y, z);
	}

	/**
	 * fake constructor
	 */
	protected ImposterProtoChunkMixin() {
		super();
		wrapped = null;
	}
}
