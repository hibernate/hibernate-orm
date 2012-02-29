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

package org.hibernate.spatial.dialect.mysql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import org.hibernate.spatial.dialect.AbstractJTSGeometryValueExtractor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public class MySQLGeometryValueExtractor extends AbstractJTSGeometryValueExtractor {

	private static final int SRIDLEN = 4;

	/**
	 * Converts the native geometry object to a JTS <code>Geometry</code>.
	 *
	 * @param object native database geometry object (depends on the JDBC spatial
	 *               extension of the database)
	 * @return JTS geometry corresponding to geomObj.
	 */
	public Geometry toJTS(Object object) {
		if (object == null) {
			return null;
		}
		byte[] data = (byte[]) object;
		byte[] wkb = new byte[data.length - SRIDLEN];
		System.arraycopy(data, SRIDLEN, wkb, 0, wkb.length);
		int srid = 0;
		// WKB in MySQL Spatial is always little endian.
		srid = data[3] << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8
				| (data[0] & 0xff);
		Geometry geom = null;
		try {
			WKBReader reader = new WKBReader();
			geom = reader.read(wkb);
		} catch (Exception e) {
			throw new RuntimeException(
					"Couldn't parse incoming MySQL Spatial data.");
		}
		geom.setSRID(srid);
		return geom;
	}

}
