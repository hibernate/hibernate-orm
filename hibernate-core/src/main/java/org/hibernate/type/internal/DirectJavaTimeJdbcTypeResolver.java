/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.function.Predicate;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/// Resolves the direct JDBC type code for a mapped Java Time class.
///
/// @since 8.0
///
/// @author Steve Ebersole
@Internal
public final class DirectJavaTimeJdbcTypeResolver {
	private DirectJavaTimeJdbcTypeResolver() {
	}

	/// Resolve the direct JDBC type code, or `null` when direct access is disabled.
	/// A `ZonedDateTime` may be represented at the JDBC boundary as an
	/// `OffsetDateTime` when direct zoned access is unavailable.
	public static @Nullable Integer resolve(
			Class<?> mappedJavaTimeType,
			Predicate<Class<?>> directAccessEnabled) {
		if ( mappedJavaTimeType == LocalDate.class ) {
			return enabledCode( LocalDate.class, SqlTypes.LOCAL_DATE, directAccessEnabled );
		}
		if ( mappedJavaTimeType == LocalTime.class ) {
			return enabledCode( LocalTime.class, SqlTypes.LOCAL_TIME, directAccessEnabled );
		}
		if ( mappedJavaTimeType == LocalDateTime.class ) {
			return enabledCode( LocalDateTime.class, SqlTypes.LOCAL_DATE_TIME, directAccessEnabled );
		}
		if ( mappedJavaTimeType == OffsetTime.class ) {
			return enabledCode( OffsetTime.class, SqlTypes.OFFSET_TIME, directAccessEnabled );
		}
		if ( mappedJavaTimeType == OffsetDateTime.class ) {
			return enabledCode( OffsetDateTime.class, SqlTypes.OFFSET_DATE_TIME, directAccessEnabled );
		}
		if ( mappedJavaTimeType == ZonedDateTime.class ) {
			if ( directAccessEnabled.test( ZonedDateTime.class ) ) {
				return SqlTypes.ZONED_DATE_TIME;
			}
			return enabledCode( OffsetDateTime.class, SqlTypes.OFFSET_DATE_TIME, directAccessEnabled );
		}
		if ( mappedJavaTimeType == Instant.class ) {
			return enabledCode( Instant.class, SqlTypes.INSTANT, directAccessEnabled );
		}
		return null;
	}

	/// Resolve the direct JDBC type code using the supplied indicators, logging
	/// the Dialect-driven fallback when direct access was enabled by configuration.
	@SuppressWarnings("deprecation")
	public static @Nullable Integer resolve(
			Class<?> mappedJavaTimeType,
			JdbcTypeIndicators indicators) {
		final Integer directJdbcTypeCode = resolve(
				mappedJavaTimeType,
				indicators::isDirectJavaTimeJdbcAccessEnabled
		);
		if ( indicators.isPreferJavaTimeJdbcTypesEnabled() ) {
			if ( directJdbcTypeCode == null ) {
				indicators.getTypeConfiguration().logDirectJavaTimeJdbcFallback( mappedJavaTimeType, null );
			}
			else if ( mappedJavaTimeType == ZonedDateTime.class
					&& directJdbcTypeCode == SqlTypes.OFFSET_DATE_TIME ) {
				indicators.getTypeConfiguration().logDirectJavaTimeJdbcFallback(
						ZonedDateTime.class,
						OffsetDateTime.class
				);
			}
		}
		return directJdbcTypeCode;
	}

	private static @Nullable Integer enabledCode(
			Class<?> jdbcJavaTimeType,
			int jdbcTypeCode,
			Predicate<Class<?>> directAccessEnabled) {
		return directAccessEnabled.test( jdbcJavaTimeType ) ? jdbcTypeCode : null;
	}
}
