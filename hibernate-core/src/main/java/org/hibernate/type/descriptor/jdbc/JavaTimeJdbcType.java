/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.LocalDateTime;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Common marker interface for mapping {@linkplain java.time Java Time} objects
 * directly through the JDBC driver.
 *
 * @author Steve Ebersole
 */
public interface JavaTimeJdbcType extends JdbcType {
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
