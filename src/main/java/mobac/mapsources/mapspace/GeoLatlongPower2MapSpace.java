package mobac.mapsources.mapspace;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class GeoLatlongPower2MapSpace extends MercatorPower2MapSpace {

	protected GeoLatlongPower2MapSpace(int tileSize) {
		super(tileSize);
	}

	@Override
	public int cLonToX(double lon, int zoom) {
		int mp = getMaxPixels(zoom);
		int x = (int) ((mp * (lon + 180l)) / 360l);
		x = Math.min(x, mp - 1);
		return x;
	}

	@Override
	public int cLatToY(double lat, int zoom) {
		int mp = getMaxPixels(zoom) / 2;
		int y = (int) ((mp * (-lat + 90l)) / 180l);
		y = Math.min(y, mp - 1);
		return y;
	}

	@Override
	public double cXToLon(int x, int zoom) {
		return ((360d * x) / getMaxPixels(zoom)) - 180.0;
	}

	@Override
	public double cYToLat(int y, int zoom) {
		int mp = getMaxPixels(zoom) / 2;
		y = Math.min(y, mp - 1);
		return 90.0 - ((180d * y) / (getMaxPixels(zoom) / 2));
	}

	@Override
	public Point cLonLatToXY(double lon, double lat, int zoom) {
		Point p = new Point();
		p.x = cLonToX(lon, zoom);
		p.y = cLatToY(lat, zoom);
		return p;
	}

	@Override
	public Double cXYToLonLat(int x, int y, int zoom) {
		Point2D.Double p = new Point2D.Double();
		p.x = cXToLon(x, zoom);
		p.y = cYToLat(y, zoom);
		return p;
	}

	@Override
	public MapSpaceType getMapSpaceType() {
		return MapSpaceType.msGeoLatlong;
	}

}
