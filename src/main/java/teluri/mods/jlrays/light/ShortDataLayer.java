package teluri.mods.jlrays.light;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import teluri.mods.jlrays.config.IDepthHandler;

/**
 * DataLayer implementation for short sized light level
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class ShortDataLayer extends DynamicDataLayer {
	private static VarHandle DATA_VARHANDLE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.nativeOrder()).withInvokeExactBehavior();

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

	@Override
	public ShortDataLayer copy() {
		return this.data == null ? new ShortDataLayer(this.defaultValue) : new ShortDataLayer((byte[]) this.data.clone());
	}

	@Override
	public int getDyn(int index) {
		short value = (short) DATA_VARHANDLE.get(data, index * 2);
		return (int) value & 0xFFFF;
	}

	@Override
	public void setDyn(int index, int value) {
		DATA_VARHANDLE.set(data, index * 2, (short) Math.clamp(value, 0, 0xFFFF));
	}

	public static class ShortDataLayerFactory implements IDepthHandler {

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
