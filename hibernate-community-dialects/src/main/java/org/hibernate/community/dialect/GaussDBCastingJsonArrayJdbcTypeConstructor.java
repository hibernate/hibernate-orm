/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Factory for {@link GaussDBCastingJsonArrayJdbcType}.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLCastingJsonArrayJdbcTypeConstructor.
 */
public class GaussDBCastingJsonArrayJdbcTypeConstructor implements JdbcTypeConstructor {

	public static final GaussDBCastingJsonArrayJdbcTypeConstructor JSONB_INSTANCE = new GaussDBCastingJsonArrayJdbcTypeConstructor( true );
	public static final GaussDBCastingJsonArrayJdbcTypeConstructor JSON_INSTANCE = new GaussDBCastingJsonArrayJdbcTypeConstructor( false );

	private final boolean jsonb;

	public GaussDBCastingJsonArrayJdbcTypeConstructor(boolean jsonb) {
		this.jsonb = jsonb;
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation) {
		return resolveType( typeConfiguration, dialect, elementType.getJdbcType(), columnTypeInformation );
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		return new GaussDBCastingJsonArrayJdbcType( elementType, jsonb );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.JSON_ARRAY;
	}
}
