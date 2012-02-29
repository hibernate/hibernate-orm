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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.jts.JTS;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;

/**
 * Decodes SQL Server Geometry objects to JTS <code>Geometry</code>s.
 *
 * @author Karel Maesen, Geovise BVBA.
 */
public class Decoders {

	final private static List<Decoder<? extends Geometry>> DECODERS = new ArrayList<Decoder<? extends Geometry>>();

	static {
		MGeometryFactory factory = JTS.getDefaultGeometryFactory();

		//Decoders
		DECODERS.add( new PointDecoder( factory ) );
		DECODERS.add( new LineStringDecoder( factory ) );
		DECODERS.add( new PolygonDecoder( factory ) );
		DECODERS.add( new MultiLineStringDecoder( factory ) );
		DECODERS.add( new MultiPolygonDecoder( factory ) );
		DECODERS.add( new MultiPointDecoder( factory ) );
		DECODERS.add( new GeometryCollectionDecoder( factory ) );
	}


	private static Decoder<? extends Geometry> decoderFor(SqlServerGeometry object) {
		for ( Decoder<? extends Geometry> decoder : DECODERS ) {
			if ( decoder.accepts( object ) ) {
				return decoder;
			}
		}
		throw new IllegalArgumentException( "No decoder for type " + object.openGisType() );
	}

	/**
	 * Decodes the SQL Server Geometry object to its JTS Geometry instance
	 *
	 * @param raw
	 *
	 * @return
	 */
	public static Geometry decode(byte[] raw) {
		SqlServerGeometry sqlServerGeom = SqlServerGeometry.deserialize( raw );
		Decoder decoder = decoderFor( sqlServerGeom );
		return decoder.decode( sqlServerGeom );
	}

	/**
	 * Returns the decoder capable of decoding an object of the specified OpenGisType
	 *
	 * @param type OpenGisType for which a decoder is returned
	 *
	 * @return
	 */
	public static Decoder<? extends Geometry> decoderFor(OpenGisType type) {
		for ( Decoder<? extends Geometry> decoder : DECODERS ) {
			if ( decoder.accepts( type ) ) {
				return decoder;
			}
		}
		throw new IllegalArgumentException( "No decoder for type " + type );
	}

}
