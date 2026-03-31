package teluri.mods.jlrays.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

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
	private static String BLOCK_LIGHT_VANILLA = "BlockLight";

	private static final ActionWithCooldown WARN_NON_2048 = new ActionWithCooldown(20, () -> {
		String msg = "BlockLight doesnt have a length of 2048, possibly from a previous jlr version. consider clearing world cache!"
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});
	private static final ActionWithCooldown WARN_JLR_BAD_LENGTH = new ActionWithCooldown(20, () -> {
		String msg = "BlockLightJlr length isnt coherent with current config, it has probably changed. consider clearing world cache!"
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});
	private static final ActionWithCooldown WARN_BOUNCE_MISSMATCH = new ActionWithCooldown(20, () -> {
		String msg = "Fake light bounce is enabled but data for it is missing in saves. consider clearing world cache!"
				+ " (this warning is silenced for the next 20 triggers)";
		JustLikeRays.LOGGER.warn(msg);
	});

	@ModifyConstant(method = { "parse*", "write*" }, constant = @Constant(stringValue = "BlockLight"))
	private String injected(String value) {
		return BLOCK_LIGHT_JLR;
	}

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(method = "parse*", at = @At(value = "NEW", ordinal = 0, //
			target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;"))
	static private DataLayer newDataLayerWithByteArray(byte[] jlrdata, Operation<DataLayer> original, @Local(ordinal = 2) CompoundTag compoundTag3) {
		IDepthHandler factory = JlrConfig.LazyGet().depthHandler;
		int wanted = factory.getDataLayerSize();
		if (jlrdata.length != wanted) {
			WARN_JLR_BAD_LENGTH.Do();
			return null;
		}
		boolean hasBounceData = compoundTag3.contains(BLOCK_LIGHT_VANILLA);
		if (!JlrConfig.LazyGet().fakeLightBounce) {
			return factory.createDataLayer(jlrdata);
		}
		if (!hasBounceData) {
			WARN_BOUNCE_MISSMATCH.Do();
		}
		byte[] data = hasBounceData ? compoundTag3.getByteArray(BLOCK_LIGHT_VANILLA) : new byte[2048];
		if (data.length != 2048) {
			WARN_NON_2048.Do();
			return null;
		}
		byte[] both = Arrays.copyOf(jlrdata, jlrdata.length + 2048);
		System.arraycopy(data, 0, both, jlrdata.length, 2048);

		return factory.createDataLayer(both);
	}

	@WrapOperation(method = "write*", at = @At(value = "INVOKE", ordinal = 0, target = "net/minecraft/world/level/chunk/DataLayer.getData()[B"))
	static private byte[] writeBlockLightJlr(DataLayer datalayer, Operation<byte[]> original, //
			@Local(ordinal = 1) CompoundTag compoundTag2, //
			@Local SerializableChunkData.SectionData sectionData //
	) {
		int size = JlrConfig.LazyGet().depthHandler.getDataLayerSize();
		byte[] merged = original.call(datalayer);
		if (!JlrConfig.LazyGet().fakeLightBounce) {
			return merged;
		}
		byte[] data = Arrays.copyOfRange(merged, 0, size);
		byte[] jlrdata = Arrays.copyOfRange(merged, size, merged.length);
		compoundTag2.putByteArray(BLOCK_LIGHT_VANILLA, data);
		return jlrdata;
	}
}
