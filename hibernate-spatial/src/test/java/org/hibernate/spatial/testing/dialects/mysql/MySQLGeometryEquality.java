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
