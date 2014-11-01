package mobac.mapsources.mapspace;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import mobac.gui.mapview.PreviewMap;
import mobac.program.interfaces.MapSpace;

public class GeoLatlongPower2MapSpace implements MapSpace {

	protected final int tileSize;
	protected final int[] worldSize;

	protected GeoLatlongPower2MapSpace(int tileSize) {
		this.tileSize = tileSize;
		worldSize = new int[PreviewMap.MAX_ZOOM + 1];
		for (int zoom = 0; zoom < worldSize.length; zoom++)
			worldSize[zoom] = tileSize * (1 << zoom);
	}

	protected double radius(int zoom) {
		return getMaxPixels(zoom) / (2.0 * Math.PI);
	}

	protected int falseNorthing(int aZoomlevel) {
		return (-1 * getMaxPixels(aZoomlevel) / 2);
	}

	@Override
	public ProjectionCategory getProjectionCategory() {
		return ProjectionCategory.SPHERE;
	}

	@Override
	public int getMaxPixels(int zoom) {
		return worldSize[zoom];
	}

	@Override
	public int getTileSize() {
		return tileSize;
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
	public int moveOnLatitude(int startX, int y, int zoom, double angularDist) {
		y += falseNorthing(zoom);
		double lat = -1 * ((Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius(zoom)))));

		double lon = cXToLon(startX, zoom);
		double sinLat = Math.sin(lat);

		lon += Math
				.toDegrees(Math.atan2(Math.sin(angularDist) * Math.cos(lat), Math.cos(angularDist) - sinLat * sinLat));
		int newX = cLonToX(lon, zoom);
		int w = newX - startX;
		return w;
	}

	@Override
	public double horizontalDistance(int zoom, int y, int xDist) {
		y = Math.max(y, 0);
		y = Math.min(y, getMaxPixels(zoom));
		double lat = cYToLat(y, zoom);
		double lon1 = -180.0;
		double lon2 = cXToLon(xDist, zoom);

		double dLon = Math.toRadians(lon2 - lon1);

		double cos_lat = Math.cos(Math.toRadians(lat));
		double sin_dLon_2 = Math.sin(dLon) / 2;

		double a = cos_lat * cos_lat * sin_dLon_2 * sin_dLon_2;
		return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	@Override
	public Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom) {
		int x = xChangeZoom(pixelCoordinate.x, oldZoom, newZoom);
		int y = yChangeZoom(pixelCoordinate.y, oldZoom, newZoom);
		return new Point(x, y);
	}

	@Override
	public int xChangeZoom(int x, int oldZoom, int newZoom) {
		int zoomDiff = oldZoom - newZoom;
		return (zoomDiff > 0) ? x >> zoomDiff : x << -zoomDiff;
	}

	@Override
	public int yChangeZoom(int y, int oldZoom, int newZoom) {
		int zoomDiff = oldZoom - newZoom;
		return (zoomDiff > 0) ? y >> zoomDiff : y << -zoomDiff;
	}

	@Override
	public MapSpaceType getMapSpaceType() {
		return MapSpaceType.msGeoLatlong;
	}

}
