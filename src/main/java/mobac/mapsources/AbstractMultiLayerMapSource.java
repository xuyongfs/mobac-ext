/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.mapsources;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlTransient;

import mobac.exceptions.TileException;
import mobac.gui.mapview.PreviewMap;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.interfaces.InitializableMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.TileImageType;

import org.apache.log4j.Logger;

public abstract class AbstractMultiLayerMapSource implements InitializableMapSource, Iterable<MapSource> {

	protected Logger log;

	protected String name = "";
	protected TileImageType tileType = TileImageType.PNG;
	protected MapSource[] mapSources;

	private int maxZoom;
	private int minZoom;
	private MapSpace mapSpace;
	protected MapSourceLoaderInfo loaderInfo = null;

	protected boolean forceMercator = false;
	protected boolean unionAllZoom = false;

	public AbstractMultiLayerMapSource(String name, TileImageType tileImageType) {
		this();
		this.name = name;
		this.tileType = tileImageType;
	}

	protected AbstractMultiLayerMapSource() {
		log = Logger.getLogger(this.getClass());
	}

	private void initializeZoom() {
		if (unionAllZoom) {
			maxZoom = 0;
			minZoom = PreviewMap.MAX_ZOOM;
			for (MapSource ms : mapSources) {
				maxZoom = Math.max(maxZoom, ms.getMaxZoom());
				minZoom = Math.min(minZoom, ms.getMinZoom());
			}
		} else {
			maxZoom = PreviewMap.MAX_ZOOM;
			minZoom = 0;
			for (MapSource ms : mapSources) {
				maxZoom = Math.min(maxZoom, ms.getMaxZoom());
				minZoom = Math.max(minZoom, ms.getMinZoom());
				//if (!forceMercator && !ms.getMapSpace().equals(mapSpace))
				//	throw new RuntimeException("Different map spaces used in multi-layer map source");
			}
		}
	}

	protected void initializeValues() {
		MapSource refMapSource = mapSources[0];
		mapSpace = forceMercator ? MercatorPower2MapSpace.INSTANCE_256 : refMapSource.getMapSpace();
		initializeZoom();
	}

	@Override
	public void initialize() {
		MapSource refMapSource = mapSources[0];
		mapSpace = forceMercator ? MercatorPower2MapSpace.INSTANCE_256 : refMapSource.getMapSpace();
		initializeZoom();
		for (MapSource ms : mapSources) {
			if (ms instanceof InitializableMapSource)
				((InitializableMapSource) ms).initialize();
		}
	}

	public MapSource[] getLayerMapSources() {
		return mapSources;
	}

	public Color getBackgroundColor() {
		return Color.BLACK;
	}

	public MapSpace getMapSpace() {
		return mapSpace;
	}

	public int getMaxZoom() {
		return maxZoom;
	}

	public int getMinZoom() {
		return minZoom;
	}

	public String getName() {
		return name;
	}

