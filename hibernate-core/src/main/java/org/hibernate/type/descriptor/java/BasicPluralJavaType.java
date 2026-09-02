/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Describes a basic plural Java value mapped to one column, such as an SQL
/// array or a JSON/XML collection. The contract exposes the element descriptor
/// and resolves a container [BasicType] from the mapped element type.
///
/// @param <T> the element value type
/// @see org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType
@Incubating
@SPI({ USE, IMPLEMENT })
public interface BasicPluralJavaType<T> extends Serializable {
	/**
	 * Get the Java type descriptor for the element type
	 */
	JavaType<T> getElementJavaType();
	/**
	 * Creates a container type for the given element type
	 */
	BasicType<?> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<T> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators);

}
