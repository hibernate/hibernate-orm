/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.OffsetTime;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain OffsetTime} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class OffsetTimeJdbcType extends AbstractJavaTimeJdbcType<OffsetTime> {
	public static OffsetTimeJdbcType INSTANCE = new OffsetTimeJdbcType();

	public OffsetTimeJdbcType() {
		super( OffsetTime.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.OFFSET_TIME;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.TIME_WITH_TIMEZONE;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}
}
