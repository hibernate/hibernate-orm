/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for a basic plural Java type.
 * A basic plural type represents a type, that is mapped to a single column instead of multiple rows.
 * This is used for array or collection types, that are backed by e.g. SQL array or JSON/XML DDL types.
 * <p>
 * The interface can be implemented by a plural java type e.g. {@link org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType}
 * and provides access to the element java type, as well as a hook to resolve the {@link BasicType} based on the element {@link BasicType},
 * in order to gain enough information to implement storage and retrieval of the composite data type via JDBC.
 *
 * @see org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType
 */
@Incubating
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
