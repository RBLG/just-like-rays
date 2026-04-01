package teluri.mods.jlrays.light;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import teluri.mods.jlrays.config.IDepthHandler;

/**
 * DataLayer implementation for integer sized light level
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class IntDataLayer extends DynamicDataLayer {
	private static VarHandle DATA_VARHANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder()).withInvokeExactBehavior();

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

	@Override
	public IntDataLayer copy() {
		return this.data == null ? new IntDataLayer(this.defaultValue) : new IntDataLayer((byte[]) this.data.clone());
	}

	@Override
	public int getDyn(int index) {
		return (int) DATA_VARHANDLE.get(data, index);
	}

	@Override
	public void setDyn(int index, int value) {
		DATA_VARHANDLE.set(data, index, value);
	}

	public static class IntDataLayerFactory implements IDepthHandler {

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
