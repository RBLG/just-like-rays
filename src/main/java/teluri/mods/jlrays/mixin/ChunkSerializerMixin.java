package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import teluri.mods.jlrays.ByteDataLayer;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

	@WrapOperation(//
			method = "read(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/ai/village/poi/PoiManager;Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/chunk/ProtoChunk;", //
			at = @At(value = "NEW", target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;", ordinal = 0))
	static private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original) {
		return new ByteDataLayer(data);
	}

}
