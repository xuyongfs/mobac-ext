/*******************************************************************************
 * Copyright (c) MOBAC developers (by Randolph)
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

package mobac.mapsources.mapspace;

import java.awt.geom.Point2D;

/**
 * WGS-84到GCJ-02的转换（即 GPS 加偏）算法
 * 此算法不可逆，严格来说GCJ-02到WGS-84的坐标转换需要使用二分法作逼近计算
 * 在精度要求不高的情况下可以把GCJ-02坐标再次加偏获得的新坐标与原坐标偏移值作为WGS-84坐标与GCJ-02的差值，误差应该在1米以内
 *
 */

public class CoorConvertGCJ02 {

	private static final double pi = 3.14159265358979324;

    //
    // Krasovsky 1940
    //
    // a = 6378245.0, 1/f = 298.3
    // b = a * (1 - f)
    // ee = (a^2 - b^2) / a^2;
	private static final double a = 6378245.0;
	private static final double ee = 0.00669342162296594323;

    //WGS-84 半长轴6378137米 扁率1/298.25722
    //static double a = 6378137.0;
    //static double ee = 0.006694380069978526;
    
	public static double lon2meter(double lon, double lat)
    {
		return 2 * pi * a * Math.cos(pi / 180 * lat) * lon / 360;
    }

	public static double lat2meter(double lat)
    {
    	return 2 * pi * a * lat / 360;
    }

	public static double meter2lon(double mLon, double lat)
    {
		return mLon * 360 / (2 * pi * a * Math.cos(pi / 180 * lat));
    }

	public static double meter2lat(double mLat)
    {
		return mLat * 360 / (2 * pi * a);
    }

	//
    // World Geodetic System ==> Mars Geodetic System
	private static Point2D.Double transform(double wgLon, double wgLat)
    {
		Point2D.Double p = new Point2D.Double();
        if (outOfChina(wgLon, wgLat))
        {
        	p.x = wgLon;
        	p.y = wgLat;
            return p;
        }
        double dLat = transformLat(wgLon - 105.0, wgLat - 35.0);
        double dLon = transformLon(wgLon - 105.0, wgLat - 35.0);
        double radLat = wgLat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
    	p.x = wgLon + dLon;
    	p.y = wgLat + dLat;
    	return p;
    }

	private static boolean outOfChina(double lon, double lat)
    {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

	private static double transformLat(double x, double y)
    {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

	private static double transformLon(double x, double y)
    {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }


    public static Point2D.Double toGCJ02(double x, double y) {
    	Point2D.Double p = transform(x, y);
    	return p;
    }

    public static Point2D.Double fromGCJ02(double x, double y) {
    	Point2D.Double p = transform(x, y);
    	p.x = x - (p.getX() - x);
    	p.y = y - (p.getY() - y);
    	return p;
    }

}
