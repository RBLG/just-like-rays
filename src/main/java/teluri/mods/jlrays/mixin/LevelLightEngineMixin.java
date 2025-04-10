package teluri.mods.jlrays.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LightEventListener;
import teluri.mods.jlrays.light.JlrBlockLightEngineAdapter;

@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngineMixin implements LightEventListener {
	@Shadow
	@Nullable
	private LightEngine<?, ?> blockEngine;

	@Inject(method = "<init>*", at = @At(value = "RETURN"))
	public void replaceBlockEngineOnInit(LightChunkGetter lightChunkGetter, boolean blockLight, boolean skyLight, CallbackInfo info) {
		blockEngine = blockLight ? new JlrBlockLightEngineAdapter(lightChunkGetter) : null;
	}

}
