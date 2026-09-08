/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;

/// Standard immutable direct Java Time JDBC support profile.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class StandardDirectJavaTimeJdbcSupport implements DirectJavaTimeJdbcSupport {
	private static final Set<Class<?>> RECOGNIZED_TYPES = Set.of(
			LocalDate.class,
			LocalTime.class,
			LocalDateTime.class,
			OffsetTime.class,
			OffsetDateTime.class,
			ZonedDateTime.class,
			Instant.class
	);

	private static final DirectJavaTimeJdbcSupport NONE = new StandardDirectJavaTimeJdbcSupport( Set.of() );
	private static final DirectJavaTimeJdbcSupport JDBC_42 = new StandardDirectJavaTimeJdbcSupport( Set.of(
			LocalDate.class,
			LocalTime.class,
			LocalDateTime.class,
			OffsetTime.class,
			OffsetDateTime.class
	) );
	private static final DirectJavaTimeJdbcSupport ALL = new StandardDirectJavaTimeJdbcSupport( RECOGNIZED_TYPES );

	private final Set<Class<?>> supportedTypes;

	private StandardDirectJavaTimeJdbcSupport(Set<Class<?>> supportedTypes) {
		this.supportedTypes = supportedTypes;
	}

	public static DirectJavaTimeJdbcSupport none() {
		return NONE;
	}

	public static DirectJavaTimeJdbcSupport jdbc42() {
		return JDBC_42;
	}

	public static DirectJavaTimeJdbcSupport all() {
		return ALL;
	}

	public static DirectJavaTimeJdbcSupport of(Class<?>... supportedJavaTimeTypes) {
		final Set<Class<?>> supportedTypes = new HashSet<>( Arrays.asList( supportedJavaTimeTypes ) );
		if ( !RECOGNIZED_TYPES.containsAll( supportedTypes ) ) {
			supportedTypes.removeAll( RECOGNIZED_TYPES );
			throw new IllegalArgumentException( "Unrecognized Java Time classes: " + supportedTypes );
		}
		if ( !supportedTypes.contains( OffsetTime.class ) || !supportedTypes.contains( OffsetDateTime.class ) ) {
			supportedTypes.remove( OffsetTime.class );
			supportedTypes.remove( OffsetDateTime.class );
		}
		return supportedTypes.isEmpty()
				? NONE
				: new StandardDirectJavaTimeJdbcSupport( Set.copyOf( supportedTypes ) );
	}

	@Override
	public boolean supports(Class<?> javaTimeType) {
		return supportedTypes.contains( javaTimeType );
	}
}
