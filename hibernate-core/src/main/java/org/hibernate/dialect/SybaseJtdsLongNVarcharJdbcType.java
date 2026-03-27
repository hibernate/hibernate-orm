/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class SybaseJtdsLongNVarcharJdbcType extends SybaseJtdsNVarcharJdbcType {

	public static final SybaseJtdsLongNVarcharJdbcType JTDS_INSTANCE = new SybaseJtdsLongNVarcharJdbcType();

	public SybaseJtdsLongNVarcharJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.LONGNVARCHAR;
	}

	@Override
	public String toString() {
		return "SybaseJtdsLongNVarcharJdbcType";
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
		else if ( shouldUseMaterializedLob( indicators ) ) {
			jdbcTypeCode = indicators.isNationalized() ? SqlTypes.MATERIALIZED_NCLOB : SqlTypes.MATERIALIZED_CLOB;
		}
		else {
			jdbcTypeCode = indicators.isNationalized() ? Types.LONGNVARCHAR : Types.LONGVARCHAR;
		}

		return jdbcTypeRegistry.getDescriptor( indicators.resolveJdbcTypeCode( jdbcTypeCode ) );
	}
}
