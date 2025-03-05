/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#NCHAR NCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class NCharJdbcType extends NVarcharJdbcType {
	public static final NCharJdbcType INSTANCE = new NCharJdbcType();

	public NCharJdbcType() {
	}

	@Override
	public String toString() {
		return "NCharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NCHAR;
	}

	@Override
	public JdbcType resolveIndicatedType(
			JdbcTypeIndicators indicators,
			JavaType<?> domainJtd) {
		assert domainJtd != null;

		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();

		final int jdbcTypeCode;
		if ( indicators.isLob() ) {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCLOB : Types.CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized() ? Types.NCHAR : Types.CHAR;
		}

		return jdbcTypeRegistry.getDescriptor( indicators.resolveJdbcTypeCode( jdbcTypeCode ) );
	}
}
