/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * This class tests for the equality between geometries.
 * <p/>
 * The notion of geometric equality can differ slightly between
 * spatial databases.
 *
 * @deprecated Should be replaced by Geolatte-geom GeometryEquality
 */
@Deprecated
public class GeometryEquality {

	public boolean test(Geometry geom1, Geometry geom2) {
		return test( geom1, geom2, false );
	}

	private boolean test(Geometry geom1, Geometry geom2, boolean ignoreSRID) {
		if ( geom1 == null ) {
			return geom2 == null;
		}
		if ( geom1.isEmpty() ) {
			return geom2.isEmpty();
		}
		if ( !ignoreSRID && !equalSRID( geom1, geom2 ) ) {
			return false;
		}

		if ( geom1 instanceof GeometryCollection ) {
			if ( !(geom2 instanceof GeometryCollection) ) {
				return false;
			}
			GeometryCollection expectedCollection = (GeometryCollection) geom1;
			GeometryCollection receivedCollection = (GeometryCollection) geom2;
			for ( int partIndex = 0; partIndex < expectedCollection.getNumGeometries(); partIndex++ ) {
				Geometry partExpected = expectedCollection.getGeometryN( partIndex );
				Geometry partReceived = receivedCollection.getGeometryN( partIndex );
				if ( !test( partExpected, partReceived, true ) ) {
					return false;
				}
			}
			return true;
		}
		else {
			return testSimpleGeometryEquality( geom1, geom2 );
		}
	}

	/**
	 * Two geometries are equal iff both have the same SRID, or both are unknown (i.e. a SRID of 0 or -1).
	 *
	 * @param geom1
	 * @param geom2
	 *
	 * @return
	 */
	private boolean equalSRID(Geometry geom1, Geometry geom2) {
		return geom1.getSRID() == geom2.getSRID() ||
				(geom1.getSRID() < 1 && geom2.getSRID() < 1);
	}

	/**
	 * Test whether two geometries, not of type GeometryCollection are equal.
	 *
	 * @param geom1
	 * @param geom2
	 *
	 * @return
	 */
	protected boolean testSimpleGeometryEquality(Geometry geom1, Geometry geom2) {
		//return geom1.equals(geom2);
		return testTypeAndVertexEquality( geom1, geom2 );
	}

	protected boolean testTypeAndVertexEquality(Geometry geom1, Geometry geom2) {
		if ( !geom1.getGeometryType().equals( geom2.getGeometryType() ) ) {
			return false;
		}
		if ( geom1.getNumGeometries() != geom2.getNumGeometries() ) {
			return false;
		}
		if ( geom1.getNumPoints() != geom2.getNumPoints() ) {
			return false;
		}
		Coordinate[] coordinates1 = geom1.getCoordinates();
		Coordinate[] coordinates2 = geom2.getCoordinates();
		for ( int i = 0; i < coordinates1.length; i++ ) {
			Coordinate c1 = coordinates1[i];
			Coordinate c2 = coordinates2[i];
			if ( !testCoordinateEquality( c1, c2 ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean testCoordinateEquality(Coordinate c1, Coordinate c2) {
//		if ( c1 instanceof MCoordinate ) {
//			if ( !( c2 instanceof MCoordinate ) ) {
//				return false;
//			}
//			MCoordinate mc1 = (MCoordinate) c1;
//			MCoordinate mc2 = (MCoordinate) c2;
//			if ( !Double.isNaN( mc1.m ) && mc1.m != mc2.m ) {
//				return false;
//			}
//		}
		if ( !Double.isNaN( c1.z ) && c1.z != c2.z ) {
			return false;
		}
		return c1.x == c2.x && c1.y == c2.y;
	}
}
