/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA, Geodan IT b.v.
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

package org.hibernate.spatial.dialect.h2geodb;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * A utility class to serialize from/to GeoDB WKB's.
 * <p/>
 * <p>Note: this utility makes it unnecessary to have a dependency on GeoDB. As long as GeoDB is
 * not available in common maven repositories, such a dependency is to be avoided.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/29/12
 */
class WKB {

	static Geometry fromWKB(byte[] bytes, GeometryFactory factory) throws ParseException {
		WKBReader reader = new WKBReader(factory);
		return reader.read(bytes);
	}

	/**
	 * Reads a EWKB byte (which is just a WKB prepended with an envelope of 32 bytes.
	 *
	 * @param bytes
	 * @param factory
	 * @return
	 * @throws ParseException
	 */
	static Geometry fromEWKB(byte[] bytes, GeometryFactory factory) throws ParseException {
		byte[] wkbBytes = new byte[bytes.length - 32];
		System.arraycopy(bytes, 32, wkbBytes, 0, bytes.length - 32);
		return fromWKB(wkbBytes, factory);
	}


	static byte[] toWKB(Geometry jtsGeom) {
		WKBWriter writer = new WKBWriter(3, true);
		return writer.write(jtsGeom);
	}
}

