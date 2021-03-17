/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.locationtech.jts.geom.Geometry;

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
				org.locationtech.jts.geom.Geometry.class.getCanonicalName(),
				org.locationtech.jts.geom.Point.class.getCanonicalName(),
				org.locationtech.jts.geom.Polygon.class.getCanonicalName(),
				org.locationtech.jts.geom.MultiPolygon.class.getCanonicalName(),
				org.locationtech.jts.geom.LineString.class.getCanonicalName(),
				org.locationtech.jts.geom.MultiLineString.class.getCanonicalName(),
				org.locationtech.jts.geom.MultiPoint.class.getCanonicalName(),
				org.locationtech.jts.geom.GeometryCollection.class.getCanonicalName(),
				"jts_geometry"
		};
	}


	@Override
	public String getName() {
		return "jts_geometry";
	}

}
