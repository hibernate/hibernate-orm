/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import java.util.Locale;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import org.geolatte.geom.jts.JTS;
import org.geolatte.geom.jts.JTSUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Descriptor for JTS {@code Geometry}s.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 7/27/11
 */
public class JTSGeometryJavaType extends AbstractJavaType<Geometry> {

	/**
	 * An instance of this descriptor
	 */
	public static final JTSGeometryJavaType GEOMETRY_INSTANCE = new JTSGeometryJavaType(
			Geometry.class );
	public static final JTSGeometryJavaType POINT_INSTANCE = new JTSGeometryJavaType(
			Point.class );
	public static final JTSGeometryJavaType LINESTRING_INSTANCE = new JTSGeometryJavaType(
			LineString.class );
	public static final JTSGeometryJavaType POLYGON_INSTANCE = new JTSGeometryJavaType(
			Polygon.class );
	public static final JTSGeometryJavaType GEOMETRYCOLL_INSTANCE = new JTSGeometryJavaType(
			GeometryCollection.class );
	public static final JTSGeometryJavaType MULTIPOINT_INSTANCE = new JTSGeometryJavaType(
			MultiPoint.class );
	public static final JTSGeometryJavaType MULTILINESTRING_INSTANCE = new JTSGeometryJavaType(
			MultiLineString.class );
	public static final JTSGeometryJavaType MULTIPOLYGON_INSTANCE = new JTSGeometryJavaType(
			MultiPolygon.class );

	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public JTSGeometryJavaType(Class<? extends Geometry> type) {
		super( type );
	}

	@Override
	public String toString(Geometry value) {
		return value.toText();
	}

	@Override
	public Geometry fromString(CharSequence string) {
		final WKTReader reader = new WKTReader();
		try {
			return reader.read( string.toString() );
		}
		catch (ParseException e) {
			throw new RuntimeException( String.format( Locale.ENGLISH, "Can't parse string %s as WKT", string ) );
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.GEOMETRY );
	}

	@Override
	public boolean areEqual(Geometry one, Geometry another) {
		return JTSUtils.equalsExact3D( one, another );
	}

	@SuppressWarnings("unchecked")
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
		if ( value instanceof Geometry ) {
			return (Geometry) value;
		}
		if ( value instanceof org.geolatte.geom.Geometry ) {
			return JTS.to( (org.geolatte.geom.Geometry<?>) value );
		}
		if ( value instanceof CharSequence ) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
