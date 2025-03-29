/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Factory for {@link SQLServerCastingXmlArrayJdbcType}.
 */
public class SQLServerCastingXmlArrayJdbcTypeConstructor implements JdbcTypeConstructor {

	public static final SQLServerCastingXmlArrayJdbcTypeConstructor INSTANCE = new SQLServerCastingXmlArrayJdbcTypeConstructor();

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
		return new SQLServerCastingXmlArrayJdbcType( elementType );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.XML_ARRAY;
	}
}
