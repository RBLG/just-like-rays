package teluri.mods.jlrays.light;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.JlrConfig;

/**
 * implementation of LayerLightSectionStorage to make use of the new fancy effects of ByteDataLayer to be used by the custom light engine
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class JlrLightSectionStorage extends LayerLightSectionStorage<JlrLightSectionStorage.JlrDataLayerStorageMap> {
	protected JlrLightSectionStorage(LightChunkGetter chunkSource) {
		super(LightLayer.BLOCK, chunkSource, new JlrDataLayerStorageMap(new Long2ObjectOpenHashMap<>()));
	}

	@Override
	protected int getLightValue(long levelPos) {
		long l = SectionPos.blockToSection(levelPos);
		DataLayer dataLayer = this.getDataLayer(l, false);
		int x, y, z;
		x = SectionPos.sectionRelative(BlockPos.getX(levelPos));
		y = SectionPos.sectionRelative(BlockPos.getY(levelPos));
		z = SectionPos.sectionRelative(BlockPos.getZ(levelPos));
		// return dataLayer == null ? 0 : Math.clamp(dataLayer.get(x, y, z), 0, 15);
		return dataLayer == null ? 0 : dataLayer.get(x, y, z);
		// return dataLayer == null ? 0 : cheapTonemap(dataLayer.get(x, y, z) >>> 1);
		// return dataLayer == null ? 0 : (int) extendedTonemap(dataLayer.get(x, y, z) >>> 1, 255, 1);
	}

	@Override
	protected DataLayer createDataLayer(long sectionPos) {
		DataLayer dataLayer = this.queuedSections.get(sectionPos);
		return dataLayer != null ? dataLayer : JlrConfig.LazyGet().depthHandler.createDataLayer();
	}

	public void addStoredLevel(long levelPos, int lightLevel) {
		long l = SectionPos.blockToSection(levelPos);
		DataLayer dataLayer;
		if (this.changedSections.add(l)) {
			dataLayer = this.updatingSectionData.copyDataLayer(l);
		} else {
			dataLayer = this.getDataLayer(l, true);
		}
		int x, y, z;
		x = SectionPos.sectionRelative(BlockPos.getX(levelPos));
		y = SectionPos.sectionRelative(BlockPos.getY(levelPos));
		z = SectionPos.sectionRelative(BlockPos.getZ(levelPos));
		if (dataLayer instanceof DynamicDataLayer) {
			((DynamicDataLayer) dataLayer).add(x, y, z, lightLevel);
		} else {
			JustLikeRays.LOGGER.warn("could not do a proper add in DataLayer because it wasnt byte sized");
		}
		notifyUpdate(BlockPos.getX(levelPos), BlockPos.getY(levelPos), BlockPos.getZ(levelPos));
	}

	public void notifyUpdate(int x, int y, int z) { // TODO remove synchronized perf hog?
		this.syncUsing(() -> {
			SectionPos.aroundAndAtBlockPos(x, y, z, this.sectionsAffectedByLightUpdates::add);
		});
	}

	/**
	 * unsynchronized way to add a section to the affected sections set
	 */
	public void notifySingleSectionUpdate(int x, int y, int z) {
		this.sectionsAffectedByLightUpdates.add(SectionPos.asLong(x, y, z));
	}

	/**
	 * trick to get a sync access to unsync methods
	 * 
	 * @param action
	 */
	public synchronized void syncUsing(Runnable action) {
		action.run();
	}

	/**
	 * get the full light level without tonemapping
	 * 
	 * @param levelPos
	 * @return
	 */
	public int getFullStoredLevel(long levelPos) {
		long l = SectionPos.blockToSection(levelPos);
		DataLayer dataLayer = (DynamicDataLayer) this.getDataLayer(l, true);

		int x, y, z;
		x = SectionPos.sectionRelative(BlockPos.getX(levelPos));
		y = SectionPos.sectionRelative(BlockPos.getY(levelPos));
		z = SectionPos.sectionRelative(BlockPos.getZ(levelPos));
		if (dataLayer instanceof DynamicDataLayer) {
			return ((DynamicDataLayer) dataLayer).getFull(x, y, z);
		} else {
			JustLikeRays.LOGGER.warn("a DataLayer in JlrLightSectionStorage getFullStoredLevel wasnt byte sized");
			return dataLayer.get(x, y, z);
		}
	}

	/**
	 * get data layer of a section coordinates while doing vanilla thingamagig so it should work like vanilla
	 */
	public synchronized DynamicDataLayer getDataLayerForCaching(int x, int y, int z) {
		long l = SectionPos.asLong(x, y, z);
		if (!this.storingLightForSection(l)) {
			return null;
		}
		DataLayer dataLayer;
		if (this.changedSections.add(l)) {
			dataLayer = this.updatingSectionData.copyDataLayer(l);
		} else {
			dataLayer = this.getDataLayer(l, true);
		}
		if (dataLayer instanceof DynamicDataLayer) {
			return (DynamicDataLayer) dataLayer;
		} else {
			JustLikeRays.LOGGER.warn("a DataLayer in JlrLightSectionStorage getDataLayerForCaching wasnt byte sized");
			return null;
		}
	}

	/**
	 * check that a dataLayer at a position is a ByteDataLayer
	 * 
	 * @param blockPos
	 */
	public void assertValidity(long blockPos) {
		long sectionPos = SectionPos.blockToSection(blockPos);
		DataLayer layer = getDataLayer(sectionPos, false);
		if (layer == null) {
			return;
		}
		if (!(layer instanceof DynamicDataLayer)) {
			JustLikeRays.LOGGER.error("not cached datalayer isnt byte sized during validity check");
		}
		layer = getDataLayer(sectionPos, true);
		if (layer == null) {
			return;
		}
		if (!(layer instanceof DynamicDataLayer)) {
			JustLikeRays.LOGGER.error("cached datalayer isnt byte sized during validity check");
		}
	}

	public static final class JlrDataLayerStorageMap extends DataLayerStorageMap<JlrDataLayerStorageMap> {
		public JlrDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> long2ObjectOpenHashMap) {
			super(long2ObjectOpenHashMap);
		}

		public JlrLightSectionStorage.JlrDataLayerStorageMap copy() {
			return new JlrLightSectionStorage.JlrDataLayerStorageMap(this.map.clone());
		}

		/**
		 * stateless version of getLayer
		 */
		@Override
		@Nullable
		public DataLayer getLayer(long sectionPos) {
			return this.map.get(sectionPos);
		}
	}

}
