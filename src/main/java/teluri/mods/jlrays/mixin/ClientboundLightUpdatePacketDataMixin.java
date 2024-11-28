package teluri.mods.jlrays.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.netty.buffer.ByteBuf;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import teluri.mods.jlrays.mixed.IBetterDataLayer;

//@Debug(export=true)
@Mixin(ClientboundLightUpdatePacketData.class)
public class ClientboundLightUpdatePacketDataMixin {
	private static final StreamCodec<ByteBuf, byte[]> BLOCK_LIGHT_DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(IBetterDataLayer.BYTE_SIZED);

	@ModifyExpressionValue(//
			method = {"write(Lnet/minecraft/network/FriendlyByteBuf;)V","<init>(Lnet/minecraft/network/FriendlyByteBuf;II)V"}, //
			at = @At(//
					value = "FIELD", //
					target = "Lnet/minecraft/network/protocol/game/ClientboundLightUpdatePacketData;DATA_LAYER_STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;", //
					opcode = Opcodes.GETSTATIC, //
					ordinal = 1//
			) //
	)
	public StreamCodec<ByteBuf, byte[]> getBlockLightDataLayerStreamCodec(StreamCodec<ByteBuf, byte[]> prev) {
		return BLOCK_LIGHT_DATA_LAYER_STREAM_CODEC;
	}

}