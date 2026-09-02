/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Constructs a [JdbcType] parameterized by an element `JdbcType`. For
/// example, [ArrayJdbcType] is parameterized by the type of its elements.
///
/// Providers register one stable constructor under its default SQL type code.
/// Hibernate invokes it with mapping-specific element and column information;
/// an implementation must not retain mutable bootstrap context.
///
/// @see org.hibernate.boot.model.TypeContributions#contributeJdbcTypeConstructor(JdbcTypeConstructor)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addTypeConstructor(JdbcTypeConstructor)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addTypeConstructor(int, JdbcTypeConstructor)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addTypeConstructorIfAbsent(JdbcTypeConstructor)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addTypeConstructorIfAbsent(int, JdbcTypeConstructor)
///
/// @author Gavin King
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface JdbcTypeConstructor {
	/**
	 * Called by {@link org.hibernate.type.descriptor.java.ArrayJavaType}
	 * and friends. Here we already know the type argument, which
	 * we're given as a {@link BasicType}.
	 * @see JdbcType
	 */
	@SPI(SUPPLY)
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
	 * @see JdbcType
	 */
	@SPI(SUPPLY)
	JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation);

	int getDefaultSqlTypeCode();
}
