package teluri.mods.jlrays.mixin;

import java.util.ArrayDeque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.light.starlight.JlrBlockEngineStarlightAdapter;

/**
 * @author RBLG
 * @since v0.0.6/v0.0.7
 */
@Mixin(StarLightInterface.class)
public class StarLightInterfaceMixin {
	@Shadow
	protected ArrayDeque<BlockStarLightEngine> cachedBlockPropagators;

	JlrBlockEngineStarlightAdapter engine;

	@Inject(method = "blockChange*", at = { @At(value = "HEAD") })
	public void blockChange(final BlockPos blockpos, CallbackInfoReturnable<StarLightInterface.LightQueue> info) {
		this.engine.checkBlock(blockpos);
	}

	@Inject(method = "propagateChanges()V", at = { @At(value = "HEAD") })
	public void propagateChanges() {
		this.engine.runLightUpdates();
	}

	@Inject(method = "<init>*", at = { @At(value = "RETURN") })
	public void init(LightChunkGetter lightAccess, boolean hasSkyLight, boolean hasBlockLight,
			LevelLightEngine lightEngine, CallbackInfo info) {

		this.cachedBlockPropagators = null;

	}

}
