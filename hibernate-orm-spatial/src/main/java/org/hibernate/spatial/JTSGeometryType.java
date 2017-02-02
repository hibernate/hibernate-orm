/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
