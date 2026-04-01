package teluri.mods.jlrays.client.mixin;

import java.util.BitSet;
import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.JlrConfig;

/**
 * @author RBLG
 * @since v0.0.1
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	/**
	 * replace a call to the DataLayer() to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(// //TODO shorten descriptors?
			method = "readSectionList(IILnet/minecraft/world/level/lighting/LevelLightEngine;Lnet/minecraft/world/level/LightLayer;Ljava/util/BitSet;Ljava/util/BitSet;Ljava/util/Iterator;Z)V", //
			at = @At(value = "NEW", target = "()Lnet/minecraft/world/level/chunk/DataLayer;"))
	private DataLayer newDataLayer(Operation<DataLayer> original, int x, int z, LevelLightEngine lightEngine, LightLayer lightLayer, BitSet skyYMask, BitSet emptySkyYMask, Iterator<byte[]> skyUpdates, boolean update) {
		if (lightLayer != LightLayer.BLOCK) {
			return original.call();
		}
		return JlrConfig.LazyGet().depthHandler.createDataLayer();
	}

	/**
	 * replace a call to the DataLayer(byte[]) to the ByteDataLayer equivalent at chunk loading
	 */
	@WrapOperation(//
			method = "readSectionList(IILnet/minecraft/world/level/lighting/LevelLightEngine;Lnet/minecraft/world/level/LightLayer;Ljava/util/BitSet;Ljava/util/BitSet;Ljava/util/Iterator;Z)V", //
			at = @At(value = "NEW", target = "([B)Lnet/minecraft/world/level/chunk/DataLayer;"))
	private DataLayer newDataLayerWithByteArray(byte[] data, Operation<DataLayer> original, int x, int z, LevelLightEngine lightEngine, LightLayer lightLayer, BitSet skyYMask, BitSet emptySkyYMask, Iterator<byte[]> skyUpdates, boolean update) {
		if (lightLayer != LightLayer.BLOCK) {
			return original.call(data);
		}
		JlrConfig conf = JlrConfig.LazyGet();
		if (data.length != conf.getFullDataLayerSize()) {
			String msg = "received a packet with a length incoherent with jlr config, server config is probably different from this client config?";
			JustLikeRays.LOGGER.warn(msg);
		}

		return conf.depthHandler.createDataLayer(data);

	}
}
