/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.spatial.dialect.h2geodb;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.C2D;
import org.geolatte.geom.Envelope;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.PositionSequence;
import org.geolatte.geom.PositionSequenceBuilders;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.geolatte.geom.jts.JTS;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.spatial.HSMessageLogger;

/**
 * A utility class to serialize from/to GeoDB WKB's.
 * <p/>
 * <p>Note: this utility makes it unnecessary to have a dependency on GeoDB. As long as GeoDB is
 * not available in common maven repositories, such a dependency is to be avoided.</p>
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class GeoDbWkb {

	private static final HSMessageLogger LOGGER = Logger.getMessageLogger(
			HSMessageLogger.class,
			GeoDbWkb.class.getName()
	);

	private GeoDbWkb() {
	}

	/**
	 * Encode the specified {@code Geometry} into a WKB
	 *
	 * @param geometry The value to encode
	 *
	 * @return A byte-array representing the geometry in WKB.
	 */
	public static byte[] to(Geometry geometry) {
		final WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.POSTGIS_EWKB_1 );
		final ByteBuffer buffer = encoder.encode( geometry, ByteOrder.NDR );
		return (buffer == null ? null : buffer.toByteArray());
	}

	/**
	 * Decode the object into a {@code Geometry}
	 *
	 * @param object The object to decode
	 *
	 * @return The {@code Geometry}
	 */
	public static Geometry from(Object object) {
		if ( object == null ) {
			return null;
		}
		try {
			final WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.POSTGIS_EWKB_1 );
			if ( object instanceof Blob ) {
				return decoder.decode( toByteBuffer( (Blob) object ) );
			}
			else if ( object instanceof byte[] ) {
				return decoder.decode( ByteBuffer.from( (byte[]) object ) );
			}
			else if ( object instanceof com.vividsolutions.jts.geom.Envelope ) {
				return toPolygon( JTS.from( (com.vividsolutions.jts.geom.Envelope) object ) );
			}
			else {
				throw new IllegalArgumentException(
						"Can't convert database object of type "
								+ object.getClass().getCanonicalName()
				);
			}
		}
		catch (Exception e) {
			LOGGER.warn( "Could not convert database object to a Geometry." );
			throw new HibernateException( e );
		}

	}

	private static Geometry<C2D> toPolygon(Envelope env) {
		final PositionSequence<C2D> ps = PositionSequenceBuilders.fixedSized( 4, C2D.class )
				.add( env.lowerLeft().getCoordinate( 0 ), env.lowerLeft().getCoordinate( 1 ) )
				.add( env.lowerLeft().getCoordinate( 0 ), env.upperRight().getCoordinate( 1 ) )
				.add( env.upperRight().getCoordinate( 0 ), env.upperRight().getCoordinate( 1 ) )
				.add( env.lowerLeft().getCoordinate( 0 ), env.lowerLeft().getCoordinate( 1 ) )
				.toPositionSequence();
		return new Polygon<C2D>( ps, CoordinateReferenceSystems.PROJECTED_2D_METER );
	}

	private static ByteBuffer toByteBuffer(Blob blob) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] buf = new byte[1024];

		InputStream in = null;
		try {
			in = blob.getBinaryStream();
			int n = 0;
			while ( (n = in.read( buf )) >= 0 ) {
				baos.write( buf, 0, n );
			}
		}
		catch (Exception e) {
			LOGGER.warn( "Could not convert database BLOB object to binary stream.", e );
		}
		finally {
			try {
				if ( in != null ) {
					in.close();
				}
			}
			catch (IOException e) {
				LOGGER.warn( "Could not close binary stream." );
			}
		}
		return ByteBuffer.from( baos.toByteArray() );
	}


}

