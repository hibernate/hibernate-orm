/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

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
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
	}

	@Override
	public String toString(Geometry value) {
		return value.toString();
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
