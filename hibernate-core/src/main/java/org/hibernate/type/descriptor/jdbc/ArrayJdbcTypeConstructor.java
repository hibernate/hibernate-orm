/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;

/**
 * Factory for {@link ArrayJdbcType}.
 *
 * @author Gavin King
 */
public class ArrayJdbcTypeConstructor implements JdbcTypeConstructor {
	public static final ArrayJdbcTypeConstructor INSTANCE = new ArrayJdbcTypeConstructor();

	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation) {
		return resolveType( typeConfiguration, dialect, elementType.getJdbcType(), columnTypeInformation );
	}

	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		return new ArrayJdbcType( elementType );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return Types.ARRAY;
	}
}
