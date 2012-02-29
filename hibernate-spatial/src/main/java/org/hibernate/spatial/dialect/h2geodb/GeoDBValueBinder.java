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
import org.hibernate.spatial.Log;
import org.hibernate.spatial.LogFactory;
import org.hibernate.spatial.dialect.AbstractJTSGeometryValueBinder;

import java.sql.Connection;

/**
 * @author Jan Boonen, Geodan IT b.v.
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/29/12
 */
public class GeoDBValueBinder extends AbstractJTSGeometryValueBinder {

	private static Log LOG = LogFactory.make();

	@Override
	protected Object toNative(Geometry jtsGeom, Connection connection) {
		try {
			return WKB.toWKB(jtsGeom);
		} catch (Exception e) {
			LOG.warn("Could not convert JTS Geometry to a database object.");
			e.printStackTrace();
			return null;
		}
	}

}
