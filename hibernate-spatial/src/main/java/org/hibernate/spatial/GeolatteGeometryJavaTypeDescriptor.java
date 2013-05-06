/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for spatial (geographic) data.
 *
 * Copyright © 2007-2013 Geovise BVBA
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
 */

package org.hibernate.spatial;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * Descriptor for geolatte-geom {@code Geometry}s.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/12/12
 */
public class GeolatteGeometryJavaTypeDescriptor extends AbstractTypeDescriptor<Geometry> {

	/**
	 * an instance of this descriptor
	 */
	public static final GeolatteGeometryJavaTypeDescriptor INSTANCE = new GeolatteGeometryJavaTypeDescriptor();

	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public GeolatteGeometryJavaTypeDescriptor() {
		super( Geometry.class );
	}

	@Override
	public String toString(Geometry value) {
		return value.asText();
	}

	@Override
	public Geometry fromString(String string) {
		return Wkt.fromWkt( string );
	}

	@Override
	public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Geometry.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( com.vividsolutions.jts.geom.Geometry.class.isAssignableFrom( type ) ) {
			return (X) JTS.to( value );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Geometry wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Geometry.class.isInstance( value ) ) {
			return (Geometry) value;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}

		if ( com.vividsolutions.jts.geom.Geometry.class.isInstance( value ) ) {
			return JTS.from( (com.vividsolutions.jts.geom.Geometry) value );
		}

		throw unknownWrap( value.getClass() );

	}
}
