package teluri.mods.jlrays.config;

//import io.netty.buffer.ByteBuf;
//import net.minecraft.network.codec.StreamCodec;
import teluri.mods.jlrays.light.ByteDataLayer;
import teluri.mods.jlrays.light.DynamicDataLayer;
import teluri.mods.jlrays.light.IntDataLayer;
import teluri.mods.jlrays.light.ShortDataLayer;

public interface IDepthHandler {
	int getNibbleCount();

	DynamicDataLayer createDataLayer();

	DynamicDataLayer createDataLayer(int defaultval);

	DynamicDataLayer createDataLayer(byte[] data);

	default int getDataLayerSize() {
		return getNibbleCount() * 2048;
	}
	
	//StreamCodec<ByteBuf, byte[]> getCodec();

	public static final IDepthHandler BYTE = new ByteDataLayer.ByteDataLayerFactory();
	public static final IDepthHandler SHORT = new ShortDataLayer.ShortDataLayerFactory();
	public static final IDepthHandler INT = new IntDataLayer.IntDataLayerFactory();
	
}
