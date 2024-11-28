package teluri.mods.jlrays.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.Util;
import net.minecraft.world.level.chunk.DataLayer;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.mixed.IBetterDataLayer;

@Mixin(DataLayer.class)
public abstract class DataLayerMixin implements IBetterDataLayer {
	private int size = 512 * 4;

	@Shadow
	protected byte[] data;
	@Shadow
	private int defaultValue;

	@Shadow
	public abstract byte[] getData();

	@Override
	public void setSize(int nsize) {
		if (this.data != null && this.data.length != nsize) {
			JustLikeRays.LOGGER.error("size set while data was already existing and sized");
		}
		size = nsize;
	}

	@Override
	public int getSize() {
		return this.data == null ? size : this.data.length;
	}

	@ModifyConstant(method = "<init>([B)V", constant = @Constant(intValue = 2048)) // TODO use BeforeConstant + ModifyExpressionValue
	public int InitMixin(int val, byte[] data) {
		if (data == null) {
			JustLikeRays.LOGGER.error("data in DataLayer was null");
		} else if (data.length != 2048 && data.length != BYTE_SIZED) {
			String err = "DataLayer isnt a handled length: " + data.length;
			JustLikeRays.LOGGER.error(err);
			throw (IllegalArgumentException) Util.pauseInIde(new IllegalArgumentException(err));
		}
		size = data.length;

		return data.length;
	}

	@Inject(method = "getData()[B", at = @At(value = "HEAD"), cancellable = true)
	public void getDataMixin(CallbackInfoReturnable<byte[]> info) {
		if (this.data == null && size == BYTE_SIZED) {
			this.data = new byte[BYTE_SIZED];
			if (this.defaultValue != 0) {
				Arrays.fill(this.data, (byte) defaultValue);
			}
			info.setReturnValue(this.data);
		}

	}

	@Inject(method = "get(I)I", at = @At(value = "HEAD"), cancellable = true)
	private void getMixin(int index, CallbackInfoReturnable<Integer> info) {
		if (this.data != null && this.data.length == BYTE_SIZED) {
			info.setReturnValue((int) data[index]);
		}
	}

	@Inject(method = "set(II)V", at = @At(value = "HEAD"), cancellable = true)
	private void setMixin(int index, int value, CallbackInfo info) {
		if (isByteSized()) {
			byte[] bs = this.getData();
			bs[index] = (byte) value;
			info.cancel();
		}
	}

	@Inject(method = "copy()Lnet/minecraft/world/level/chunk/DataLayer;", at = @At(value = "TAIL"))
	public void copyMixin(CallbackInfoReturnable<DataLayer> info) {
		DataLayer dataLayer = info.getReturnValue();
		((IBetterDataLayer) dataLayer).setSize(this.getSize());
	}

}
