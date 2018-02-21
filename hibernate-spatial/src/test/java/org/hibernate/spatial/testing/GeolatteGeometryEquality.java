package org.hibernate.spatial.testing;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryPointEquality;
import org.geolatte.geom.Position;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public class GeolatteGeometryEquality<P extends Position> implements GeometryEquality<Geometry<P>> {

	private final org.geolatte.geom.GeometryEquality delegate;

	public GeolatteGeometryEquality() {
		this( new GeometryPointEquality() );
	}

	public GeolatteGeometryEquality(org.geolatte.geom.GeometryEquality delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean test(Geometry<P> geom1, Geometry<P> geom2) {
		return delegate.equals( geom1, geom2 );
	}


}
