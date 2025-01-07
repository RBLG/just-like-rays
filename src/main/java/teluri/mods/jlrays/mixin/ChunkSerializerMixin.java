package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import teluri.mods.jlrays.ByteDataLayer;

@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin {

	@WrapOperation(// read(PoiManager,RegionStorageInfo,ChunkPos,CompoundTag)->ProtoChunk to parse(LevelHeightAccessor, RegistryAccess, CompoundTag)
			method = "parse*", //
			at = @At(value = "NEW", target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;", ordinal = 0))
	static private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original) {
		return new ByteDataLayer(data);
	}

}
