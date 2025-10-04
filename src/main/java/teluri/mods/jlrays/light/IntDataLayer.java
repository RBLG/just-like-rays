package teluri.mods.jlrays.light;

import java.util.Arrays;

//import io.netty.buffer.ByteBuf;
//import net.minecraft.network.codec.ByteBufCodecs;
//import net.minecraft.network.codec.StreamCodec;
import teluri.mods.jlrays.config.IDepthHandler;

/**
 * DataLayer implementation for integer sized light level
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class IntDataLayer extends DynamicDataLayer {
	protected int[] data;

	/**
	 * instantiate empty
	 */
	public IntDataLayer() {
		super();
	}

	/**
	 * instantiate empty with a default value
	 * 
	 * @param defaultvalue
	 */
	public IntDataLayer(int defaultvalue) {
		super(defaultvalue);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public IntDataLayer(byte[] ndata) {
		super(ndata);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public IntDataLayer(int[] ndata) {
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
	public IntDataLayer copy() {
		return this.data == null ? new IntDataLayer(this.defaultValue) : new IntDataLayer((int[]) this.data.clone());
	}

	@Override
	public int getDyn(int index) {
		return data[index];
	}

	@Override
	public void setDyn(int index, int value) {
		data[index] = value;
	}

	@Override
	public boolean isEmptyDyn() {
		return data == null;
	}

	@Override
	public void initDyn() {
		if (data == null) {
			data = new int[SIZE];
			if (defaultValue != 0) {
				Arrays.fill(data, defaultValue);
			}
		}
	}

	@Override
	public byte[] getData2() {
		byte[] rtn = new byte[SIZE * 4];
		int itr = 0;
		for (int bytes4 : data) {
			rtn[itr + 0] = (byte) (bytes4);
			rtn[itr + 1] = (byte) (bytes4 >>> 8);
			rtn[itr + 2] = (byte) (bytes4 >>> 16);
			rtn[itr + 3] = (byte) (bytes4 >>> 24);
			itr += 4;
		}
		return rtn;
	}

	@Override
	protected void initDyn(byte[] ndata) {
		data = new int[SIZE];
		for (int itr = 0; itr < data.length; itr++) {
			int itr2 = itr * 4;
			int b0 = ndata[itr2 + 0] & 0xFF;
			int b1 = ndata[itr2 + 1] & 0xFF;
			int b2 = ndata[itr2 + 2] & 0xFF;
			int b3 = ndata[itr2 + 3] & 0xFF;
			data[itr] = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
		}
	}

	public static class IntDataLayerFactory implements IDepthHandler {
		//private static final StreamCodec<ByteBuf, byte[]> INT_DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(SIZE * 4);

		//@Override
		//public StreamCodec<ByteBuf, byte[]> getCodec() {
		//	return INT_DATA_LAYER_STREAM_CODEC;
		//}

		@Override
		public DynamicDataLayer createDataLayer() {
			return new IntDataLayer();
		}

		@Override
		public DynamicDataLayer createDataLayer(byte[] data) {
			return new IntDataLayer(data);
		}

		@Override
		public DynamicDataLayer createDataLayer(int defaultval) {
			return new IntDataLayer(defaultval);
		}

		@Override
		public int getNibbleCount() {
			return 8;
		}
	}

	@Override
	protected int getNibbleCount() {
		return 8;
	}

}