	public String getStoreName() {
		return null;
	}

	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, InterruptedException,
			TileException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		BufferedImage image = getTileImage(zoom, x, y, loadMethod);
		if (image == null)
			return null;
		ImageIO.write(image, tileType.getFileExt(), buf);
		return buf.toByteArray();
	}

	public BufferedImage getTileImage(MapSource layerMapSource, int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException {
		BufferedImage image = null;
		if (layerMapSource.getMapSpace().getMapSpaceType() == mapSpace.getMapSpaceType())
			image = layerMapSource.getTileImage(zoom, x, y, loadMethod);
		else {
			int tileSize = mapSpace.getTileSize();
			int pixelx1 = x * tileSize;
			int pixely1 = y * tileSize;
			Point2D.Double pd1 = mapSpace.cXYToLonLat(pixelx1, pixely1, zoom);
			Point2D.Double pd2 = mapSpace.cXYToLonLat(pixelx1 + tileSize - 1, pixely1 + tileSize - 1, zoom);

			Point p1 = layerMapSource.getMapSpace().cLonLatToXY(pd1.x, pd1.y, zoom);
			Point p2 = layerMapSource.getMapSpace().cLonLatToXY(pd2.x, pd2.y, zoom);
			int tileSizeSrc = layerMapSource.getMapSpace().getTileSize();
			int tilex1 = p1.x / tileSizeSrc;
			int tiley1 = p1.y / tileSizeSrc;
			int tilex2 = p2.x / tileSizeSrc;
			int tiley2 = p2.y / tileSizeSrc;

			BufferedImage imgSrc = new BufferedImage(p2.x - p1.x + 1, p2.y - p1.y + 1, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2 = imgSrc.createGraphics();
			try {
				//int dx = (p1.x - pixelx1) % tileSizeSrc;
				//int dy = (p1.y - pixely1) % tileSizeSrc;
				//int drawx = (dx >= 0) ? - dx : - (tileSizeSrc + dx) ;
				int drawx = (tilex1 * tileSizeSrc) - p1.x;
				boolean matchTile = false;
				IOException tileException = null;
				for (int i = tilex1; i <= tilex2; i++) {
					//int drawy = (dy >= 0) ? - dy : - (tileSizeSrc + dy);
					int drawy = (tiley1 * tileSizeSrc) - p1.y;
					for (int j = tiley1; j <= tiley2; j++) {
						BufferedImage img;
						try {
							img = layerMapSource.getTileImage(zoom, i, j, loadMethod);
							g2.drawImage(img, drawx, drawy, null);
							matchTile = true;
						} catch (IOException e) {
							tileException = e;
							img = null;
						}
						drawy += tileSizeSrc;
					}
					drawx += tileSizeSrc;
				}
				if (!matchTile && tileException != null)
					throw tileException;
			} finally {
				g2.dispose();
			}
			image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_4BYTE_ABGR);
			g2 = image.createGraphics();
			try {
				g2.drawImage(imgSrc, 0, 0, tileSize, tileSize, null);
			} finally {
				g2.dispose();
			}

		}
		return image;
	}

	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException,
			InterruptedException, TileException {
		BufferedImage image = null;
		Graphics2D g2 = null;
		try {
			ArrayList<BufferedImage> layerImages = new ArrayList<BufferedImage>(mapSources.length);
			int maxSize = mapSpace.getTileSize();
			for (int i = 0; i < mapSources.length; i++) {
				MapSource layerMapSource = mapSources[i];
				if (zoom < layerMapSource.getMinZoom() || zoom > layerMapSource.getMaxZoom())
					continue;
				//BufferedImage layerImage = layerMapSource.getTileImage(zoom, x, y, loadMethod);
				BufferedImage layerImage = getTileImage(layerMapSource, zoom, x, y, loadMethod);
				if (layerImage != null) {
					log.debug("Multi layer loading: " + layerMapSource + " " + x + " " + y + " " + zoom);
					layerImages.add(layerImage);
					int size = layerImage.getWidth();
					if (size > maxSize) {
						maxSize = size;
					}
				}
			}

			image = new BufferedImage(maxSize, maxSize, BufferedImage.TYPE_3BYTE_BGR);
			g2 = image.createGraphics();
			g2.setColor(getBackgroundColor());
			g2.fillRect(0, 0, maxSize, maxSize);

			for (int i = 0; i < layerImages.size(); i++) {
				BufferedImage layerImage = layerImages.get(i);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getLayerAlpha(i)));
				g2.drawImage(layerImage, 0, 0, maxSize, maxSize, null);
			}
			return image;
		} finally {
			if (g2 != null) {
				g2.dispose();
			}

		}
	}

	protected float getLayerAlpha(int layerIndex) {
		return 1.0f;
	}

	public TileImageType getTileImageType() {
		return tileType;
	}

	@Override
	public String toString() {
		return getName();
	}

	public Iterator<MapSource> iterator() {
		return Arrays.asList(mapSources).iterator();
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo() {
		return loaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo) {
		if (this.loaderInfo != null)
			throw new RuntimeException("LoaderInfo already set");
		this.loaderInfo = loaderInfo;
	}

	@Override
	public boolean getHiddenDefault() {
		return false;
	}
}
