package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.misc.DullBlockPos;
import teluri.mods.jlrays.misc.ShinyBlockPos;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine implements AutoCloseable {

	@WrapOperation(method = "checkBlock*", //
			at = @At(value = "INVOKE", //
					target = "net/minecraft/core/BlockPos.immutable()Lnet.minecraft.core.BlockPos;"))
	public BlockPos assertCheckBlock(BlockPos pos, Operation<BlockPos> original) {
		if (pos instanceof ShinyBlockPos || pos instanceof DullBlockPos) {
			return original.call(pos);
		}
		return new DullBlockPos(pos);
	}

	//////////////////////////////////////////

	public ThreadedLevelLightEngineMixin() {
		super(null, false, false);
	}
}
