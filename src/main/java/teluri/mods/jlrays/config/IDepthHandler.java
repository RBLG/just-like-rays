package teluri.mods.jlrays.config;

import teluri.mods.jlrays.light.ByteDataLayer2;
import teluri.mods.jlrays.light.DynamicDataLayer;
import teluri.mods.jlrays.light.IntDataLayer;

public interface IDepthHandler {
	int getNibbleCount();

	DynamicDataLayer createDataLayer();

	DynamicDataLayer createDataLayer(int defaultval);

	DynamicDataLayer createDataLayer(byte[] data);

	default int getDataLayerSize() {
		return getNibbleCount() * 2048;
	}

	public static final IDepthHandler BYTE = new ByteDataLayer2.ByteDataLayerFactory();
	public static final IDepthHandler SHORT = new ShortDataLayer.ShortDataLayerFactory();
	public static final IDepthHandler INT = new IntDataLayer.IntDataLayerFactory();
	
}
