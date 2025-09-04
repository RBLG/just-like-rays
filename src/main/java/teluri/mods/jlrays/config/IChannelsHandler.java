package teluri.mods.jlrays.config;

import java.util.Locale;

import org.joml.Vector3i;

import com.google.common.collect.HashBiMap;

import net.minecraft.core.SectionPos;
import teluri.mods.jlrays.light.DynamicDataLayer;
import teluri.mods.jlrays.light.JlrBlockLightEngine;
import teluri.mods.jlrays.light.misc.TaskCache;
import teluri.mods.jlrays.light.sight.misc.ISightUpdateConsumer;
import teluri.mods.jlrays.util.MiscHelper;

public interface IChannelsHandler {

	int getChannelCount();

	void handleChannelsInSample(int index1, int sample);

	ISightUpdateConsumer getSightConsumer(Vector3i source, int oldemit, int newemit, TaskCache taskCache);

	public static enum ChannelsHandlers implements IChannelsHandler {
		MONO(1) {

			@Override
			public ISightUpdateConsumer getSightConsumer(Vector3i source, int oldemit, int newemit, TaskCache taskCache) {
				return (xyz, ovisi, nvisi) -> {
					DynamicDataLayer data = taskCache.getCachedDataLayer(xyz.x, xyz.y, xyz.z);
					if (data == null) {
						return;
					}
					int change = JlrBlockLightEngine.getLightLevelChange(source, xyz, ovisi, nvisi, oldemit, newemit);
					if (change != 0) {
						int lx = SectionPos.sectionRelative(xyz.x);
						int ly = SectionPos.sectionRelative(xyz.y);
						int lz = SectionPos.sectionRelative(xyz.z);
						int index = DynamicDataLayer.getIndex2(lx, ly, lz, 0, 0);
						data.add(index, change);
						taskCache.notifyUpdate(xyz.x, xyz.y, xyz.z);
					}

				};

			}

		},
		// RGB(3){}
		;

		private int count;

		ChannelsHandlers(int ncount) {
			count = ncount;
		}

		public int getChannelCount() {
			return count;
		}
	}

	public static final HashBiMap<IChannelsHandler, String> CHANNELS_HANDLERS = MiscHelper.using(HashBiMap.create(), (h) -> {
		for (ChannelsHandlers handler : ChannelsHandlers.values()) {
			h.put(handler, handler.name().toLowerCase(Locale.ROOT));
		}
	});
}
