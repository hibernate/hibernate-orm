package org.hibernate.spatial.testing;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public interface GeometryEquality<G> {
	boolean test(G geom1, G geom2);
}
