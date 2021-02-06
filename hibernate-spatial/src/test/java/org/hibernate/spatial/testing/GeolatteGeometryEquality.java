/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryPositionEquality;
import org.geolatte.geom.Position;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public class GeolatteGeometryEquality<P extends Position> implements GeometryEquality<Geometry<P>> {

	private final org.geolatte.geom.GeometryEquality delegate;

	public GeolatteGeometryEquality() {
		this( new GeometryPositionEquality() );
	}

	public GeolatteGeometryEquality(org.geolatte.geom.GeometryEquality delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean test(Geometry<P> geom1, Geometry<P> geom2) {
		return delegate.equals( geom1, geom2 );
	}


}
