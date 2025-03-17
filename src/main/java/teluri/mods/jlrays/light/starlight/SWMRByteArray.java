package teluri.mods.jlrays.light.starlight;

import java.util.ArrayDeque;

import org.joml.Math;

import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import teluri.mods.jlrays.util.ToneMapperHelper;

public class SWMRByteArray extends SWMRNibbleArray {

	public static final int ARRAY_SIZE = 16 * 16 * 16; // blocks / bytes per block
	// this allows us to maintain only 1 byte array when we're not updating

	public static final ThreadLocal<ArrayDeque<byte[]>> WORKING_BYTES_POOL_2 = ThreadLocal.withInitial(ArrayDeque::new);

	@Override
	protected byte[] allocateBytes() {
		final byte[] inPool = WORKING_BYTES_POOL_2.get().pollFirst();
		if (inPool != null) {
			return inPool;
		}

		return new byte[getArraySize()];
	}

	@Override
	protected void freeBytes(final byte[] bytes) {
		WORKING_BYTES_POOL_2.get().addFirst(bytes);
	}

	public SWMRByteArray() {
		this(null, false); // lazy init
	}

	public SWMRByteArray(final byte[] bytes) {
		this(bytes, false);
	}

	// duplicated from starlight because java is cringe with constructors
	public SWMRByteArray(final byte[] bytes, final boolean isNullNibble) {
		super(bytes, isNullNibble);
	}

	// same
	public SWMRByteArray(final byte[] bytes, final int state) {
		super(bytes, state);
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("State: ");
		switch (this.stateVisible) {
		case INIT_STATE_NULL:
			stringBuilder.append("null");
			break;
		case INIT_STATE_UNINIT:
			stringBuilder.append("uninitialised");
			break;
		case INIT_STATE_INIT:
			stringBuilder.append("initialised");
			break;
		case INIT_STATE_HIDDEN:
			stringBuilder.append("hidden");
			break;
		default:
			stringBuilder.append("unknown");
			break;
		}
		stringBuilder.append("\nData:\n");

		final byte[] data = this.storageVisible;
		if (data != null) {
			for (int i = 0; i < 4096; ++i) {
				final int level = data[i];

				stringBuilder.append(Integer.toHexString(level));
				if ((i & 15) == 15) {
					stringBuilder.append("\n");
				}

				if ((i & 255) == 255) {
					stringBuilder.append("\n");
				}
			}
		} else {
			stringBuilder.append("null");
		}

		return stringBuilder.toString();
	}

	@Override
	public int getArraySize() {
		return SWMRByteArray.ARRAY_SIZE;
	}

	// operation type: updating
	public int getUpdating(final int index) {
		// indices range from 0 -> 4096
		final byte[] bytes = this.storageUpdating;
		if (bytes == null) {
			return 0;
		}
		return bytes[index] & 0xFF;
	}

	@Override
	// operation type: visible
	public int getVisible(final int index) {
		if (this.storageVisible == null) {
			return 0;
		}
		return (int) ToneMapperHelper.tonemap(getFullVisible(index), 15, 15);
	}

	public int getFullVisible(final int index) {
		// indices range from 0 -> 4096
		final byte[] visibleBytes = this.storageVisible;
		if (visibleBytes == null) {
			return 0;
		}
		return visibleBytes[index] & 0xFF;
	}

	@Override
	public void set(final int index, final int value) {
		if (!this.updatingDirty) {
			this.swapUpdatingAndMarkDirty();
		}
		this.storageUpdating[index] = (byte) Math.clamp(value, 0, 255);
	}

	public void add(final int x, final int y, final int z, final int value) {
		this.add((x & 15) | ((z & 15) << 4) | ((y & 15) << 8), value);
	}

	public void add(final int index, int value) {
		if (!this.updatingDirty) {
			this.swapUpdatingAndMarkDirty();
		}
		value += storageUpdating[index] & 0xFF; // cast to int as unsigned byte
		storageUpdating[index] = (byte) Math.clamp(value, 0, 255);
	}
}
