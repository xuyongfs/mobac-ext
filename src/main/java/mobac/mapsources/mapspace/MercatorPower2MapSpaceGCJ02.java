package mobac.mapsources.mapspace;

import java.awt.Point;
import java.awt.geom.Point2D;

public class MercatorPower2MapSpaceGCJ02 extends MercatorPower2MapSpace {

	protected MercatorPower2MapSpaceGCJ02(int tileSize) {
		super(tileSize);
	}

	@Override
	public Point cLonLatToXY(double lon, double lat, int zoom) {
		Point2D.Double p = CoorConvertGCJ02.toGCJ02(lon, lat);
		return super.cLonLatToXY(p.x, p.y, zoom);
	}

	@Override
	public Point2D.Double cXYToLonLat(int x, int y, int zoom) {
		Point2D.Double p = super.cXYToLonLat(x, y, zoom);
		p = CoorConvertGCJ02.fromGCJ02(p.x, p.y);
		return p;
	}

	@Override
	public MapSpaceType getMapSpaceType() {
		return MapSpaceType.msMercatorGCJ02;
	}

}
