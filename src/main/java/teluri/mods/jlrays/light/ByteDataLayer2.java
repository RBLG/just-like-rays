package teluri.mods.jlrays.light;

import java.util.Arrays;

import teluri.mods.jlrays.config.CurrentConfig;

public class ByteDataLayer2 extends DynamicDataLayer {

	/**
	 * instantiate empty
	 */
	public ByteDataLayer2() {
		super();
	}

	/**
	 * instantiate empty with a default value
	 * 
	 * @param defaultvalue
	 */
	public ByteDataLayer2(int defaultvalue) {
		super(defaultvalue);
	}

	/**
	 * instantiate with existing data
	 * 
	 * @param ndata
	 */
	public ByteDataLayer2(byte[] ndata) {
		super(ndata);
	}

	@Override
	public ByteDataLayer2 copy() {
		return this.data == null ? new ByteDataLayer2(this.defaultValue) : new ByteDataLayer2((byte[]) this.data.clone());
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
			data = new byte[CurrentConfig.current.dataSize];
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

}
