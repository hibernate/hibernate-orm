/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import java.util.Locale;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

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
public class JTSGeometryJavaTypeDescriptor extends AbstractJavaTypeDescriptor<Geometry> {

	/**
	 * An instance of this descriptor
	 */
	public static final JTSGeometryJavaTypeDescriptor GEOMETRY_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			Geometry.class );
	public static final JTSGeometryJavaTypeDescriptor POINT_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			Point.class );
	public static final JTSGeometryJavaTypeDescriptor LINESTRING_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			LineString.class );
	public static final JTSGeometryJavaTypeDescriptor POLYGON_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			Polygon.class );
	public static final JTSGeometryJavaTypeDescriptor GEOMETRYCOLL_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			GeometryCollection.class );
	public static final JTSGeometryJavaTypeDescriptor MULTIPOINT_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			MultiPoint.class );
	public static final JTSGeometryJavaTypeDescriptor MULTILINESTRING_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			MultiLineString.class );
	public static final JTSGeometryJavaTypeDescriptor MULTIPOLYGON_INSTANCE = new JTSGeometryJavaTypeDescriptor(
			MultiPolygon.class );

	/**
	 * Initialize a type descriptor for the geolatte-geom {@code Geometry} type.
	 */
	public JTSGeometryJavaTypeDescriptor(Class<? extends Geometry> type) {
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
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators indicators) {
		return indicators.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( SqlTypes.GEOMETRY );
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
