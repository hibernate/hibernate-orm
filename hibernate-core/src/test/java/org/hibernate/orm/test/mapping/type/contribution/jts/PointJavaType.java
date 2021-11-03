/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

import org.locationtech.jts.geom.Point;

/**
 * @author Steve Ebersole
 */
public class PointJavaType implements BasicJavaType<Point> {
	/**
	 * Singleton access
	 */
	public static final PointJavaType INSTANCE = new PointJavaType();

	@Override
	public Class<Point> getJavaTypeClass() {
		return Point.class;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return context
				.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry()
				.getDescriptor( PointJdbcType.POINT_TYPE_CODE );
	}

	@Override
	public Point fromString(CharSequence string) {
		throw unsupported( String.class );
	}

	private static UnsupportedOperationException unsupported(Class<?> type) {
		return new UnsupportedOperationException( "At the moment, Point cannot be converted to/from other types : " + type.getName() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Point value, Class<X> type, WrapperOptions options) {
		if ( value == null || type.isInstance( value ) ) {
			return (X) value;
		}
		throw unsupported( type );
	}

	@Override
	public <X> Point wrap(X value, WrapperOptions options) {
		if ( value == null || value instanceof Point ) {
			return (Point) value;
		}
		throw unsupported( value.getClass() );
	}
}
