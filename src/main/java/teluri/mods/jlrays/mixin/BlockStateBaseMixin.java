package teluri.mods.jlrays.mixin;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
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
import net.minecraft.world.level.material.FluidState;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.BlockConfig;
import teluri.mods.jlrays.misc.IHasEmitProperties;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

/**
 * @author RBLG
 * @since v0.0.4
 */
@Mixin(BlockStateBase.class)
public class BlockStateBaseMixin extends StateHolder<Block, BlockState> implements IHasEmitProperties {

	@Shadow
	private int lightEmission;
	@Shadow
	private int lightBlock;
	@Shadow
	private FluidState fluidState;

	@Nullable
	protected EmitProperties emitprops = null;

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
			if (emitprops != null && !emitprops.isValid()) {
				JustLikeRays.LOGGER.warn(owner.getDescriptionId() + "'s blockstate had invalid offset and/or radius");
				emitprops.enforceValidity();
			}
		}
		float emitByDist = lightEmission;
		if (emitprops == null) {
			emitByDist *= emitprops.emitScale;
		}
		config.maxEmission = Float.max(config.maxEmission, emitByDist);
	}

	@Override
	public void setLightBlock(int value) {
		lightBlock = value;
	}

	@Override
	public void setLightEmit(int value) {
		lightEmission = value;
	}

	@Override
	public EmitProperties initEmitProperties() {
		if (emitprops == null) {
			emitprops = new EmitProperties();
		}
		return emitprops;
	}

	@Override
	public @Nullable EmitProperties getEmitPropertiesNullable() {
		return emitprops;
	}

	/**
	 * fake constructor for java compiler sake
	 */
	protected BlockStateBaseMixin(Block owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, MapCodec<BlockState> propertiesCodec) {
		super(owner, values, propertiesCodec);
	}

}
