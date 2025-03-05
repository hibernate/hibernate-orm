/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.time.Instant;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.AbstractJavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for handling {@linkplain Instant} directly through the JDBC driver
 *
 * @author Steve Ebersole
 */
public class InstantJdbcType extends AbstractJavaTimeJdbcType<Instant> {
	public static final InstantJdbcType INSTANCE = new InstantJdbcType();

	public InstantJdbcType() {
		super( Instant.class );
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.INSTANT;
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.TIMESTAMP;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}
}
