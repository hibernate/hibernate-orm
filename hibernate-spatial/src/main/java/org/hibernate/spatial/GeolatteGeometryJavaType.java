/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

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
public class GeolatteGeometryJavaType extends AbstractJavaType<Geometry> {

	final private Wkt.Dialect wktDialect;
	/**
	 * an instance of this descriptor
	 */
	public static final GeolatteGeometryJavaType GEOMETRY_INSTANCE = new GeolatteGeometryJavaType(
			Geometry.class );
	public static final GeolatteGeometryJavaType POINT_INSTANCE = new GeolatteGeometryJavaType(
			Point.class );
	public static final GeolatteGeometryJavaType LINESTRING_INSTANCE = new GeolatteGeometryJavaType(
			LineString.class );
	public static final GeolatteGeometryJavaType POLYGON_INSTANCE = new GeolatteGeometryJavaType(
			Polygon.class );
	public static final GeolatteGeometryJavaType GEOMETRYCOLL_INSTANCE = new GeolatteGeometryJavaType(
			GeometryCollection.class );
	public static final GeolatteGeometryJavaType MULTIPOINT_INSTANCE = new GeolatteGeometryJavaType(
			MultiPoint.class );
	public static final GeolatteGeometryJavaType MULTILINESTRING_INSTANCE = new GeolatteGeometryJavaType(
			MultiLineString.class );
	public static final GeolatteGeometryJavaType MULTIPOLYGON_INSTANCE = new GeolatteGeometryJavaType(
			MultiPolygon.class );


	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public GeolatteGeometryJavaType(Class<? extends Geometry> type) {
		this( type, Wkt.Dialect.SFA_1_1_0 );
	}

	public GeolatteGeometryJavaType(Class<? extends Geometry> type, Wkt.Dialect wktDialect) {
		super( type );
		this.wktDialect = wktDialect;
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
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.GEOMETRY );
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
