package teluri.mods.jlrays.light;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import teluri.mods.jlrays.config.IDepthHandler;

/**
 * DataLayer implementation for byte sized light level
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class ByteDataLayer extends DynamicDataLayer {

	/**
	 * instantiate empty
	 */
	public ByteDataLayer() {
		super();
	}

	/**
	 * instantiate empty with a default value
	 * 
	 * @param defaultvalue
	 */
	public ByteDataLayer(int defaultvalue) {
		super(defaultvalue);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public ByteDataLayer(byte[] ndata) {
		super(ndata);
	}

	@Override
	public ByteDataLayer copy() {
		return this.data == null ? new ByteDataLayer(this.defaultValue) : new ByteDataLayer((byte[]) this.data.clone());
	}

	@Override
	public int getDyn(int index) {
		return data[index] & 0xFF;
	}

	@Override
	public void setDyn(int index, int value) {
		data[index] = (byte) Math.clamp(value, 0, 0xFF);
	}

	@Override
	public boolean isEmptyDyn() {
		return data == null;
	}

	@Override
	public void initDyn() {
		if (data == null) {
			data = new byte[SIZE];
			if (defaultValue != 0) {
				Arrays.fill(data, (byte) defaultValue);
			}
		}
	}

	@Override
	public byte[] getData2() {
		return data;
	}

	@Override
	protected void initDyn(byte[] ndata) {
		data = ndata;
	}

	public static class ByteDataLayerFactory implements IDepthHandler {
		private static final StreamCodec<ByteBuf, byte[]> BYTE_DATA_LAYER_STREAM_CODEC = ByteBufCodecs.byteArray(SIZE);
		@Override
		public StreamCodec<ByteBuf, byte[]> getCodec() {
			return BYTE_DATA_LAYER_STREAM_CODEC;
		}
		@Override
		public DynamicDataLayer createDataLayer() {
			return new ByteDataLayer();
		}

		@Override
		public DynamicDataLayer createDataLayer(byte[] data) {
			return new ByteDataLayer(data);
		}

		@Override
		public DynamicDataLayer createDataLayer(int defaultval) {
			return new ByteDataLayer(defaultval);
		}

		@Override
		public int getNibbleCount() {
			return 2;
		}
	}

	@Override
	protected int getNibbleCount() {
		return 2;
	}
}
