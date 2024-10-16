package teluri.mods.jlrays.mixin;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin extends LightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//////////////////////////////////////////// yeet /////////////
	
	protected BlockLightEngineMixin(LightChunkGetter lightChunkGetter, BlockLightSectionStorage layerLightSectionStorage) {
		super(lightChunkGetter, layerLightSectionStorage);
		// TODO Auto-generated constructor stub
	}


	//@Inject(at = @At("HEAD"), method = "loadLevel")
	//private void init(CallbackInfo info);
}