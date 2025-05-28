package teluri.mods.jlrays.config;

import java.util.function.Supplier;

import teluri.mods.jlrays.light.ByteDataLayer2;
import teluri.mods.jlrays.light.DynamicDataLayer;
import teluri.mods.jlrays.light.IntDataLayer;
import teluri.mods.jlrays.light.JlrBlockLightEngine;
import teluri.mods.jlrays.light.ShortDataLayer;

public class CurrentConfig {
	public static CurrentConfig current;

	public IChannelsHandler channelsHandler = JlrConfig.CONFIG.channels.get();
	public ISamplesHandler samplesHandler = JlrConfig.CONFIG.samples.get();
	public final int channelCount = channelsHandler.getChannelCount();
	public final int sampleCount = samplesHandler.getSampleCount();
	// DataLayer size
	public final int oneDataSize = channelCount * sampleCount;
	public final int dataSize = 4096 * oneDataSize;

	public final float distanceRatio = JlrConfig.CONFIG.scale.get();
	public final float minimumValue = JlrConfig.CONFIG.cutoff.get();
	public final float rangeEdgeNumber = 1 / (minimumValue * distanceRatio); // number used to get the edge from the source intensity

	// range of the highest value emissive source possible. define how far to search for sources
	public final int maxRange = JlrBlockLightEngine.getRange(Settings.settings.maxEmission);

	public int depth = JlrConfig.CONFIG.depth.get();

	public final Supplier<DynamicDataLayer> dataLayerFactory = switch (depth) {
	// case 0 -> () -> new ByteDataLayer(); //TODO nibble size
	case 1 -> () -> new ByteDataLayer2();
	case 2 -> () -> new ShortDataLayer();
	case 3, 4 -> () -> new IntDataLayer();
	default -> throw new IllegalArgumentException("Unexpected value: " + depth);
	};

}
