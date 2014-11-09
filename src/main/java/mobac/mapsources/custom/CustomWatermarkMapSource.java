package mobac.mapsources.custom;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;

import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.mapsources.mapspace.MapSpaceFactory;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.interfaces.MapSpace.MapSpaceType;
import mobac.program.jaxb.ColorAdapter;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;
import mobac.utilities.Utilities;

@XmlRootElement(name = "watermark")
public class CustomWatermarkMapSource implements FileBasedMapSource {

	private static final Logger log = Logger.getLogger(CustomWatermarkMapSource.class);

	private MapSpace mapSpace = MapSpaceFactory.getInstance(256, MapSpaceType.msMercatorSpherical);

	private boolean initialized = false;

	private TileImageType tileImageType = null;

	private MapSourceLoaderInfo loaderInfo = null;

	private byte[] mosaicMask = null;

	@XmlElement(nillable = false, defaultValue = "Watermark")
	private String name = "Watermark";

	@XmlElement(defaultValue = "0")
	private int minZoom = PreviewMap.MIN_ZOOM;

	@XmlElement(defaultValue = "22")
	private int maxZoom = PreviewMap.MAX_ZOOM;

	@XmlElement(required = true)
	private File watermarkFile = null;

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	@XmlElement(defaultValue = "100")
	private int probability = 100;
	
	@XmlElement
	private String mosaic = null;
	
	public CustomWatermarkMapSource() {
		super();
	}

	@Override
	public void initialize() {
		if (initialized)
			return;
		reinitialize();
	}

	@Override
	public int getMaxZoom() {
		return maxZoom;
	}

	@Override
	public int getMinZoom() {
		return minZoom;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod)
			throws IOException, TileException, InterruptedException {
		if (!initialized)
			initialize();

		if (mosaicMask != null) {
			byte mask = mosaicMask[y % 8];
			if ((mask & (1 << (x % 8))) == 0)
				return null;
		} else {
			Random random = new Random();
			if (random.nextInt(100) + 1 > probability)
				return null;
		}

		try {
			return Utilities.getFileBytes(watermarkFile);
		} catch (FileNotFoundException e) {
			log.debug("Map tile file not found: " + watermarkFile.getAbsolutePath());
			return null;
		}
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y,
			LoadMethod loadMethod) throws IOException, TileException,
			InterruptedException {
		byte[] data = getTileData(zoom, x, y, loadMethod);
		if (data == null)
			return null;
		return ImageIO.read(new ByteArrayInputStream(data));
	}

	@Override
	public TileImageType getTileImageType() {
		return tileImageType;
	}

	@Override
	public MapSpace getMapSpace() {
		return mapSpace;
	}

	@Override
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		this.loaderInfo = loaderInfo;
	}

	@Override
	public void reinitialize() {
		initialized = true;
		if (!watermarkFile.exists())
			throw new RuntimeException("watermarkFile file does not exist: " + watermarkFile.getAbsolutePath());
		String fileName = watermarkFile.getName();
		int indexPeriod = fileName.lastIndexOf(".");
		String fileExt = (indexPeriod == -1 ? "" : fileName.substring(indexPeriod + 1).toLowerCase());
		tileImageType = TileImageType.getTileImageType(fileExt);
		if (mosaic != null) {
			mosaicMask = new byte[8];
			String[] masks = mosaic.split(",");
			for (int i = 0; i < masks.length && i < mosaicMask.length; i++) {
				char[] mask = masks[i].toCharArray();
				mosaicMask[i] = 0;
				for (int j = 0; j < mask.length && j < 8; j++)
					if (mask[j] == '1')
						mosaicMask[i] |= 1 << j;
			}
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
