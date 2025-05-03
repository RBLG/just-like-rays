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
import teluri.mods.jlrays.config.Settings;
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
		ArrayList<Consumer<BlockStateBase>> bsmods = Settings.settings.blockstates.get(owner.getDescriptionId());
		if (bsmods != null) {
			for (Consumer<BlockStateBase> bsmod : bsmods) {
				bsmod.accept((BlockStateBase) (Object) this);
			}
		}
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
