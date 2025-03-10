package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import teluri.mods.jlrays.light.ByteDataLayer;

/**
 * @author RBLG
 * @since 
 */
@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(// read(PoiManager,RegionStorageInfo,ChunkPos,CompoundTag)->ProtoChunk to parse(LevelHeightAccessor, RegistryAccess, CompoundTag)
			method = "read*", //
			at = @At(value = "NEW", target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;", ordinal = 0))
	static private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original) {
		return new ByteDataLayer(data);
	}

}
