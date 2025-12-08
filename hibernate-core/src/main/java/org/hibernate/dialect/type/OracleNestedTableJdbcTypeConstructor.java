/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Factory for {@link OracleNestedTableJdbcType}.
 *
 * @see OracleJdbcHelper#getArrayJdbcTypeConstructor
 *
 * @author Gavin King
 */
public class OracleNestedTableJdbcTypeConstructor implements JdbcTypeConstructor {
	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation) {
		String typeName = columnTypeInformation == null ? null : columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			typeName = OracleArrayJdbcType.getTypeName( elementType, dialect );
		}
		return new OracleNestedTableJdbcType( elementType.getJdbcType(), typeName );
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		// a bit wrong, since columnTypeInformation.getTypeName() is typically null!
		String typeName = columnTypeInformation == null ? null : columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			Integer precision = null;
			Integer scale = null;
			if ( columnTypeInformation != null ) {
				precision = columnTypeInformation.getColumnSize();
				scale = columnTypeInformation.getDecimalDigits();
			}
			typeName = OracleArrayJdbcType.getTypeName( elementType.getRecommendedJavaType(
					precision,
					scale,
					typeConfiguration
			), elementType, dialect );
		}
		return new OracleNestedTableJdbcType( elementType, typeName );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TABLE;
	}
}
