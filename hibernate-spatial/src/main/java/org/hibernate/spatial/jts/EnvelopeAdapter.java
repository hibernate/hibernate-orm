/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

/**
 * Converts an {@code Envelope} to a {@code Polygon}
 */
public class EnvelopeAdapter {

	private static volatile GeometryFactory geomFactory = new GeometryFactory();

	private EnvelopeAdapter() {
	}

	/**
	 * Converts the specified {@code Envelope} to a {@code Polygon} having the specified srid.
	 *
	 * @param env The envelope to convert
	 * @param srid The srid for the polygon
	 *
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
