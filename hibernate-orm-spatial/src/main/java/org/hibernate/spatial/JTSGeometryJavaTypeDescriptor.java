/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.geolatte.geom.jts.JTS;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

/**
 * Descriptor for JTS {@code Geometry}s.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class JTSGeometryJavaTypeDescriptor extends AbstractTypeDescriptor<Geometry> {

	/**
	 * An instance of this descriptor
	 */
	public static final JavaTypeDescriptor<Geometry> INSTANCE = new JTSGeometryJavaTypeDescriptor();

	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public JTSGeometryJavaTypeDescriptor() {
		super( Geometry.class );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
	}

	@Override
	public String toString(Geometry value) {
		return value.toText();
	}

	@Override
	public Geometry fromString(String string) {
		final WKTReader reader = new WKTReader();
		try {
			return reader.read( string );
		}
		catch ( ParseException e ) {
			throw new RuntimeException( String.format( "Can't parse string %s as WKT", string ) );
		}
	}

	@Override
	public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Geometry.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( org.geolatte.geom.Geometry.class.isAssignableFrom( type ) ) {
			return (X) JTS.from( value );
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
		if ( org.geolatte.geom.Geometry.class.isInstance( value ) ) {
			return JTS.to( (org.geolatte.geom.Geometry) value );
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
