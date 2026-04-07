package teluri.mods.jlrays.light;

import java.util.Arrays;

import org.joml.Math;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.chunk.DataLayer;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.JlrConfig;
import teluri.mods.jlrays.util.ToneMapperHelper;

/**
 * common part of the implementations of the different sizes of DataLayer
 * 
 * @author RBLG
 * @since v0.2.0
 */
public abstract class DynamicDataLayer extends DataLayer {
	// TODO maybe lazy init to ensure it doesnt happen to early?
	public static final StreamCodec<ByteBuf, byte[]> DYNAMIC_STREAM_CODEC = ByteBufCodecs.byteArray(JlrConfig.LazyGet().getFullDataLayerSize());
	/**
	 * size of a chunk
	 */
	public static final int SIZE = 4096;
	public static final int HALF_SIZE = 2048;
	public static final int MERGED_SIZE = 4096 + 2048;

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
		int wantedSize = getExpectedSize();
		int receivedSize = ndata.length;
		if (receivedSize == wantedSize) {
			data = ndata;
		} else {
			data = new byte[wantedSize];
			warnForIncorrectSize(wantedSize, receivedSize);
		}
	}

	public static void warnForIncorrectSize(int wanted, int length) {
		String msg = "ByteDataLayer should be %d bytes not %d, defaulting to empty but something went wrong so clear world cache";
		JustLikeRays.LOGGER.warn(String.format(msg, wanted, length));
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
		if (isEmpty()) {
			return defaultValue;
		}
		int main = (int) ToneMapperHelper.clamp(getFull(index) * (1 >> this.precision));
		if (!JlrConfig.LazyGet().fakeLightBounce) {
			return main;
		}
		int bounce = this.getBounced(getByteIndex(index), getNibbleIndex(index));
		return Math.max(main, bounce);
	}

	public float getFull(int x, int y, int z) {
		return getFull(getIndex(x, y, z));
	}

	/**
	 * get light level in the full range (0..255)
	 */
	public float getFull(int index) {
		return isEmpty() ? defaultValue : getDyn(index);
	}

	@Override
	public void set(int index, int value) {
		init();
		setDyn(index, value);
	}

	@Override
	public byte[] getData() {
		init();
		return data;
	}

	@Override
	public abstract DynamicDataLayer copy();

	public boolean isEmpty() {
		return data == null;
	}

	/**
	 * add to the stored light level (reduce the amount of operations compared to getting then setting)
	 */
	public void add(int index, int value) {
		init();
		value += getDyn(index);
		setDyn(index, value);
	}

	public void init() {
		if (data == null) {
			data = new byte[getExpectedSize()];
			if (defaultValue != 0) { // TODO respecialize fill?
				Arrays.fill(data, (byte) defaultValue);
			}
		}
	}

	public int getExpectedSize() {
		return HALF_SIZE * this.getNibbleCount() + (JlrConfig.LazyGet().fakeLightBounce ? 2048 : 0);
	}

	public int getBounceDataIndexStart() {
		return HALF_SIZE * this.getNibbleCount();
	}

	public static int getIndex(int x, int y, int z) {
		return DataLayer.getIndex(x, y, z);
	}

	public abstract int getDyn(int index);

	public abstract void setDyn(int index, int value);

	protected abstract int getNibbleCount();

	public static int getIndexBounced(int x, int y, int z) {
		return y << 8 | z << 4 | x;
	}

	public int getBounced(int x, int y, int z) {
		int index = getIndexBounced(x, y, z);
		return this.getBounced(getByteIndex(index), getNibbleIndex(index));
	}

	public void setBounced(int x, int y, int z, int value) {
		int index = getIndexBounced(x, y, z);
		this.setBounced(getByteIndex(index), getNibbleIndex(index), value);
	}

	public int getBounced(int bindex, int nindex) {
		return isEmpty() ? defaultValue : data[bindex] >> 4 * nindex & 15;
	}

	public void setBounced(int bindex, int nindex, int value) {
		init();
		int k = ~(15 << 4 * nindex);
		int l = (value & 15) << 4 * nindex;
		data[bindex] = (byte) (data[bindex] & k | l);
	}
}
