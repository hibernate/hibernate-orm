/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects.mysql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.testing.GeometryEquality;

/**
 * Extends the test for geometry equality, because
 * MySQL stores empty geometries as NULL objects.
 */
public class MySQLGeometryEquality extends GeometryEquality {

	@Override
	public boolean test(Geometry geom1, Geometry geom2) {
		if ( geom1 != null && geom1.isEmpty() ) {
			return geom2 == null || geom2.isEmpty();
		}
		return super.test( geom1, geom2 );
	}

	@Override
	protected boolean testSimpleGeometryEquality(Geometry geom1, Geometry geom2) {
		return testVerticesEquality( geom1, geom2 );
	}

	private boolean testVerticesEquality(Geometry geom1, Geometry geom2) {
		if ( geom1.getNumPoints() != geom2.getNumPoints() ) {
			return false;
		}
		for ( int i = 0; i < geom1.getNumPoints(); i++ ) {
			Coordinate cn1 = geom1.getCoordinates()[i];
			Coordinate cn2 = geom2.getCoordinates()[i];
			if ( !cn1.equals2D( cn2 ) ) {
				return false;
			}
		}
		return true;
	}
}
