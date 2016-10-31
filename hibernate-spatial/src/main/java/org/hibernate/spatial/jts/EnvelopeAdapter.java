/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.jts;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Converts an {@code Envelope} to a {@code Polygon}
 */
public class EnvelopeAdapter {

	private static GeometryFactory geomFactory = new GeometryFactory();

	private EnvelopeAdapter() {
	}

	/**
	 * Converts the specified {@code Envelope} to a {@code Polygon} having the specified srid.
	 * @param env The envelope to convert
	 * @param srid The srid for the polygon
	 * @return The Polygon
	 */
	public static Polygon toPolygon(Envelope env, int srid) {
		final Coordinate[] coords = new Coordinate[5];

		coords[0] = new Coordinate( env.getMinX(), env.getMinY() );
		coords[1] = new Coordinate( env.getMinX(), env.getMaxY() );
		coords[2] = new Coordinate( env.getMaxX(), env.getMaxY() );
		coords[3] = new Coordinate( env.getMaxX(), env.getMinY() );
		coords[4] = new Coordinate( env.getMinX(), env.getMinY() );
		final LinearRing shell = geomFactory.createLinearRing( coords );

		final Polygon pg = geomFactory.createPolygon( shell, null );
		pg.setSRID( srid );
		return pg;
	}

	public static void setGeometryFactory(GeometryFactory gf) {
		geomFactory = gf;
	}

}
