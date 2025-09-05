package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import teluri.mods.jlrays.config.IDepthHandler;
import teluri.mods.jlrays.config.JlrConfig;
import teluri.mods.jlrays.light.DynamicDataLayer;

/**
 * @author RBLG
 * @since
 */
@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin {

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(method = "parse*", //
			at = @At(value = "NEW", target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;", ordinal = 0))
	static private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original) {
		IDepthHandler factory = JlrConfig.LazyGet().depthHandler;
		int wanted = factory.getDataLayerSize();
		if (data.length == wanted) {
			return factory.createDataLayer(data);
		} else {
			DynamicDataLayer.warnForIncorrectSize(data.length, wanted);
			return null;
		}
	}
}
