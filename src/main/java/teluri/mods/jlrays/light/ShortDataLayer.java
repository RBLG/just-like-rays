package teluri.mods.jlrays.light;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import teluri.mods.jlrays.config.IDepthHandler;

public class ShortDataLayer extends DynamicDataLayer {
	protected short[] data;

	/**
	 * instantiate empty
	 */
	public ShortDataLayer() {
		super();
	}

	/**
	 * instantiate empty with a default value
	 * 
	 * @param defaultvalue
	 */
	public ShortDataLayer(int defaultvalue) {
		super(defaultvalue);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public ShortDataLayer(byte[] ndata) {
		super(ndata);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public ShortDataLayer(short[] ndata) {
		this(0);
		int wantedSize = SIZE;
		int receivedSize = ndata.length;
		if (receivedSize == wantedSize) {
			data = ndata;
		} else {
			warnForIncorrectSize(wantedSize, receivedSize);
		}
	}

	@Override
	public ShortDataLayer copy() {
		return this.data == null ? new ShortDataLayer(this.defaultValue) : new ShortDataLayer((short[]) this.data.clone());
	}

	@Override
	public int getDyn(int index) {
		return data[index] & 0xFFFF;
	}

	@Override
	public void setDyn(int index, int value) {
		data[index] = (short) Math.clamp(value, 0, 0xFFFF);
	}

	@Override
	public boolean isEmptyDyn() {
		return data == null;
	}

	@Override
	public void initDyn() {
		if (data == null) {
			data = new short[SIZE];
			if (defaultValue != 0) {
				Arrays.fill(data, (short) defaultValue);
			}
		}
	}

	@Override
	public byte[] getData2() {
		byte[] rtn = new byte[SIZE * 2];
		int itr = 0;
		for (short bytes2 : data) {
			rtn[itr + 0] = (byte) (bytes2);
			rtn[itr + 1] = (byte) (bytes2 >>> 8);
			itr += 2;
		}
		return rtn;
	}

	@Override
	protected void initDyn(byte[] ndata) {
		data = new short[SIZE];
		for (int itr = 0; itr < data.length; itr++) {
			int itr2 = itr * 2;
			int b0 = ndata[itr2 + 0] & 0xFF;
			int b1 = ndata[itr2 + 1] & 0xFF;
			data[itr] = (short) (b0 | (b1 << 8));
		}
	}

	public static class ShortDataLayerFactory implements IDepthHandler {
		private static final StreamCodec<ByteBuf, byte[]> SHORT_DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(SIZE * 2);

		@Override
		public StreamCodec<ByteBuf, byte[]> getCodec() {
			return SHORT_DATA_LAYER_STREAM_CODEC;
		}

		@Override
		public DynamicDataLayer createDataLayer() {
			return new ShortDataLayer();
		}

		@Override
		public DynamicDataLayer createDataLayer(byte[] data) {
			return new ShortDataLayer(data);
		}

		@Override
		public DynamicDataLayer createDataLayer(int defaultval) {
			return new ShortDataLayer(defaultval);
		}

		@Override
		public int getNibbleCount() {
			return 4;
		}
	}

	@Override
	protected int getNibbleCount() {
		return 4;
	}

}
