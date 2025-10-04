package teluri.mods.jlrays.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * @author RBLG
 * @since
 */
@Mixin(ClientboundLightUpdatePacketData.class)
public class ClientboundLightUpdatePacketDataMixin {

	@ModifyArg(
			method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/network/FriendlyByteBuf;writeCollection(Ljava/util/Collection;Lnet/minecraft/network/FriendlyByteBuf$Writer;)V"
			),
			index = 1
	)
	private <T> FriendlyByteBuf.Writer<T> modifyWriteCollection(FriendlyByteBuf.Writer<T> original) {
		return (buffer, value) -> {
			if (value instanceof byte[]) {
				ensureWritable(buffer, ((byte[]) value).length);
			}
			original.accept(buffer, value);
		};
	}

	private void ensureWritable(FriendlyByteBuf buffer, int minWritableBytes) {
		if (buffer.writableBytes() < minWritableBytes) {
			buffer.capacity(buffer.capacity() + minWritableBytes);
		}
	}
}