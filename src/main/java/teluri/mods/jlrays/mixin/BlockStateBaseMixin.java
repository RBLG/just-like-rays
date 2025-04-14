package teluri.mods.jlrays.mixin;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;
import teluri.mods.jlrays.JustLikeRays;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * @author RBLG
 * @since v0.0.4
 */
@Mixin(BlockStateBase.class)
public class BlockStateBaseMixin extends StateHolder<Block, BlockState> {

	@Shadow
	private int lightEmission;
	// @Shadow
	// private int lightBlock;
	@Shadow
	private FluidState fluidState;
	@Nullable
	protected BlockBehaviour.BlockStateBase.Cache cache;

	/**
	 * modify emition and opacity of lava blockstates
	 */
	// @Inject(method = "<init>*", at = @At("TAIL"))
	// protected void dataDrivenInit(Block owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, MapCodec<BlockState> propertiesCodec, CallbackInfo info) {
	// }

	/**
	 * modify fields based on description id. will be used for settings
	 * 
	 * @param info
	 */
	@Inject(method = "initCache()V", at = @At("RETURN"))
	public void dataDrivenCacheInit(CallbackInfo info) {
		if (owner instanceof LiquidBlock && Objects.equals(owner.getDescriptionId(), "block.minecraft.lava")) {

			this.lightEmission = 7;

			int lightBlock = fluidState.isSource() ? 15 : 0;
			if (cache != null) {
				cache.lightBlock = lightBlock;
			} else {
				
				JustLikeRays.LOGGER.info("lava block's cache didnt existed so lightBlock couldnt be set, expect lava lakes to be laggy");
			}
		}
	}

	/**
	 * fake constructor for java compiler sake
	 */
	protected BlockStateBaseMixin() {
		super(null, null, null);
	}

}
