/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.hibernate.spatial.jts;

import com.vividsolutions.jts.geom.*;

public class EnvelopeAdapter {

	static private GeometryFactory geomFactory = new GeometryFactory();

	static public Polygon toPolygon(Envelope env, int SRID) {
		Coordinate[] coords = new Coordinate[5];

		coords[0] = new Coordinate(env.getMinX(), env.getMinY());
		coords[1] = new Coordinate(env.getMinX(), env.getMaxY());
		coords[2] = new Coordinate(env.getMaxX(), env.getMaxY());
		coords[3] = new Coordinate(env.getMaxX(), env.getMinY());
		coords[4] = new Coordinate(env.getMinX(), env.getMinY());
		LinearRing shell = geomFactory.createLinearRing(coords);

		Polygon pg = geomFactory.createPolygon(shell, null);
		pg.setSRID(SRID);
		return pg;
	}

	public static void setGeometryFactory(GeometryFactory gf) {
		geomFactory = gf;
	}

}
