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

package org.hibernate.spatial.dialect;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.spatial.jts.JTS;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public abstract class AbstractJTSGeometryValueExtractor implements ValueExtractor<Geometry> {

	@Override
	public Geometry extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		Object geomObj = rs.getObject(name);
		return toJTS(geomObj);
	}

	protected MGeometryFactory getGeometryFactory() {
		return JTS.getDefaultGeomFactory();
	}

	//Note: access is public because it is also used in test class. Besides it is
	//side-effect free and doesn't use any intermediate state. So public access is safe.

	abstract public Geometry toJTS(Object object);

}
