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
package org.hibernate.spatial;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A {@link org.hibernate.type.BasicType BasicType} for JTS <code>Geometry</code>s.
 *
 * @author Karel Maesen
 */
public class JTSGeometryType extends AbstractSingleColumnStandardBasicType<Geometry> implements Spatial {

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				com.vividsolutions.jts.geom.Geometry.class.getCanonicalName(),
				com.vividsolutions.jts.geom.Point.class.getCanonicalName(),
				com.vividsolutions.jts.geom.Polygon.class.getCanonicalName(),
				com.vividsolutions.jts.geom.MultiPolygon.class.getCanonicalName(),
				com.vividsolutions.jts.geom.LineString.class.getCanonicalName(),
				com.vividsolutions.jts.geom.MultiLineString.class.getCanonicalName(),
				com.vividsolutions.jts.geom.MultiPoint.class.getCanonicalName(),
				com.vividsolutions.jts.geom.GeometryCollection.class.getCanonicalName(),
				"jts_geometry"
		};
	}

	public JTSGeometryType(SqlTypeDescriptor sqlTypeDescriptor) {
		super( sqlTypeDescriptor, JTSGeometryJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "jts_geometry";
	}

}
