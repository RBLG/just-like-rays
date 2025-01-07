package teluri.mods.jlrays;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

public class JlrLightSectionStorage extends LayerLightSectionStorage<JlrLightSectionStorage.JlrDataLayerStorageMap> {
	protected JlrLightSectionStorage(LightChunkGetter chunkSource) {
		super(LightLayer.BLOCK, chunkSource, new JlrLightSectionStorage.JlrDataLayerStorageMap(new Long2ObjectOpenHashMap<>()));
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
		return dataLayer != null ? dataLayer : new ByteDataLayer();
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
		if (dataLayer instanceof ByteDataLayer) {
			((ByteDataLayer) dataLayer).add(x, y, z, lightLevel);
		} else {
			JustLikeRays.LOGGER.warn("could not do a proper add in DataLayer because it wasnt a ByteSizeLayer");
			dataLayer.set(x, y, z, dataLayer.get(x, y, z) + lightLevel);
		}
		SectionPos.aroundAndAtBlockPos(levelPos, this.sectionsAffectedByLightUpdates::add);
	}

	public int getFullStoredLevel(long levelPos) {
		long l = SectionPos.blockToSection(levelPos);
		DataLayer dataLayer = (ByteDataLayer) this.getDataLayer(l, true);

		int x, y, z;
		x = SectionPos.sectionRelative(BlockPos.getX(levelPos));
		y = SectionPos.sectionRelative(BlockPos.getY(levelPos));
		z = SectionPos.sectionRelative(BlockPos.getZ(levelPos));
		if (dataLayer instanceof ByteDataLayer) {
			return ((ByteDataLayer) dataLayer).getFull(x, y, z);
		} else {
			JustLikeRays.LOGGER.warn("a DataLayer in JlrLightSectionStorage getFullStoredLevel wasnt a ByteSizeLayer");
			return dataLayer.get(x, y, z);
		}
	}

	public static final class JlrDataLayerStorageMap extends DataLayerStorageMap<JlrLightSectionStorage.JlrDataLayerStorageMap> {
		public JlrDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> long2ObjectOpenHashMap) {
			super(long2ObjectOpenHashMap);
		}

		public JlrLightSectionStorage.JlrDataLayerStorageMap copy() {
			return new JlrLightSectionStorage.JlrDataLayerStorageMap(this.map.clone());
		}
	}

	public void assertValidity(long blockPos) {
		long sectionPos = SectionPos.blockToSection(blockPos);
		DataLayer layer = getDataLayer(sectionPos, false);
		if (layer == null) {
			return;
		}
		if (!(layer instanceof ByteDataLayer)) {
			JustLikeRays.LOGGER.error("not cached datalayer isnt byte sized during validity check");
		}
		layer = getDataLayer(sectionPos, true);
		if (layer == null) {
			return;
		}
		if (!(layer instanceof ByteDataLayer)) {
			JustLikeRays.LOGGER.error("cached datalayer isnt byte sized during validity check");
		}
	}
}
