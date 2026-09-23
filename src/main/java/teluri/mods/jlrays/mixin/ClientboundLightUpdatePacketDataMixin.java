package teluri.mods.jlrays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import teluri.mods.jlrays.config.JlrConfig;

/**
 * @author RBLG
 * @since
 */
@Mixin(ClientboundLightUpdatePacketData.class)
public class ClientboundLightUpdatePacketDataMixin {
	
	/**
	 * change the size in codec based on config
	 */
	@ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 2048))
	private static int getBlockLightDataLayerSize(int value) {
		return JlrConfig.LazyGet().depthHandler.getDataLayerSize();
	}

}
