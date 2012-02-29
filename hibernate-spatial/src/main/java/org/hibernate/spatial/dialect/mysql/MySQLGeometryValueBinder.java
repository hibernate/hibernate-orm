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

package org.hibernate.spatial.dialect.mysql;

import java.sql.Connection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.WKBWriter;

import org.hibernate.spatial.dialect.AbstractGeometryValueBinder;
import org.hibernate.spatial.jts.JTS;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public class MySQLGeometryValueBinder<X> extends AbstractGeometryValueBinder {

	private static final int SRIDLEN = 4;

	public MySQLGeometryValueBinder(JavaTypeDescriptor<X> javaDescriptor) {
		super( javaDescriptor, MySQLGeometryTypeDescriptor.INSTANCE );
	}

	@Override
	protected Object toNative(Geometry jtsGeom, Connection connection) {
		if ( jtsGeom.isEmpty() ) {
			return null;
		}
		jtsGeom = forceGeometryCollection( jtsGeom );
		int srid = jtsGeom.getSRID();

		WKBWriter writer = new WKBWriter(
				2,
				ByteOrderValues.LITTLE_ENDIAN
		);
		byte[] wkb = writer.write( jtsGeom );

		byte[] byteArr = new byte[wkb.length + SRIDLEN];
		byteArr[3] = (byte) ( ( srid >> 24 ) & 0xFF );
		byteArr[2] = (byte) ( ( srid >> 16 ) & 0xFF );
		byteArr[1] = (byte) ( ( srid >> 8 ) & 0xFF );
		byteArr[0] = (byte) ( srid & 0xFF );
		System.arraycopy( wkb, 0, byteArr, SRIDLEN, wkb.length );
		return byteArr;
	}

	private Geometry forceGeometryCollection(Geometry jtsGeom) {
		if ( jtsGeom.isEmpty() ) {
			return createEmptyGeometryCollection( jtsGeom );
		}
		if ( jtsGeom instanceof GeometryCollection ) {
			GeometryCollection gc = (GeometryCollection) jtsGeom;
			Geometry[] components = new Geometry[gc.getNumGeometries()];
			for ( int i = 0; i < gc.getNumGeometries(); i++ ) {
				Geometry component = gc.getGeometryN( i );
				if ( component.isEmpty() ) {
					components[i] = jtsGeom.getFactory().createGeometryCollection( null );
				}
				else {
					components[i] = component;
				}
			}
			Geometry geometryCollection = jtsGeom.getFactory().createGeometryCollection( components );
			geometryCollection.setSRID( jtsGeom.getSRID() );
			return geometryCollection;
		}
		return jtsGeom;
	}

	private Geometry createEmptyGeometryCollection(Geometry jtsGeom) {
		GeometryFactory factory = jtsGeom.getFactory();
		if ( factory == null ) {
			factory = JTS.getDefaultGeometryFactory();
		}
		Geometry empty = factory.createGeometryCollection( null );
		empty.setSRID( jtsGeom.getSRID() );
		return empty;
	}
}
