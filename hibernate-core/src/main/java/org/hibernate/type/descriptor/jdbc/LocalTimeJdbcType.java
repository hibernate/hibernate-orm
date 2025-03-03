/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.LocalTime;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain LocalTime} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class LocalTimeJdbcType extends AbstractJavaTimeJdbcType<LocalTime> {
	public static LocalTimeJdbcType INSTANCE = new LocalTimeJdbcType();

	public LocalTimeJdbcType() {
		super( LocalTime.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.LOCAL_TIME;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.TIME;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}
}
