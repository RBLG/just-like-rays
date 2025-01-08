package teluri.mods.jlrays;

import java.util.Arrays;

import org.joml.Math;

import net.minecraft.Util;
import net.minecraft.world.level.chunk.DataLayer;

/**
 * @author RBLG
 * @since v0.0.1
 * 
 * 
 */
public class ByteDataLayer extends DataLayer {
	public static int BYTE_SIZED = 4096;

	public ByteDataLayer() {
		super(0);
	}

	public ByteDataLayer(int size) {
		super(size);
	}

	public ByteDataLayer(byte[] ndata) {
		super(0);
		this.data = ndata;
		if (ndata.length != BYTE_SIZED) {
			throw (IllegalArgumentException) Util.pauseInIde(new IllegalArgumentException("ByteDataLayer should be 4096 bytes not: " + ndata.length));
		}
	}

	@Override
	public int get(int index) {
		if (this.data == null) {
			return this.defaultValue;
		}
		return (int) ToneMapperHelper.tonemap(getFull(index), 15, 15);
	}
	
	public int getFull(int x, int y, int z) {
		return getFull(getIndex(x, y, z));
	}

	public int getFull(int index) {
		if (this.data == null) {
			return this.defaultValue;
		}
		return data[index] & 0xFF; // cast to int as unsigned byte
	}
	

	@Override
	public void set(int index, int value) {
		byte[] bs = this.getData();
		bs[index] = (byte) Math.clamp(value, 0, 255);
	}

	@Override
	public byte[] getData() {
		if (this.data == null) {
			this.data = new byte[BYTE_SIZED];
			if (this.defaultValue != 0) {
				Arrays.fill(this.data, (byte) this.defaultValue);
			}
		}
		return this.data;
	}

	@Override
	public ByteDataLayer copy() {
		return this.data == null ? new ByteDataLayer(this.defaultValue) : new ByteDataLayer((byte[]) this.data.clone());
	}

	public void add(int x, int y, int z, int value) {
		this.add(getIndex(x, y, z), value);
	}

	protected void add(int index, int value) {
		byte[] bs = this.getData();
		value += bs[index] & 0xFF; // cast to int as unsigned byte
		bs[index] = (byte) Math.clamp(value, 0, 255);
	}
	
}
