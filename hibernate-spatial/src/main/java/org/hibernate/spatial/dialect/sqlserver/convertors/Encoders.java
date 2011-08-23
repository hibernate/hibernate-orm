/*
 * $Id: Encoders.java 201 2010-04-05 13:49:25Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2009 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial.dialect.sqlserver.convertors;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * Serializes a JTS <code>Geometry</code> to a byte-array.
 *
 * @author Karel Maesen, Geovise BVBA.
 */
public class Encoders {

	final private static List<Encoder<? extends Geometry>> ENCODERS = new ArrayList<Encoder<? extends Geometry>>();


	static {
		//Encoders
		ENCODERS.add( new PointEncoder() );
		ENCODERS.add( new LineStringEncoder() );
		ENCODERS.add( new PolygonEncoder() );
		ENCODERS.add( new MultiPointEncoder() );
		ENCODERS.add( new GeometryCollectionEncoder<MultiLineString>( OpenGisType.MULTILINESTRING ) );
		ENCODERS.add( new GeometryCollectionEncoder<MultiPolygon>( OpenGisType.MULTIPOLYGON ) );
		ENCODERS.add( new GeometryCollectionEncoder<GeometryCollection>( OpenGisType.GEOMETRYCOLLECTION ) );

	}

	public static Encoder<? extends Geometry> encoderFor(Geometry geom) {
		for ( Encoder<? extends Geometry> encoder : ENCODERS ) {
			if ( encoder.accepts( geom ) ) {
				return encoder;
			}
		}
		throw new IllegalArgumentException( "No encoder for type " + geom.getGeometryType() );
	}

	public static <T extends Geometry> byte[] encode(T geom) {
		Encoder<T> encoder = (Encoder<T>) encoderFor( geom );
		SqlServerGeometry sqlServerGeometry = encoder.encode( geom );
		return SqlServerGeometry.serialize( sqlServerGeometry );
	}

}
