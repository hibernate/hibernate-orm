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

package org.hibernate.spatial.dialect.sqlserver;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.dialect.AbstractJTSGeometryValueExtractor;
import org.hibernate.spatial.dialect.sqlserver.convertors.Decoders;

import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/23/11
 */
public class SqlServer2008GeometryValueExtractor extends AbstractJTSGeometryValueExtractor {

	public Geometry toJTS(Object obj) {
		byte[] raw = null;
		if (obj == null) {
			return null;
		}
		if ((obj instanceof byte[])) {
			raw = (byte[]) obj;
		} else if (obj instanceof Blob) {
			raw = toByteArray((Blob) obj);
		} else {
			throw new IllegalArgumentException("Expected byte array.");
		}
		return Decoders.decode(raw);
	}

	private byte[] toByteArray(Blob blob) {
		try {
			return blob.getBytes(1, (int) blob.length());
		} catch (SQLException e) {
			throw new RuntimeException("Error on transforming blob into array.", e);
		}
	}

}
