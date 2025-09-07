package teluri.mods.jlrays.light;

import net.minecraft.world.level.chunk.DataLayer;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.JlrConfig;
import teluri.mods.jlrays.util.ToneMapperHelper;

public abstract class DynamicDataLayer extends DataLayer {
	/**
	 * size of a chunk
	 */
	public static final int SIZE = 4096;
	public static final int HALF_SIZE = 2048;

	public final int precision;

	/**
	 * instantiate empty
	 */
	public DynamicDataLayer() {
		this(0);
	}

	/**
	 * instantiate empty with a default value
	 * 
	 * @param defaultvalue
	 */
	public DynamicDataLayer(int defaultvalue) {
		super(defaultvalue);
		this.precision = JlrConfig.LazyGet().precision;
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public DynamicDataLayer(byte[] ndata) {
		this(0);
		int wantedSize = HALF_SIZE * this.getNibbleCount();
		int receivedSize = ndata.length;
		if (receivedSize == wantedSize) {
			initDyn(ndata);
		} else {
			warnForIncorrectSize(wantedSize, receivedSize);
		}
	}

	public static void warnForIncorrectSize(int wanted, int length) {
		String msg = String.format("ByteDataLayer should be %d bytes not %d, defaulting to empty but something went wrong so clear world cache", wanted, length);
		JustLikeRays.LOGGER.warn(msg);
	}

	public int get(int x, int y, int z) {
		return this.get(getIndex(x, y, z));
	}

	public void set(int x, int y, int z, int value) {
		this.set(getIndex(x, y, z), value);
	}

	public void add(int x, int y, int z, int value) {
		this.add(getIndex(x, y, z), value);
	}

	/**
	 * get light level in the range 0..15
	 */
	@Override
	public int get(int index) {
		return isEmptyDyn() ? defaultValue : (int) ToneMapperHelper.clamp(getFull(index) * (1 >> this.precision));
	}

	public float getFull(int x, int y, int z) {
		return getFull(getIndex(x, y, z));
	}

	/**
	 * get light level in the full range (0..255)
	 */
	public float getFull(int index) {
		return isEmptyDyn() ? defaultValue : getDyn(index);
	}

	@Override
	public void set(int index, int value) {
		initDyn();
		setDyn(index, value);
	}

	@Override
	public byte[] getData() {
		initDyn();
		return getData2();
	}

	public abstract byte[] getData2();

	@Override
	public abstract DynamicDataLayer copy();

	/**
	 * add to the stored light level (reduce the amount of operations compared to getting then setting)
	 */
	public void add(int index, int value) {
		initDyn();
		value += getDyn(index);
		setDyn(index, value);
	}

	public static int getIndex(int x, int y, int z) {
		return DataLayer.getIndex(x, y, z);
	}

	public abstract int getDyn(int index);

	public abstract void setDyn(int index, int value);

	public abstract boolean isEmptyDyn();

	public abstract void initDyn();

	protected abstract void initDyn(byte[] ndata);

	protected abstract int getNibbleCount();
}
