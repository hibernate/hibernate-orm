/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
