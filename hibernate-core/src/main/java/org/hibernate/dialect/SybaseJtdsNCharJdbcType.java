/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class SybaseJtdsNCharJdbcType extends SybaseJtdsNVarcharJdbcType {

	public static final SybaseJtdsNCharJdbcType JTDS_INSTANCE = new SybaseJtdsNCharJdbcType();

	public SybaseJtdsNCharJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NCHAR;
	}

	@Override
	public String toString() {
		return "SybaseJtdsNCharJdbcType";
	}

	@Override
	public JdbcType resolveIndicatedType(
			JdbcTypeIndicators indicators,
			JavaType<?> domainJtd) {
		assert domainJtd != null;

		final var typeConfiguration = indicators.getTypeConfiguration();
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
