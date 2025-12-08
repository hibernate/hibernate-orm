/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;
import java.time.OffsetTime;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for {@link Types#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE} handling.
 */
public class TimeAsTimestampWithTimeZoneJdbcType extends TimestampWithTimeZoneJdbcType {
	public static final TimeAsTimestampWithTimeZoneJdbcType INSTANCE = new TimeAsTimestampWithTimeZoneJdbcType();

	public TimeAsTimestampWithTimeZoneJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return Types.TIME_WITH_TIMEZONE;
	}

	@Override
	public String getFriendlyName() {
		return "TIME_AS_TIMESTAMP_WITH_TIMEZONE";
	}

	@Override
	public String toString() {
		return "TimeAsTimestampWithTimeZoneDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( OffsetTime.class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}

}
