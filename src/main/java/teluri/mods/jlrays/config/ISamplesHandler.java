package teluri.mods.jlrays.config;

import java.util.Locale;

import com.google.common.collect.HashBiMap;

import teluri.mods.jlrays.util.MiscHelper;

public interface ISamplesHandler {

	int getSampleCount();

	public static enum SamplesHandlers implements ISamplesHandler {
		ONE_PER_VOLUME(1) {

		},
		// ONE_PER_FACE (3){}
		;

		private int count;

		SamplesHandlers(int ncount) {
			count = ncount;
		}

		public int getSampleCount() {
			return count;
		}
	}

	public static final HashBiMap<SamplesHandlers, String> SAMPLES_HANDLERS = MiscHelper.using(HashBiMap.create(), (h) -> {
		for (SamplesHandlers handler : SamplesHandlers.values()) {
			h.put(handler, handler.name().toLowerCase(Locale.ROOT));
		}
	});
}
