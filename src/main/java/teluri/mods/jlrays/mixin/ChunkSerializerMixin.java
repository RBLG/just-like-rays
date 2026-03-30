package teluri.mods.jlrays.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.IDepthHandler;
import teluri.mods.jlrays.config.JlrConfig;
import teluri.mods.jlrays.util.ActionWithCooldown;

/**
 * @author RBLG
 * @since
 */
@Mixin(SerializableChunkData.class)
public class ChunkSerializerMixin {

	private static String BLOCK_LIGHT_JLR = "BlockLightJlr";

	protected static final ActionWithCooldown WARN_VANILLA = new ActionWithCooldown(20, () -> {
		String msg = "BlockLightJlr not found! this is probably vanilla light data. consider clearing world cache! "
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});
	protected static final ActionWithCooldown WARN_NON_2048 = new ActionWithCooldown(20, () -> {
		String msg = "BlockLight doesnt have a length of 2048, possibly from a previous jlr version. consider clearing world cache!"
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});
	protected static final ActionWithCooldown WARN_JLR_BAD_LENGTH = new ActionWithCooldown(20, () -> {
		String msg = "BlockLightJlr length isnt coherent with current config, it has probably changed. consider clearing world cache!"
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(method = "parse*", at = @At(value = "NEW", ordinal = 0, //
			target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;"))
	static private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original, @Local(ordinal = 2) CompoundTag compoundTag3) {
		if (data.length != 2048) {
			WARN_NON_2048.Do();
			return null;
		}
		if (!compoundTag3.contains(BLOCK_LIGHT_JLR, 7)) {
			WARN_VANILLA.Do();
			return null;
		}
		byte[] jlrdata = compoundTag3.getByteArray(BLOCK_LIGHT_JLR);
		IDepthHandler factory = JlrConfig.LazyGet().depthHandler;
		int wanted = factory.getDataLayerSize();
		if (jlrdata.length != wanted) {
			WARN_JLR_BAD_LENGTH.Do();
			return null;
		}
		byte[] both = Arrays.copyOf(data, data.length + jlrdata.length);
		System.arraycopy(jlrdata, 0, both, data.length, jlrdata.length);
		return factory.createDataLayer(both);
	}

	@WrapOperation(method = "write*", at = @At(value = "INVOKE", ordinal = 0, target = "net/minecraft/world/level/chunk/DataLayer.getData()[B"))
	static private byte[] writeBlockLightJlr(DataLayer datalayer, Operation<byte[]> original, //
			@Local(ordinal = 1) CompoundTag compoundTag2, //
			@Local SerializableChunkData.SectionData sectionData //
	) {
		byte[] merged = datalayer.getData();
		byte[] data = Arrays.copyOfRange(merged, 0, 2048);
		byte[] jlrdata = Arrays.copyOfRange(data, 2048, merged.length);
		compoundTag2.putByteArray(BLOCK_LIGHT_JLR, jlrdata);
		return data;
	}
}
