/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.ZonedDateTime;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain ZonedDateTime} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class ZonedDateTimeJdbcType extends AbstractJavaTimeJdbcType<ZonedDateTime> {
	public static ZonedDateTimeJdbcType INSTANCE = new ZonedDateTimeJdbcType();

	public ZonedDateTimeJdbcType() {
		super( ZonedDateTime.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.ZONED_DATE_TIME;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}
}
