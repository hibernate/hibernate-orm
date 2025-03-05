/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Factory for any {@link JdbcType} which is parameterized by
 * a second {@code JdbcType}, the "element" type.
 * <p>
 * For example, {@link ArrayJdbcType} is parameterized by the
 * type of its elements.
 *
 * @author Gavin King
 */
public interface JdbcTypeConstructor {
	/**
	 * Called by {@link org.hibernate.type.descriptor.java.ArrayJavaType}
	 * and friends. Here we already know the type argument, which
	 * we're given as a {@link BasicType}.
	 */
	default JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<?> elementType,
			ColumnTypeInformation columnTypeInformation) {
		return resolveType( typeConfiguration, dialect, elementType.getJdbcType(), columnTypeInformation );
	}

	/**
	 * Called from {@link Dialect#resolveSqlTypeDescriptor} when
	 * inferring {@link JdbcType}s from a JDBC {@code ResultSet}
	 * or when reverse-engineering a schema. Here we do not have
	 * a known {@link BasicType}.
	 */
	JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation);

	int getDefaultSqlTypeCode();
}
