/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryCollection;
import org.geolatte.geom.LineString;
import org.geolatte.geom.MultiLineString;
import org.geolatte.geom.MultiPoint;
import org.geolatte.geom.MultiPolygon;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;

/**
 * Descriptor for geolatte-geom {@code Geometry}s.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 10/12/12
 */
public class GeolatteGeometryJavaTypeDescriptor extends AbstractJavaTypeDescriptor<Geometry> {

	final private Wkt.Dialect wktDialect;
	/**
	 * an instance of this descriptor
	 */
	public static final GeolatteGeometryJavaTypeDescriptor GEOMETRY_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			Geometry.class );
	public static final GeolatteGeometryJavaTypeDescriptor POINT_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			Point.class );
	public static final GeolatteGeometryJavaTypeDescriptor LINESTRING_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			LineString.class );
	public static final GeolatteGeometryJavaTypeDescriptor POLYGON_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			Polygon.class );
	public static final GeolatteGeometryJavaTypeDescriptor GEOMETRYCOLL_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			GeometryCollection.class );
	public static final GeolatteGeometryJavaTypeDescriptor MULTIPOINT_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			MultiPoint.class );
	public static final GeolatteGeometryJavaTypeDescriptor MULTILINESTRING_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			MultiLineString.class );
	public static final GeolatteGeometryJavaTypeDescriptor MULTIPOLYGON_INSTANCE = new GeolatteGeometryJavaTypeDescriptor(
			MultiPolygon.class );


	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public GeolatteGeometryJavaTypeDescriptor(Class<? extends Geometry> type) {
		this( type, Wkt.Dialect.SFA_1_1_0 );
	}

	public GeolatteGeometryJavaTypeDescriptor(Class<? extends Geometry> type, Wkt.Dialect wktDialect) {
		super( type );
		this.wktDialect = Wkt.Dialect.SFA_1_1_0;
	}

	@Override
	public String toString(Geometry value) {
		return Wkt.toWkt( value, wktDialect );
	}


	@Override
	public Geometry fromString(CharSequence string) {
		return Wkt.fromWkt( string.toString(), wktDialect );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators indicators) {
		return indicators.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( SqlTypes.GEOMETRY );
	}

	@Override
	public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( Geometry.class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( org.locationtech.jts.geom.Geometry.class.isAssignableFrom( type ) ) {
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
		if ( CharSequence.class.isInstance( value ) ) {
			return fromString( (CharSequence) value );
		}

		if ( org.locationtech.jts.geom.Geometry.class.isInstance( value ) ) {
			return JTS.from( (org.locationtech.jts.geom.Geometry) value );
		}

		throw unknownWrap( value.getClass() );

	}
}
