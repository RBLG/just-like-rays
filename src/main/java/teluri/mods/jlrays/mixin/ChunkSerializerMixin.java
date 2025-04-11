package teluri.mods.jlrays.mixin;

import java.util.Optional;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import teluri.mods.jlrays.light.ByteDataLayer;

/**
 * @author RBLG
 * @since
 */
@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin {

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(// read(PoiManager,RegionStorageInfo,ChunkPos,CompoundTag)->ProtoChunk to parse(LevelHeightAccessor, RegistryAccess, CompoundTag)
			method = "parse*", //
			at = @At(//
					value = "INVOKE", //
					target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;", //
					ordinal = 3//
			))
	static private Optional<DataLayer> newDataLayerWithByteArray(Optional<byte[]> self, Function<byte[], DataLayer> old, Operation<Optional<DataLayer>> original) {
		return self.map(ByteDataLayer::new);
	}

//	@ModifyArg(//
//			method = "parse*", //
//			index = 0, //
//			at = @At( //
//					value = "INVOKE", //
//					target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;" //
//			// ordinal = 0 //
//			))
//	static private Function<byte[], DataLayer> replaceConstructor(Function<byte[], DataLayer> original) {
//		return (data) -> data.length == 2048 ? new DataLayer(data) : new ByteDataLayer(data);
//	}

}
