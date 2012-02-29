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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The type of geometry.
 *
 * @author Karel Maesen, Geovise BVBA.
 *         Date: Nov 2, 2009
 */
public enum OpenGisType {
	POINT( (byte) 1, Point.class ),
	LINESTRING( (byte) 2, LineString.class ),
	POLYGON( (byte) 3, Polygon.class ),
	MULTIPOINT( (byte) 4, MultiPoint.class ),
	MULTILINESTRING( (byte) 5, MultiLineString.class ),
	MULTIPOLYGON( (byte) 6, MultiPolygon.class ),
	GEOMETRYCOLLECTION( (byte) 7, GeometryCollection.class ),
	INVALID_TYPE( (byte) 0, null );

	final byte byteValue;
	final Class geomClass;

	OpenGisType(byte v, Class geomClass) {
		this.byteValue = v;
		this.geomClass = geomClass;
	}

	boolean typeOf(Object o) {
		return geomClass.isAssignableFrom( o.getClass() );
	}

	static OpenGisType valueOf(byte b) {
		for ( OpenGisType t : values() ) {
			if ( t.byteValue == b ) {
				return t;
			}
		}
		return INVALID_TYPE;
	}

}
