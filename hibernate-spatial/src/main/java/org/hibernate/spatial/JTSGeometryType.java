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

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A {@code Type} that maps between the database geometry type and JTS {@code Geometry}.
 *
 * @author Karel Maesen
 */
public class JTSGeometryType extends AbstractSingleColumnStandardBasicType<Geometry> implements Spatial {

	/**
	 * Constructs an instance with the specified {@code SqlTypeDescriptor}
	 *
	 * @param sqlTypeDescriptor The descriptor for the type used by the database for geometries.
	 */
	public JTSGeometryType(SqlTypeDescriptor sqlTypeDescriptor) {
		super( sqlTypeDescriptor, JTSGeometryJavaTypeDescriptor.INSTANCE );
	}

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


	@Override
	public String getName() {
		return "jts_geometry";
	}

}
