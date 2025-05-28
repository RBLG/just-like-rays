package teluri.mods.jlrays.light;

import net.minecraft.world.level.chunk.DataLayer;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.CurrentConfig;
import teluri.mods.jlrays.util.ToneMapperHelper;

public abstract class DynamicDataLayer extends DataLayer {
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
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public DynamicDataLayer(byte[] ndata) {
		this(0);
		int wantedSize = CurrentConfig.current.dataSize * CurrentConfig.current.depth; // TODO will break if nibble sized=0
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
		return this.get(getIndex2(x, y, z, 0, 0));
	}

	public void set(int x, int y, int z, int value) {
		this.set(getIndex2(x, y, z, 0, 0), value); // TODO handle conversion
	}

	/**
	 * get light level in the range 0..15
	 */
	@Override
	public int get(int index) {
		return isEmptyDyn() ? defaultValue : ToneMapperHelper.clamp(getFull(index));
	}

	public int getFull(int x, int y, int z, int sample, int channel) {
		return getFull(getIndex2(x, y, z, sample, channel));
	}

	/**
	 * get light level in the full range (0..255)
	 */
	public int getFull(int index) {
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
	protected void add(int index, int value) {
		initDyn();
		value += getDyn(index);
		setDyn(index, value);
	}

	public static int getIndex(int x, int y, int z) {
		int ods = CurrentConfig.current.oneDataSize;
		return (DataLayer.getIndex(x, y, z)) * ods;
	}

	public static int getIndex2(int x, int y, int z, int sample, int channel) {
		int ods = CurrentConfig.current.oneDataSize;
		int cc = CurrentConfig.current.channelCount;
		return (DataLayer.getIndex(x, y, z)) * ods + sample * cc + channel;
	}

	public abstract int getDyn(int index);

	public abstract void setDyn(int index, int value);

	public abstract boolean isEmptyDyn();

	public abstract void initDyn();

	protected abstract void initDyn(byte[] ndata);
}
