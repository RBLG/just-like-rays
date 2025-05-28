package teluri.mods.jlrays.config;

import java.util.Locale;

import com.google.common.collect.HashBiMap;

import teluri.mods.jlrays.util.MiscHelper;

public interface IChannelsHandler {

	int getChannelCount();

	public static enum ChannelsHandlers implements IChannelsHandler {
		MONO(1) {

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
