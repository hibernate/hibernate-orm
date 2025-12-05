/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

/**
 * Descriptor for {@link Types#LONGVARCHAR LONGVARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class LongVarcharJdbcType extends VarcharJdbcType {
	public static final LongVarcharJdbcType INSTANCE = new LongVarcharJdbcType();

	private final int defaultSqlTypeCode;

	public LongVarcharJdbcType() {
		this( Types.LONGVARCHAR );
	}

	public LongVarcharJdbcType(int defaultSqlTypeCode) {
		this.defaultSqlTypeCode = defaultSqlTypeCode;
	}

	@Override
	public String toString() {
		return "LongVarcharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.LONGVARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return defaultSqlTypeCode;
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
