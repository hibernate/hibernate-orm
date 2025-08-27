/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * Descriptor for JDBC value within an operation.
 *
 * @implSpec Used while {@linkplain org.hibernate.engine.jdbc.mutation.JdbcValueBindings binding}
 * values to JDBC Statements
 *
 * @author Steve Ebersole
 */
public interface JdbcValueDescriptor {
	/**
	 * The name of the column this parameter "maps to"
	 */
	String getColumnName();

	/**
	 * How the parameter is used in the query
	 */
	ParameterUsage getUsage();

	/**
	 * The position within the operation, starting at 1 per JDBC
	 */
	int getJdbcPosition();

	/**
	 * The JDBC mapping (type, etc.) for the parameter
	 */
	JdbcMapping getJdbcMapping();

	default boolean matches(String columnName, ParameterUsage usage) {
		return getColumnName().equals( columnName )
				&& getUsage() == usage;
	}
}
