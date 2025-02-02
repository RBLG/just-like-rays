package teluri.mods.jlrays.mixin;

import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * @author RBLG
 * @since v0.0.4
 */
@Mixin(BlockStateBase.class)
public class BlockStateBaseMixin extends StateHolder<Block, BlockState> {

	@Shadow
	private int lightEmission;


//	@Shadow
//	private int lightBlock;

	/**
	 * modify emition and opacity of lava blockstates
	 */
//	@Inject(method = "<init>*", at = @At("TAIL"))
//	protected void dataDrivenInit(Block owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, MapCodec<BlockState> propertiesCodec, CallbackInfo info) {
//
//	}
	
	/**
	 * modify fields based on description id. to prepare
	 * @param info
	 */
	@Inject(method = "initCache()V", at = @At("RETURN"))
	public void dataDrivenCacheInit(CallbackInfo info) {
		if (Objects.equals(owner.getDescriptionId(), "block.minecraft.lava")) {

			this.lightEmission = 10;
			//this.lightBlock = 15; //cause issues with transparent blocks
		}
	}

	/**
	 * fake constructor for java compiler sake
	 */
	protected BlockStateBaseMixin(Block object, ImmutableMap<Property<?>, Comparable<?>> immutableMap, MapCodec<BlockState> mapCodec) {
		super(object, immutableMap, mapCodec);
	}
}
