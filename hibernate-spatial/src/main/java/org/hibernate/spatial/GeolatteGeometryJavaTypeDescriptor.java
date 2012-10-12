package org.hibernate.spatial;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/12/12
 */
public class GeolatteGeometryJavaTypeDescriptor extends AbstractTypeDescriptor<Geometry> {


	public static final GeolatteGeometryJavaTypeDescriptor INSTANCE = new GeolatteGeometryJavaTypeDescriptor( Geometry.class );

	public GeolatteGeometryJavaTypeDescriptor(Class<Geometry> type) {
		super( type );
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
