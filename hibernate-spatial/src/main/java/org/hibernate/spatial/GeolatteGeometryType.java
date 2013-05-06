/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for spatial (geographic) data.
 *
 * Copyright © 2007-2013 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryCollection;
import org.geolatte.geom.LineString;
import org.geolatte.geom.MultiLineString;
import org.geolatte.geom.MultiPoint;
import org.geolatte.geom.MultiPolygon;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * a {@code Type} that maps between the database geometry type and geolatte-geom {@code Geometry}.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/12/12
 */
public class GeolatteGeometryType extends AbstractSingleColumnStandardBasicType<Geometry> implements Spatial {

	/**
	 * Constructs an instance with the specified {@code SqlTypeDescriptor}
	 *
	 * @param sqlTypeDescriptor The Descriptor for the type used by the database for geometries.
	 */
	public GeolatteGeometryType(SqlTypeDescriptor sqlTypeDescriptor) {
		super( sqlTypeDescriptor, GeolatteGeometryJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				Geometry.class.getCanonicalName(),
				Point.class.getCanonicalName(),
				Polygon.class.getCanonicalName(),
				MultiPolygon.class.getCanonicalName(),
				LineString.class.getCanonicalName(),
				MultiLineString.class.getCanonicalName(),
				MultiPoint.class.getCanonicalName(),
				GeometryCollection.class.getCanonicalName(),
				"geolatte_geometry"
		};
	}

	@Override
	public String getName() {
		return "geolatte_geometry";
	}
}
