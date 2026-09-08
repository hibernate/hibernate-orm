/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.LocalDateTime;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Common marker interface for mapping {@linkplain java.time Java Time} objects
 * directly through the JDBC driver.
 *
 * @author Steve Ebersole
 */
public interface JavaTimeJdbcType extends JdbcType {
	/**
	 * Resolve the SQL type code used for a value contained in a native JDBC
	 * container or rendered by an aggregate SQL expression.
	 */
	static int getPhysicalJdbcTypeCode(JdbcType jdbcType) {
		return jdbcType instanceof JavaTimeJdbcType
				? jdbcType.getDdlTypeCode()
				: jdbcType.getDefaultSqlTypeCode();
	}

	/**
	 * Resolve the Java representation expected by a native JDBC container for
	 * the physical SQL type of this direct Java Time descriptor.
	 */
	static Class<?> getPhysicalJavaTypeClass(JavaTimeJdbcType jdbcType, WrapperOptions options) {
		final var typeConfiguration = options.getTypeConfiguration();
		final var physicalJdbcType = typeConfiguration.getJdbcTypeRegistry()
				.getDescriptor( jdbcType.getDdlTypeCode() );
		final var preferredJavaTypeClass = physicalJdbcType.getPreferredJavaTypeClass( options );
		return preferredJavaTypeClass != null
				? preferredJavaTypeClass
				: physicalJdbcType.getRecommendedJavaType( null, null, typeConfiguration ).getJavaTypeClass();
	}

	/**
	 * Convert a direct Java Time JDBC value to the representation expected by a
	 * native JDBC container.
	 */
	static <T> Object toPhysicalJdbcValue(
			JavaTimeJdbcType jdbcType,
			Object value,
			JavaType<T> javaType,
			WrapperOptions options) {
		final Class<?> physicalJavaTypeClass = getPhysicalJavaTypeClass( jdbcType, options );
		if ( physicalJavaTypeClass.isInstance( value ) ) {
			return value;
		}
		final T relationalValue = javaType.isInstance( value )
				? javaType.cast( value )
				: javaType.wrap( value, options );
		return javaType.unwrap(
				relationalValue,
				physicalJavaTypeClass,
				options
		);
	}

	/**
	 * Decode a Java Time value written into an aggregate. Database-side aggregate
	 * operations might render a local timestamp using SQL's space separator even
	 * though Hibernate encodes it using ISO's {@code T} separator.
	 */
	static Object fromEncodedString(JavaType<?> javaType, CharSequence string, int start, int end) {
		if ( javaType.getJavaTypeClass() == LocalDateTime.class
				&& end - start > 10
				&& string.charAt( start + 10 ) == ' ' ) {
			final StringBuilder normalized = new StringBuilder( end - start );
			normalized.append( string, start, start + 10 );
			normalized.append( 'T' );
			normalized.append( string, start + 11, end );
			return javaType.fromEncodedString( normalized );
		}
		return javaType.fromEncodedString( string, start, end );
	}
}
