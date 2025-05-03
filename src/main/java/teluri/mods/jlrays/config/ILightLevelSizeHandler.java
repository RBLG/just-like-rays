package teluri.mods.jlrays.config;

public interface ILightLevelSizeHandler {
	int getNibbleCount();

	default int getDataLayerSize() {
		return getNibbleCount() * 2048;
	}

	default int translateIndex(int index) {
		return index * (getNibbleCount() >> 1);

	}

	public static enum LightLevelSizes implements ILightLevelSizeHandler {
		NIBBLE(1) {
			public int translateIndex(int index) {
				return index >> 1;
			}
		},
		BYTE(2), TWO_BYTES(3);

		LightLevelSizes(int nsize) {
			size = nsize;
		}

		int size;

		@Override
		public int getNibbleCount() {
			return size;
		}
	}
}
