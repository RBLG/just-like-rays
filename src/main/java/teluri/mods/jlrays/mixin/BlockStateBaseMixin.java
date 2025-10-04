package teluri.mods.jlrays.mixin;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.BlockConfig;
import teluri.mods.jlrays.misc.IHasLightProperties;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * @author RBLG
 * @since v0.0.4
 */
@Mixin(BlockStateBase.class)
public class BlockStateBaseMixin extends StateHolder<Block, BlockState> implements IHasLightProperties {

	@Shadow
	private int lightEmission;
	// @Shadow
	// private int lightBlock;
	@Shadow
	private FluidState fluidState;
	@Shadow
	@Nullable
	protected BlockBehaviour.BlockStateBase.Cache cache;

	/**
	 * modify fields based on description id. will be used for settings
	 * 
	 * @param info
	 */
	@Inject(method = "initCache()V", at = @At("RETURN"))
	public void dataDrivenCacheInit(CallbackInfo info) {

		BlockConfig config = BlockConfig.LazyGet();
		config.notifyInitCache();
		ArrayList<Consumer<BlockStateBase>> bsmods = config.blockstates.get(owner.getDescriptionId());
		if (bsmods != null) {
			for (Consumer<BlockStateBase> bsmod : bsmods) {
				try {
					bsmod.accept((BlockStateBase) (Object) this);
				} catch (Exception e) {
					JustLikeRays.LOGGER.error("exception in modifying blockstate: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		float emitByDist = lightEmission;
		config.maxEmission = Float.max(config.maxEmission, emitByDist);
	}

	@Override
	public void setLightBlock(int value) {

		if (cache != null) {
			this.cache.lightBlock = value;
		} else {
			JustLikeRays.LOGGER.warn("lava BlockState.cache was null, expect lava to be buggy/laggy");
			}
	}

	@Override
	public void setLightEmit(int value) {
		lightEmission = value;
	}

	/**
	 * fake constructor for java compiler sake
	 */
	protected BlockStateBaseMixin() {
		super(null, null, null);
	}

}
