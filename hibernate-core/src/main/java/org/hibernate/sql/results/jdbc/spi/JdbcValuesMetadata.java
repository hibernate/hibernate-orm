/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to information about the underlying JDBC values
 * such as type, position, column name, etc
 */
public interface JdbcValuesMetadata {
	/**
	 * Number of values in the underlying result
	 */
	int getColumnCount();

	/**
	 * Position of a particular result value by name
	 */
	int resolveColumnPosition(String columnName);

	/**
	 * Name of a particular result value by position
	 */
	String resolveColumnName(int position);

	/**
	 * Determine the mapping to use for a particular position in the result
	 */
	default <J> BasicType<J> resolveType(
			int position,
			JavaType<J> explicitJavaType,
			SessionFactoryImplementor sessionFactory) {
		return resolveType( position, explicitJavaType, sessionFactory.getTypeConfiguration() );
	}

	/**
	 * Determine the mapping to use for a particular position in the result
	 */
	<J> BasicType<J> resolveType(
			int position,
			JavaType<J> explicitJavaType,
			TypeConfiguration typeConfiguration);
}
