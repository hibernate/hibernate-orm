/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.spi.TypeConfiguration;

/// Parameterized JDBC descriptor constructor supplied by the external fixture.
///
/// @author Steve Ebersole
public final class ExampleJdbcTypeConstructor implements JdbcTypeConstructor {
	public static final int TYPE_CODE = 60_101;
	public static final ExampleJdbcTypeConstructor INSTANCE = new ExampleJdbcTypeConstructor();

	private ExampleJdbcTypeConstructor() {
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		return new ArrayJdbcType( elementType );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return TYPE_CODE;
	}
}
